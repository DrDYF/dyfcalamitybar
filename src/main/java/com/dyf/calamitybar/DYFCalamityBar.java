package com.dyf.calamitybar;

import com.dyf.calamitybar.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DYFCalamityBar.MOD_ID)
public class DYFCalamityBar {
    public static final String MOD_ID = "dyfcalamitybar";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public DYFCalamityBar() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registry contents (mob effects + sound events) via DeferredRegister.
        ModMobEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);

        // Runtime config: create / load config/dyfcalamitybar.json.
        RageConfig.load();

        // Common networking: register the channel and all payload types/handlers.
        ModNetworking.initCommon();

        LOGGER.info("DYFCalamityBar initialized.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Server-side (Forge bus) event handlers: tick loops, join/disconnect
     * bookkeeping and the damage hooks that replace the Fabric damage mixin.
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class CommonForgeEvents {
        private CommonForgeEvents() {
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            RageManager.onServerTick(event.getServer());
            AdrenalineManager.onServerTick(event.getServer());
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                RageManager.onPlayerJoin(player);
                AdrenalineManager.onPlayerJoin(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                RageManager.onPlayerDisconnect(player);
                AdrenalineManager.onPlayerDisconnect(player);
            }
        }

        /**
         * Fired once per actually-applied hit ({@code actuallyHurt}), mirroring
         * the Fabric {@code LivingEntityMixin}: rage tracking + multiplicative
         * damage bonuses for a player attacker, and adrenaline reaction for a
         * player victim.
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onLivingDamage(LivingDamageEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide) {
                return;
            }
            DamageSource source = event.getSource();
            if (source.getEntity() instanceof Player attacker) {
                RageManager.onPlayerDealtDamage(attacker);
                float amount = event.getAmount();
                if (attacker.hasEffect(ModMobEffects.RAGE_MODE.get())) {
                    amount *= RageConfig.rageDamageMultiplier;
                }
                if (attacker.hasEffect(ModMobEffects.ADRENALINE_MODE.get())) {
                    amount *= RageConfig.adrenalineDamageMultiplier;
                }
                event.setAmount(amount);
            }
            if (victim instanceof ServerPlayer receiver) {
                event.setAmount(AdrenalineManager.onPlayerHurt(receiver, event.getAmount()));
            }
        }
    }
}