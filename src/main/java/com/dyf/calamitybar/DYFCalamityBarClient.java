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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side entry point: keybindings, the client tick loop (HUD animation +
 * key consumption) and HUD rendering, wired to the two Forge event buses.
 */
public final class DYFCalamityBarClient {
    private static KeyMapping rageKey;
    private static KeyMapping adrenalineKey;
    private static KeyMapping configKey;

    private DYFCalamityBarClient() {
    }

    @Mod.EventBusSubscriber(modid = DYFCalamityBar.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

        /** Registers a custom HUD overlay that renders once per frame, after all vanilla elements. */
        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("calamity_bars", (gui, graphics, partialTick, width, height) -> {
                RageHud.render(graphics, partialTick);
                AdrenalineHud.render(graphics, partialTick);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = DYFCalamityBar.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientForgeEvents {
        private ClientForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            RageHud.tick();
            AdrenalineHud.tick();

            while (configKey.consumeClick()) {
                client.setScreen(new ConfigScreen(null));
            }
            while (rageKey.consumeClick()) {
                if (client.player != null && client.player.isAlive()
                    && !client.player.isSpectator() && RageHud.isFull()) {
                    ModNetworking.sendToServer(new ActivateRagePayload());
                }
            }
            while (adrenalineKey.consumeClick()) {
                if (client.player != null && client.player.isAlive()
                    && !client.player.isSpectator() && AdrenalineHud.isFull()) {
                    ModNetworking.sendToServer(new ActivateAdrenalinePayload());
                }
            }
        }
    }
}