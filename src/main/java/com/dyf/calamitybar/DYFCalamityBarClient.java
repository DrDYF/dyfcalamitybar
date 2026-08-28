package com.dyf.calamitybar;

import com.dyf.calamitybar.client.AdrenalineHud;
import com.dyf.calamitybar.client.ConfigScreen;
import com.dyf.calamitybar.client.RageHud;
import com.dyf.calamitybar.network.ModNetworking;
import com.dyf.calamitybar.network.ModNetworking.ActivateAdrenalinePayload;
import com.dyf.calamitybar.network.ModNetworking.ActivateRagePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only mod class. Keybindings and the HUD overlay are registered on the
 * MOD bus ({@link ClientModEvents}); per-tick input handling is registered on
 * the GAME bus in the constructor ({@link ClientForgeEvents}) — replacing the
 * Fabric client entry point ({@code ClientModInitializer} +
 * {@code HudRenderCallback}).
 */
@Mod(value = DYFCalamityBar.MOD_ID, dist = Dist.CLIENT)
public final class DYFCalamityBarClient {
    private static KeyMapping rageKey;
    private static KeyMapping adrenalineKey;
    private static KeyMapping configKey;

    public DYFCalamityBarClient() {
        NeoForge.EVENT_BUS.addListener(ClientForgeEvents::onClientTick);
    }

    @EventBusSubscriber(modid = DYFCalamityBar.MOD_ID, value = Dist.CLIENT)
    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            rageKey = new KeyMapping(
                "key.dyfcalamitybar.activate_rage",
                InputConstants.KEY_G,
                "key.categories.dyfcalamitybar"
            );
            adrenalineKey = new KeyMapping(
                "key.dyfcalamitybar.activate_adrenaline",
                InputConstants.KEY_H,
                "key.categories.dyfcalamitybar"
            );
            configKey = new KeyMapping(
                "key.dyfcalamitybar.config",
                InputConstants.KEY_U,
                "key.categories.dyfcalamitybar"
            );
            event.register(rageKey);
            event.register(adrenalineKey);
            event.register(configKey);
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            // A single per-frame overlay so the meters are drawn exactly once
            // (re-rendering the shake would jitter because of the RNG offset).
            event.registerAboveAll(DYFCalamityBar.id("calamity_bars"), (graphics, deltaTracker) -> {
                float delta = deltaTracker.getGameTimeDeltaPartialTick(true);
                RageHud.render(graphics, delta);
                AdrenalineHud.render(graphics, delta);
            });
        }
    }

    /** Per-tick client input handling; registered on the game bus at construction. */
    public static final class ClientForgeEvents {
        private ClientForgeEvents() {
        }

        public static void onClientTick(ClientTickEvent.Pre event) {
            RageHud.tick();
            AdrenalineHud.tick();

            Minecraft minecraft = Minecraft.getInstance();
            while (configKey.consumeClick()) {
                minecraft.setScreen(new ConfigScreen(null));
            }
            while (rageKey.consumeClick()) {
                if (minecraft.player != null && minecraft.player.isAlive()
                    && !minecraft.player.isSpectator() && RageHud.isFull()) {
                    ModNetworking.sendToServer(new ActivateRagePayload());
                }
            }
            while (adrenalineKey.consumeClick()) {
                if (minecraft.player != null && minecraft.player.isAlive()
                    && !minecraft.player.isSpectator() && AdrenalineHud.isFull()) {
                    ModNetworking.sendToServer(new ActivateAdrenalinePayload());
                }
            }
        }
    }
}