package com.dyf.calamitybar;

import com.dyf.calamitybar.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DYFCalamityBar implements ModInitializer {
    public static final String MOD_ID = "dyfcalamitybar";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        RageConfig.load();
        ModMobEffects.init();
        ModSounds.init();
        ModNetworking.initCommon();
        ModNetworking.initServer();

        ServerTickEvents.END_SERVER_TICK.register(RageManager::onServerTick);
        ServerTickEvents.END_SERVER_TICK.register(AdrenalineManager::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            RageManager.onPlayerJoin(handler.getPlayer());
            AdrenalineManager.onPlayerJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            RageManager.onPlayerDisconnect(handler.getPlayer());
            AdrenalineManager.onPlayerDisconnect(handler.getPlayer());
        });

        LOGGER.info("DYFCalamityBar initialized.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
