package com.dyf.calamitybar.network;

import com.dyf.calamitybar.AdrenalineManager;
import com.dyf.calamitybar.DYFCalamityBar;
import com.dyf.calamitybar.RageManager;
import com.dyf.calamitybar.client.AdrenalineHud;
import com.dyf.calamitybar.client.RageHud;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * All network payloads for the rage mechanic. Payload types are registered in
 * {@link #initCommon()} on both the client and server; receivers are then
 * registered separately for each side.
 */
public final class ModNetworking {
    private ModNetworking() {
    }

    // Rage event kinds carried by RageEventPayload.
    public static final byte EVENT_FULL = 0;
    public static final byte EVENT_ACTIVATE = 1;
    public static final byte EVENT_END = 2;

    // Adrenaline event kinds carried by AdrenalineEventPayload.
    public static final byte ADRENALINE_EVENT_FULL = 0;
    public static final byte ADRENALINE_EVENT_ACTIVATE = 1;
    public static final byte ADRENALINE_EVENT_LOSS = 2;

    /** Server -> client: current rage meter value (0-100). */
    public record RageSyncPayload(float rage) implements CustomPacketPayload {
        public static final Type<RageSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "rage_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RageSyncPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, RageSyncPayload::rage, RageSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: one-shot sound cue (full / activate / end). */
    public record RageEventPayload(byte event) implements CustomPacketPayload {
        public static final Type<RageEventPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "rage_event"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RageEventPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, RageEventPayload::event, RageEventPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: request to activate Rage Mode. */
    public record ActivateRagePayload() implements CustomPacketPayload {
        public static final Type<ActivateRagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "activate_rage"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivateRagePayload> CODEC =
            StreamCodec.unit(new ActivateRagePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: current adrenaline meter value (0-10000). */
    public record AdrenalineSyncPayload(float adrenaline) implements CustomPacketPayload {
        public static final Type<AdrenalineSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "adrenaline_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdrenalineSyncPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, AdrenalineSyncPayload::adrenaline, AdrenalineSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: one-shot adrenaline sound cue (full / activate / loss). */
    public record AdrenalineEventPayload(byte event) implements CustomPacketPayload {
        public static final Type<AdrenalineEventPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "adrenaline_event"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdrenalineEventPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, AdrenalineEventPayload::event, AdrenalineEventPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: request to activate Adrenaline Mode. */
    public record ActivateAdrenalinePayload() implements CustomPacketPayload {
        public static final Type<ActivateAdrenalinePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "activate_adrenaline"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAdrenalinePayload> CODEC =
            StreamCodec.unit(new ActivateAdrenalinePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Registers payload codecs. Called from the common initializer on both sides. */
    public static void initCommon() {
        PayloadTypeRegistry.playS2C().register(RageSyncPayload.TYPE, RageSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RageEventPayload.TYPE, RageEventPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActivateRagePayload.TYPE, ActivateRagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdrenalineSyncPayload.TYPE, AdrenalineSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdrenalineEventPayload.TYPE, AdrenalineEventPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActivateAdrenalinePayload.TYPE, ActivateAdrenalinePayload.CODEC);
    }

    /** Registers server-side receivers. Called from the common initializer (logical server). */
    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(ActivateRagePayload.TYPE,
            (payload, context) -> RageManager.activateRage(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ActivateAdrenalinePayload.TYPE,
            (payload, context) -> AdrenalineManager.activateAdrenaline(context.player()));
    }

    /** Registers client-side receivers. Called from the client initializer. */
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(RageSyncPayload.TYPE,
            (payload, context) -> RageHud.setRage(payload.rage()));
        ClientPlayNetworking.registerGlobalReceiver(RageEventPayload.TYPE,
            (payload, context) -> RageHud.handleEvent(payload.event()));
        ClientPlayNetworking.registerGlobalReceiver(AdrenalineSyncPayload.TYPE,
            (payload, context) -> AdrenalineHud.setAdrenaline(payload.adrenaline()));
        ClientPlayNetworking.registerGlobalReceiver(AdrenalineEventPayload.TYPE,
            (payload, context) -> AdrenalineHud.handleEvent(payload.event()));
    }
}
