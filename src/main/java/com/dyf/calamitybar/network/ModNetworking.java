package com.dyf.calamitybar.network;

import com.dyf.calamitybar.AdrenalineManager;
import com.dyf.calamitybar.DYFCalamityBar;
import com.dyf.calamitybar.RageManager;
import com.dyf.calamitybar.client.AdrenalineHud;
import com.dyf.calamitybar.client.RageHud;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * All network payloads for the rage and adrenaline mechanics, using the
 * vanilla {@link CustomPacketPayload} system that NeoForge 1.21.1 builds on.
 * The payload records and stream codecs are byte-identical to the Fabric 1.21.1
 * version; only the registration ({@link RegisterPayloadHandlersEvent}) and the
 * send helpers ({@link PacketDistributor}) are NeoForge-specific.
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

    /**
     * Registers every payload and its receiver. Called from the mod constructor
     * on the mod event bus ({@link RegisterPayloadHandlersEvent}).
     */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Handlers default to HandlerThread.MAIN, so they run on the game thread
        // and may touch game state directly.
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(RageSyncPayload.TYPE, RageSyncPayload.CODEC,
            (payload, context) -> RageHud.setRage(payload.rage()));
        registrar.playToClient(RageEventPayload.TYPE, RageEventPayload.CODEC,
            (payload, context) -> RageHud.handleEvent(payload.event()));
        registrar.playToServer(ActivateRagePayload.TYPE, ActivateRagePayload.CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer sender) {
                    RageManager.activateRage(sender);
                }
            });

        registrar.playToClient(AdrenalineSyncPayload.TYPE, AdrenalineSyncPayload.CODEC,
            (payload, context) -> AdrenalineHud.setAdrenaline(payload.adrenaline()));
        registrar.playToClient(AdrenalineEventPayload.TYPE, AdrenalineEventPayload.CODEC,
            (payload, context) -> AdrenalineHud.handleEvent(payload.event()));
        registrar.playToServer(ActivateAdrenalinePayload.TYPE, ActivateAdrenalinePayload.CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer sender) {
                    AdrenalineManager.activateAdrenaline(sender);
                }
            });
    }

    /** Server -> client payload; safe from any thread (packets are flushed on the main thread). */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /** Client -> server payload. */
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}