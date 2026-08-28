package com.dyf.calamitybar;

import com.dyf.calamitybar.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Entry point of the DYF Calamity Bar NeoForge mod. Registries are declared as
 * DeferredRegisters and registered on the mod event bus; gameplay hooks (server
 * tick, player join/leave, damage) live on the game event bus via
 * {@link CommonEvents} (NeoForge's auto-subscriber substitutes for Fabric's
 * mixin-based {@code actuallyHurt} hook).
 */
@Mod(DYFCalamityBar.MOD_ID)
public final class DYFCalamityBar {
    // The value here must match the modId in META-INF/neoforge.mods.toml.
    public static final String MOD_ID = "dyfcalamitybar";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your
    // mod is loaded. FML recognizes parameter types like IEventBus and injects
    // them automatically.
    public DYFCalamityBar(IEventBus modEventBus) {
        ModMobEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        RageConfig.load();

        modEventBus.addListener(ModNetworking::registerPayloads);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onLivingDamage);

        LOGGER.info("DYFCalamityBar initialized.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Gameplay hooks on the game event bus, registered manually in the mod
     * constructor. Damage handling mirrors the Fabric original
     * {@code LivingEntityMixin} (which wrapped {@code actuallyHurt}):
     * {@link LivingDamageEvent.Pre} fires once per hit inside
     * {@code actuallyHurt} after armor/potion reductions, and its
     * {@code DamageContainer} can still modify the final amount, so the rage /
     * adrenaline damage multipliers apply identically.
     */
    public static final class CommonEvents {
        private CommonEvents() {
        }

        /** Fired on {@link ServerTickEvent.Post} after the server ticked. */
        public static void onServerTick(ServerTickEvent.Post event) {
            if (event.getServer() != null) {
                RageManager.onServerTick(event.getServer());
                AdrenalineManager.onServerTick(event.getServer());
            }
        }

        /** Fired when a player joins the server. */
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                RageManager.onPlayerJoin(player);
                AdrenalineManager.onPlayerJoin(player);
            }
        }

        /** Fired when a player disconnects. */
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                RageManager.onPlayerDisconnect(player);
                AdrenalineManager.onPlayerDisconnect(player);
            }
        }

        /** Fired on {@link LivingDamageEvent.Pre} as damage is about to be applied. */
        public static void onLivingDamage(LivingDamageEvent.Pre event) {
            LivingEntity entity = event.getEntity();
            DamageSource source = event.getSource();

            // Attacker side: rage / adrenaline damage multipliers (multiplicative,
            // +35% / +150%), and refresh the rage in-combat timer.
            if (source.getEntity() instanceof Player attacker && attacker != entity) {
                if (attacker.hasEffect(ModMobEffects.RAGE_MODE)) {
                    event.setNewDamage(event.getNewDamage() * RageConfig.rageDamageMultiplier);
                }
                if (attacker.hasEffect(ModMobEffects.ADRENALINE_MODE)) {
                    event.setNewDamage(event.getNewDamage() * RageConfig.adrenalineDamageMultiplier);
                }
                RageManager.onPlayerDealtDamage(attacker);
            }

            // Victim side: adrenaline clear-on-hurt and the full-bar damage absorb.
            if (entity instanceof ServerPlayer victim) {
                event.setNewDamage(AdrenalineManager.onPlayerHurt(victim, event.getNewDamage()));
            }
        }
    }
}