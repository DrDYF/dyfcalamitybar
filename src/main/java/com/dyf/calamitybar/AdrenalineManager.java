package com.dyf.calamitybar;

import com.dyf.calamitybar.network.ModNetworking;
import com.dyf.calamitybar.network.ModNetworking.AdrenalineEventPayload;
import com.dyf.calamitybar.network.ModNetworking.AdrenalineSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.joml.Vector3f;

/**
 * Server-authoritative logic for the adrenaline meter. Adrenaline charges while
 * a boss is within {@link RageConfig#adrenalineDetectionRangeBlocks} blocks and
 * the player has not taken damage recently; when there is no boss nearby it
 * decays at the same proportional rate as rage. Taking any damage empties the
 * bar entirely, and a hit while the bar is full also halves that damage. Runs
 * entirely on the server thread, so the {@link #STATES} map needs no extra
 * synchronization.
 */
public final class AdrenalineManager {
    private AdrenalineManager() {
    }

    private static final Map<UUID, AdrenalineState> STATES = new HashMap<>();
    private static final Random RANDOM = new Random();

    /** Cyan-green colour for the activation particles (青绿色). */
    private static final Vector3f ADRENALINE_DUST_COLOR = new Vector3f(0.0f, 0.85f, 0.7f);

    public static void onServerTick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AdrenalineState state = STATES.computeIfAbsent(player.getUUID(), id -> new AdrenalineState());
            tickPlayer(player, state, now);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        STATES.put(player.getUUID(), new AdrenalineState());
        ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(0.0f));
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    /** Called from the C2S activation packet. */
    public static void activateAdrenaline(ServerPlayer player) {
        AdrenalineState state = STATES.computeIfAbsent(player.getUUID(), id -> new AdrenalineState());
        if (state.adrenaline < RageConfig.maxAdrenaline - 1.0f) {
            return;
        }

        player.addEffect(new MobEffectInstance(
            ModMobEffects.ADRENALINE_MODE,
            RageConfig.adrenalineModeDurationTicks(),
            0,
            false,
            true,
            true
        ));

        state.adrenaline = 0.0f;
        state.wasFull = false;
        state.lastSyncedAdrenaline = 0.0f;

        spawnActivationParticles(player);

        ModNetworking.sendToPlayer(player, new AdrenalineEventPayload(ModNetworking.ADRENALINE_EVENT_ACTIVATE));
        ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(0.0f));
    }

    /**
     * Called from the damage events whenever a player (as the victim) is about to
     * be hurt. When {@link RageConfig#adrenalineClearOnHurt} is enabled, any
     * damage empties the adrenaline bar entirely; if the bar was full, the damage
     * is also halved and the loss sound plays. When disabled, damage leaves the
     * bar untouched (charging still pauses for the configured grace).
     */
    public static float onPlayerHurt(ServerPlayer player, float damage) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return damage;
        }
        AdrenalineState state = STATES.computeIfAbsent(player.getUUID(), id -> new AdrenalineState());
        state.lastDamageTick = server.getTickCount();

        if (!RageConfig.adrenalineClearOnHurt) {
            return damage;
        }

        boolean wasFull = state.adrenaline >= RageConfig.maxAdrenaline - 1.0f;
        state.adrenaline = 0.0f;
        state.wasFull = false;
        state.lastSyncedAdrenaline = 0.0f;

        if (wasFull) {
            ModNetworking.sendToPlayer(player, new AdrenalineEventPayload(ModNetworking.ADRENALINE_EVENT_LOSS));
            ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(0.0f));
            return damage * RageConfig.adrenalineFullDamageMultiplier;
        }

        ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(0.0f));
        return damage;
    }

    private static void tickPlayer(ServerPlayer player, AdrenalineState state, int now) {
        if (player.isSpectator() || player.isDeadOrDying()) {
            if (state.adrenaline != 0.0f || state.wasFull) {
                reset(player, state);
            }
            return;
        }

        boolean adrenalining = player.hasEffect(ModMobEffects.ADRENALINE_MODE);

        // Adrenaline neither accumulates nor decays while Adrenaline Mode is active.
        if (!adrenalining && now - state.lastDamageTick >= RageConfig.adrenalineChargePauseTicks) {
            if (hasBossNearby(player)) {
                state.adrenaline += RageConfig.adrenalineFillPerSecond / 20.0f;
                if (state.adrenaline >= RageConfig.maxAdrenaline) {
                    state.adrenaline = RageConfig.maxAdrenaline;
                    if (!state.wasFull) {
                        state.wasFull = true;
                        ModNetworking.sendToPlayer(player, new AdrenalineEventPayload(ModNetworking.ADRENALINE_EVENT_FULL));
                    }
                }
            } else {
                // No boss nearby: decay at the same proportional rate as rage.
                state.adrenaline -= RageConfig.adrenalineDecayPerSecond() / 20.0f;
                if (state.adrenaline < 0.0f) {
                    state.adrenaline = 0.0f;
                }
            }
        }

        if (state.adrenaline < RageConfig.maxAdrenaline) {
            state.wasFull = false;
        }

        if (Math.abs(state.adrenaline - state.lastSyncedAdrenaline) > 5.0f) {
            state.lastSyncedAdrenaline = state.adrenaline;
            ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(state.adrenaline));
        }
    }

    private static void reset(ServerPlayer player, AdrenalineState state) {
        state.adrenaline = 0.0f;
        state.wasFull = false;
        state.lastSyncedAdrenaline = 0.0f;
        ModNetworking.sendToPlayer(player, new AdrenalineSyncPayload(0.0f));
    }

    /** True when a living entity tagged as a boss is within the configured range of the player. */
    private static boolean hasBossNearby(ServerPlayer player) {
        double range = RageConfig.adrenalineDetectionRangeBlocks;
        AABB aabb = player.getBoundingBox().inflate(range);
        var level = player.serverLevel();
        return !level.getEntitiesOfClass(
            LivingEntity.class,
            aabb,
            entity -> entity.getType().is(Tags.EntityTypes.BOSSES)
        ).isEmpty();
    }

    /**
     * Emits a lightning-bolt shaped burst of cyan-green dust particles around the
     * player on activation. The bolt runs vertically from above the player's head
     * down to their feet, with midpoint-displacement zigzag segments.
     */
    private static void spawnActivationParticles(ServerPlayer player) {
        DustParticleOptions dust = new DustParticleOptions(ADRENALINE_DUST_COLOR, 1.8f);
        Vec3 top = new Vec3(player.getX(), player.getY() + 2.6, player.getZ());
        Vec3 bottom = new Vec3(player.getX(), player.getY(), player.getZ());

        List<Vec3> points = lightningPoints(top, bottom, 4);
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            for (int s = 0; s <= 4; s++) {
                double t = s / 4.0;
                double x = a.x + (b.x - a.x) * t;
                double y = a.y + (b.y - a.y) * t;
                double z = a.z + (b.z - a.z) * t;
                player.serverLevel().sendParticles(dust, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** Midpoint-displacement lightning path between two points. */
    private static List<Vec3> lightningPoints(Vec3 a, Vec3 b, int depth) {
        if (depth <= 0) {
            List<Vec3> result = new ArrayList<>();
            result.add(a);
            result.add(b);
            return result;
        }
        Vec3 mid = a.add(b).scale(0.5);
        double len = b.subtract(a).length();
        // Horizontal zigzag offset in a random direction (works even for a vertical bolt,
        // where a perpendicular cross-product would collapse to zero).
        double offset = len * 0.25;
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        mid = mid.add(Math.cos(angle) * offset, 0.0, Math.sin(angle) * offset);
        // Slight vertical jitter so the bolt never looks perfectly flat.
        mid = mid.add(0.0, (RANDOM.nextDouble() * 2.0 - 1.0) * offset * 0.4, 0.0);

        List<Vec3> left = lightningPoints(a, mid, depth - 1);
        List<Vec3> right = lightningPoints(mid, b, depth - 1);
        List<Vec3> result = new ArrayList<>(left);
        result.remove(result.size() - 1);
        result.addAll(right);
        return result;
    }

    private static final class AdrenalineState {
        float adrenaline;
        int lastDamageTick;
        boolean wasFull;
        float lastSyncedAdrenaline;
    }
}