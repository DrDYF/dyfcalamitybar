package com.dyf.calamitybar;

import com.dyf.calamitybar.network.ModNetworking;
import com.dyf.calamitybar.network.ModNetworking.RageEventPayload;
import com.dyf.calamitybar.network.ModNetworking.RageSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Server-authoritative logic for the rage meter. Runs entirely on the server
 * thread, so the {@link #STATES} map needs no extra synchronization.
 */
public final class RageManager {
    private RageManager() {
    }

    private static final Map<UUID, RageState> STATES = new HashMap<>();

    public static void onServerTick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RageState state = STATES.computeIfAbsent(player.getUUID(), id -> new RageState());
            tickPlayer(player, state, now);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        STATES.put(player.getUUID(), new RageState());
        ModNetworking.sendToPlayer(player, new RageSyncPayload(0.0f));
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    /** Called from the damage events whenever a player deals damage to a mob. */
    public static void onPlayerDealtDamage(Player player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        RageState state = STATES.computeIfAbsent(player.getUUID(), id -> new RageState());
        state.lastCombatTick = server.getTickCount();
    }

    /** Called from the C2S activation packet. */
    public static void activateRage(ServerPlayer player) {
        RageState state = STATES.computeIfAbsent(player.getUUID(), id -> new RageState());
        if (state.rage < RageConfig.MAX_RAGE - 0.5f) {
            return;
        }

        player.addEffect(new MobEffectInstance(
            ModMobEffects.RAGE_MODE.get(),
            RageConfig.rageModeDurationTicks(),
            0,
            false,
            true,
            true
        ));

        state.rage = 0.0f;
        state.wasFull = false;
        state.wasRaging = true;
        state.lastSyncedRage = 0.0f;

        spawnActivationParticles(player);

        ModNetworking.sendToPlayer(player, new RageEventPayload(ModNetworking.EVENT_ACTIVATE));
        ModNetworking.sendToPlayer(player, new RageSyncPayload(0.0f));
    }

    /** Emits a radial burst of redstone-dust particles from the player on activation. */
    private static void spawnActivationParticles(ServerPlayer player) {
        player.serverLevel().sendParticles(
            DustParticleOptions.REDSTONE,
            player.getX(),
            player.getY() + 1.0,
            player.getZ(),
            70,
            0.3,
            0.3,
            0.3,
            0.45
        );
    }

    private static void tickPlayer(ServerPlayer player, RageState state, int now) {
        if (player.isSpectator() || player.isDeadOrDying()) {
            if (state.rage != 0.0f || state.wasFull || state.wasRaging) {
                reset(player, state);
            }
            return;
        }

        NearestEnemy nearest = findNearestEnemy(player);
        boolean raging = player.hasEffect(ModMobEffects.RAGE_MODE.get());

        // Rage neither accumulates nor decays while Rage Mode is active.
        if (!raging) {
            if (nearest != null) {
                state.lastCombatTick = now;
                double fillPerSecond = rageFillPerSecond(nearest.distanceBlocks, nearest.boss);
                state.rage += (float) (fillPerSecond / 20.0);
                if (state.rage >= RageConfig.MAX_RAGE) {
                    state.rage = RageConfig.MAX_RAGE;
                    if (!state.wasFull) {
                        state.wasFull = true;
                        ModNetworking.sendToPlayer(player, new RageEventPayload(ModNetworking.EVENT_FULL));
                    }
                }
            } else if (now - state.lastCombatTick > RageConfig.outOfCombatDelayTicks) {
                state.rage -= RageConfig.decayPerSecond / 20.0f;
                if (state.rage < 0.0f) {
                    state.rage = 0.0f;
                }
            }

            if (state.rage < RageConfig.MAX_RAGE) {
                state.wasFull = false;
            }
        }

        // Detect the moment the Rage Mode effect expires (or is removed) to play the end sound.
        if (state.wasRaging && !raging) {
            ModNetworking.sendToPlayer(player, new RageEventPayload(ModNetworking.EVENT_END));
        }
        state.wasRaging = raging;

        if (Math.abs(state.rage - state.lastSyncedRage) > 0.01f) {
            state.lastSyncedRage = state.rage;
            ModNetworking.sendToPlayer(player, new RageSyncPayload(state.rage));
        }
    }

    private static void reset(ServerPlayer player, RageState state) {
        state.rage = 0.0f;
        state.wasFull = false;
        state.wasRaging = false;
        state.lastSyncedRage = 0.0f;
        ModNetworking.sendToPlayer(player, new RageSyncPayload(0.0f));
    }

    private static NearestEnemy findNearestEnemy(ServerPlayer player) {
        double range = RageConfig.detectionRangeBlocks;
        AABB aabb = player.getBoundingBox().inflate(range);

        LivingEntity nearestBoss = null;
        double nearestBossSqr = Double.MAX_VALUE;
        LivingEntity nearestEnemy = null;
        double nearestEnemySqr = Double.MAX_VALUE;

        for (LivingEntity entity : player.level().getEntities(
            EntityTypeTest.forClass(LivingEntity.class),
            aabb,
            e -> e instanceof Enemy && e.isAlive() && e != player
        )) {
            double dSqr = player.distanceToSqr(entity);
            if (isBoss(entity)) {
                if (dSqr < nearestBossSqr) {
                    nearestBossSqr = dSqr;
                    nearestBoss = entity;
                }
            } else if (dSqr < nearestEnemySqr) {
                nearestEnemySqr = dSqr;
                nearestEnemy = entity;
            }
        }

        // A boss always takes priority over ordinary hostiles when in range.
        if (nearestBoss != null) {
            double blocks = Math.sqrt(nearestBossSqr);
            if (blocks <= range) {
                return new NearestEnemy(blocks, true);
            }
        }
        if (nearestEnemy != null) {
            double blocks = Math.sqrt(nearestEnemySqr);
            if (blocks <= range) {
                return new NearestEnemy(blocks, false);
            }
        }
        return null;
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    private static double rageFillPerSecond(double blocks, boolean boss) {
        double pixelDistance = blocks * 16.0;
        double d = Math.max(0.0, Math.min(640.0, pixelDistance - 160.0));
        double factor = 1.0 / (0.034 * d + 2.0) + (590.5 - d) / 1181.0;
        double fill = factor * (RageConfig.MAX_RAGE / RageConfig.fillDenominatorSeconds) * RageConfig.fillMultiplier;
        if (boss) {
            fill *= RageConfig.bossFillMultiplier;
        }
        return fill;
    }

    private record NearestEnemy(double distanceBlocks, boolean boss) {
    }

    private static final class RageState {
        float rage;
        int lastCombatTick;
        boolean wasFull;
        boolean wasRaging;
        float lastSyncedRage;
    }
}