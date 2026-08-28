package com.dyf.calamitybar.network;

import com.dyf.calamitybar.AdrenalineManager;
import com.dyf.calamitybar.DYFCalamityBar;
import com.dyf.calamitybar.RageManager;
import com.dyf.calamitybar.client.AdrenalineHud;
import com.dyf.calamitybar.client.RageHud;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * All network payloads for the rage/adrenaline mechanics. A single Forge
 * {@link SimpleChannel} carries every message; the direction of each payload is
 * decided by how it is sent (client-&gt;server via {@link #sendToServer} or
 * server-&gt;client via {@link #sendToPlayer}), and handlers self-route on the
 * {@link NetworkEvent.Context} direction.
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

    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;
    private static int nextId = 0;

    /** Server -> client: current rage meter value (0-100). */
    public record RageSyncPayload(float rage) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeFloat(rage);
        }

        public static RageSyncPayload decode(FriendlyByteBuf buf) {
            return new RageSyncPayload(buf.readFloat());
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    RageHud.setRage(rage);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Server -> client: one-shot sound cue (full / activate / end). */
    public record RageEventPayload(byte event) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(event);
        }

        public static RageEventPayload decode(FriendlyByteBuf buf) {
            return new RageEventPayload(buf.readByte());
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    RageHud.handleEvent(event);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Client -> server: request to activate Rage Mode. */
    public record ActivateRagePayload() {
        public void encode(FriendlyByteBuf buf) {
        }

        public static ActivateRagePayload decode(FriendlyByteBuf buf) {
            return new ActivateRagePayload();
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        RageManager.activateRage(player);
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Server -> client: current adrenaline meter value (0-maxAdrenaline). */
    public record AdrenalineSyncPayload(float adrenaline) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeFloat(adrenaline);
        }

        public static AdrenalineSyncPayload decode(FriendlyByteBuf buf) {
            return new AdrenalineSyncPayload(buf.readFloat());
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    AdrenalineHud.setAdrenaline(adrenaline);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Server -> client: one-shot adrenaline sound cue (full / activate / loss). */
    public record AdrenalineEventPayload(byte event) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(event);
        }

        public static AdrenalineEventPayload decode(FriendlyByteBuf buf) {
            return new AdrenalineEventPayload(buf.readByte());
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    AdrenalineHud.handleEvent(event);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Client -> server: request to activate Adrenaline Mode. */
    public record ActivateAdrenalinePayload() {
        public void encode(FriendlyByteBuf buf) {
        }

        public static ActivateAdrenalinePayload decode(FriendlyByteBuf buf) {
            return new ActivateAdrenalinePayload();
        }

        public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        AdrenalineManager.activateAdrenaline(player);
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * Registers the channel and all payload types. Called once from the common
     * {@code @Mod} constructor, on both physical sides, before any play traffic.
     */
    public static void initCommon() {
        channel = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
        channel.registerMessage(nextId++, RageSyncPayload.class,
            RageSyncPayload::encode, RageSyncPayload::decode, RageSyncPayload::handle);
        channel.registerMessage(nextId++, RageEventPayload.class,
            RageEventPayload::encode, RageEventPayload::decode, RageEventPayload::handle);
        channel.registerMessage(nextId++, ActivateRagePayload.class,
            ActivateRagePayload::encode, ActivateRagePayload::decode, ActivateRagePayload::handle);
        channel.registerMessage(nextId++, AdrenalineSyncPayload.class,
            AdrenalineSyncPayload::encode, AdrenalineSyncPayload::decode, AdrenalineSyncPayload::handle);
        channel.registerMessage(nextId++, AdrenalineEventPayload.class,
            AdrenalineEventPayload::encode, AdrenalineEventPayload::decode, AdrenalineEventPayload::handle);
        channel.registerMessage(nextId++, ActivateAdrenalinePayload.class,
            ActivateAdrenalinePayload::encode, ActivateAdrenalinePayload::decode, ActivateAdrenalinePayload::handle);
    }

    /** Sends a server-&gt;client payload to a single player. */
    public static void sendToPlayer(ServerPlayer player, Object message) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** Sends a client-&gt;server payload from the local client. */
    public static void sendToServer(Object message) {
        channel.sendToServer(message);
    }
}