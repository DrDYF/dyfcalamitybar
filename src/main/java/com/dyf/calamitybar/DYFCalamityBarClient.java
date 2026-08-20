package com.dyf.calamitybar;

import com.dyf.calamitybar.client.AdrenalineHud;
import com.dyf.calamitybar.client.ConfigScreen;
import com.dyf.calamitybar.client.RageHud;
import com.dyf.calamitybar.network.ModNetworking;
import com.dyf.calamitybar.network.ModNetworking.ActivateAdrenalinePayload;
import com.dyf.calamitybar.network.ModNetworking.ActivateRagePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;

public class DYFCalamityBarClient implements ClientModInitializer {
    private static KeyMapping rageKey;
    private static KeyMapping adrenalineKey;
    private static KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        rageKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.dyfcalamitybar.activate_rage",
                InputConstants.KEY_G,
                "key.categories.dyfcalamitybar"
            )
        );
        adrenalineKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.dyfcalamitybar.activate_adrenaline",
                InputConstants.KEY_H,
                "key.categories.dyfcalamitybar"
            )
        );
        configKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.dyfcalamitybar.config",
                InputConstants.KEY_U,
                "key.categories.dyfcalamitybar"
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RageHud.tick();
            AdrenalineHud.tick();
            while (configKey.consumeClick()) {
                client.setScreen(new ConfigScreen(null));
            }
            while (rageKey.consumeClick()) {
                if (client.player != null && client.player.isAlive()
                    && !client.player.isSpectator() && RageHud.isFull()) {
                    ClientPlayNetworking.send(new ActivateRagePayload());
                }
            }
            while (adrenalineKey.consumeClick()) {
                if (client.player != null && client.player.isAlive()
                    && !client.player.isSpectator() && AdrenalineHud.isFull()) {
                    ClientPlayNetworking.send(new ActivateAdrenalinePayload());
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, deltaTracker) -> {
            float delta = deltaTracker.getGameTimeDeltaPartialTick(true);
            RageHud.render(drawContext, delta);
            AdrenalineHud.render(drawContext, delta);
        });

        ModNetworking.initClient();
    }
}