package legacyconnector.v1_7_10.codechicken.lib.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.codechicken.lib.data.MCDataInput;
import legacyconnector.v1_7_10.codechicken.lib.data.MCDataOutput;
import legacyconnector.v1_7_10.codechicken.lib.vec.BlockCoord;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLEventChannel;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;

/**
 * CodeChickenLib's packet type: a channel (named after the owning mod), a type byte (1 to 127) and a payload written with the
 * {@link MCDataOutput} methods. Packets travel as FML custom payloads on a per-channel event channel; {@link #assignHandler}
 * registers the handlers that receive them per side.
 */
public final class PacketCustom implements MCDataInput, MCDataOutput {
    public interface ICustomPacketHandler {
    }

    public interface IClientPacketHandler extends ICustomPacketHandler {
        void handlePacket(PacketCustom packetCustom, Minecraft mc, INetHandlerPlayClient handler);
    }

    public interface IServerPacketHandler extends ICustomPacketHandler {
        void handlePacket(PacketCustom packetCustom, EntityPlayerMP sender, INetHandlerPlayServer handler);
    }

    private static final Map<String, FMLEventChannel> CHANNELS = new ConcurrentHashMap<>();
    private static final Map<String, Dispatcher> DISPATCHERS = new ConcurrentHashMap<>();

    private final ByteBuf buf;
    private String channel;
    private int type;

    /** A packet to send: channelKey is the mod instance, container or channel name; type 1 to 127. */
    public PacketCustom(Object channelKey, int type) {
        if (type <= 0 || type >= 0x80) throw new IllegalArgumentException("Packet type: " + type + " is not within the valid range (1->127)");
        this.channel = channelName(channelKey);
        this.type = type;
        this.buf = Unpooled.buffer();
        buf.writeByte(type);
    }

    /** A received packet: reads the type byte. */
    public PacketCustom(ByteBuf payload) {
        this.buf = payload;
        this.type = payload.readUnsignedByte() & 0x7F;
    }

    private PacketCustom(ByteBuf payload, String channel) {
        this(payload);
        this.channel = channel;
    }

    public int getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public ByteBuf getByteBuf() {
        return buf;
    }

    public PacketCustom compress() {
        return this;
    }

    /** The channel name FML would use for the key: a string is the name, a mod instance or container its id. */
    public static String channelName(Object key) {
        if (key instanceof String s) return s;
        if (key instanceof ModContainer c) return c.getModId();
        ModContainer c = Loader.instance().getReversedModObjectList().get(key);
        if (c != null) return c.getModId();
        c = Loader.instance().activeModContainer();
        return c != null ? c.getModId() : String.valueOf(key);
    }

    private static FMLEventChannel channel(String name) {
        return CHANNELS.computeIfAbsent(name, n -> {
            FMLEventChannel ch = NetworkRegistry.INSTANCE.newEventDrivenChannel(n);
            Dispatcher d = new Dispatcher();
            DISPATCHERS.put(n, d);
            ch.register(d);
            return ch;
        });
    }

    /** Registers the handler for all packet types of the channel (one handler per side). */
    public static void assignHandler(Object channelKey, ICustomPacketHandler handler) {
        assignHandler(channelKey, 0, 0x7F, handler);
    }

    /** Registers the handler for the packet types firstID to lastID of the channel. */
    public static void assignHandler(Object channelKey, int firstID, int lastID, ICustomPacketHandler handler) {
        String name = channelName(channelKey);
        channel(name);
        Dispatcher d = DISPATCHERS.get(name);
        if (handler instanceof IClientPacketHandler c) d.client.add(new Range<>(firstID, lastID, c));
        if (handler instanceof IServerPacketHandler s) d.server.add(new Range<>(firstID, lastID, s));
    }

    private record Range<T>(int first, int last, T handler) {
        boolean covers(int type) {
            return type >= first && type <= last;
        }
    }

    /** Receives the packets of one channel on both sides and hands them to the handlers registered for the type. */
    public static final class Dispatcher {
        final java.util.List<Range<IClientPacketHandler>> client = new java.util.concurrent.CopyOnWriteArrayList<>();
        final java.util.List<Range<IServerPacketHandler>> server = new java.util.concurrent.CopyOnWriteArrayList<>();

        @SubscribeEvent
        public void onClient(FMLNetworkEvent.ClientCustomPacketEvent event) {
            PacketCustom packet = new PacketCustom(event.packet.payload().duplicate(), event.packet.channel());
            for (Range<IClientPacketHandler> r : client) {
                if (r.covers(packet.type)) {
                    r.handler.handlePacket(packet, Minecraft.getMinecraft(), (INetHandlerPlayClient) event.handler);
                    return;
                }
            }
        }

        @SubscribeEvent
        public void onServer(FMLNetworkEvent.ServerCustomPacketEvent event) {
            PacketCustom packet = new PacketCustom(event.packet.payload().duplicate(), event.packet.channel());
            EntityPlayerMP sender = event.handler instanceof NetHandlerPlayServer h ? h.playerEntity : null;
            for (Range<IServerPacketHandler> r : server) {
                if (r.covers(packet.type)) {
                    r.handler.handlePacket(packet, sender, (INetHandlerPlayServer) event.handler);
                    return;
                }
            }
        }
    }

    public FMLProxyPacket toPacket() {
        return new FMLProxyPacket(buf.duplicate(), channel);
    }

    // ---- sending

    public void sendToServer() {
        channel(channel).sendToServer(toPacket());
    }

    public void sendToPlayer(EntityPlayer player) {
        channel(channel).sendTo(toPacket(), (EntityPlayerMP) player);
    }

    public void sendToClients() {
        channel(channel).sendToAll(toPacket());
    }

    public void sendToDimension(int dimension) {
        channel(channel).sendToDimension(toPacket(), dimension);
    }

    public void sendToAllAround(double x, double y, double z, double range, int dim) {
        channel(channel).sendToAllAround(toPacket(), new NetworkRegistry.TargetPoint(dim, x, y, z, range));
    }

    public void sendToChunk(World world, int chunkX, int chunkZ) {
        sendToAllAround(chunkX * 16 + 8, 128, chunkZ * 16 + 8, 160, world.provider.dimensionId);
    }

    public void sendToOps() {
        sendToOps(toPacket());
    }

    public static void sendToServer(Packet packet) {
        channel(((FMLProxyPacket) packet).channel()).sendToServer((FMLProxyPacket) packet);
    }

    public static void sendToPlayer(Packet packet, EntityPlayer player) {
        channel(((FMLProxyPacket) packet).channel()).sendTo((FMLProxyPacket) packet, (EntityPlayerMP) player);
    }

    public static void sendToClients(Packet packet) {
        channel(((FMLProxyPacket) packet).channel()).sendToAll((FMLProxyPacket) packet);
    }

    public static void sendToDimension(Packet packet, int dimension) {
        channel(((FMLProxyPacket) packet).channel()).sendToDimension((FMLProxyPacket) packet, dimension);
    }

    public static void sendToAllAround(Packet packet, double x, double y, double z, double range, int dim) {
        channel(((FMLProxyPacket) packet).channel()).sendToAllAround((FMLProxyPacket) packet, new NetworkRegistry.TargetPoint(dim, x, y, z, range));
    }

    public static void sendToChunk(Packet packet, World world, int chunkX, int chunkZ) {
        sendToAllAround(packet, chunkX * 16 + 8, 128, chunkZ * 16 + 8, 160, world.provider.dimensionId);
    }

    public static void sendToOps(Packet packet) {
        var cfg = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer().getConfigurationManager();
        for (Object o : legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager.mcc$get$playerEntityList(cfg)) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (cfg.func_152596_g(p.getGameProfile())) sendToPlayer(packet, p);
        }
    }

    // ---- writing

    public PacketCustom writeBoolean(boolean b) {
        buf.writeBoolean(b);
        return this;
    }

    public PacketCustom writeByte(int b) {
        buf.writeByte(b);
        return this;
    }

    public PacketCustom writeShort(int s) {
        buf.writeShort(s);
        return this;
    }

    public PacketCustom writeInt(int i) {
        buf.writeInt(i);
        return this;
    }

    public PacketCustom writeFloat(float f) {
        buf.writeFloat(f);
        return this;
    }

    public PacketCustom writeDouble(double d) {
        buf.writeDouble(d);
        return this;
    }

    public PacketCustom writeLong(long l) {
        buf.writeLong(l);
        return this;
    }

    public PacketCustom writeChar(char c) {
        buf.writeChar(c);
        return this;
    }

    public PacketCustom writeVarShort(int s) {
        MCDataOutput.super.writeVarShort(s);
        return this;
    }

    public PacketCustom writeVarInt(int i) {
        MCDataOutput.super.writeVarInt(i);
        return this;
    }

    public PacketCustom writeByteArray(byte[] barray) {
        buf.writeBytes(barray);
        return this;
    }

    public PacketCustom writeString(String s) {
        MCDataOutput.super.writeString(s);
        return this;
    }

    public PacketCustom writeCoord(int x, int y, int z) {
        MCDataOutput.super.writeCoord(x, y, z);
        return this;
    }

    public PacketCustom writeCoord(BlockCoord c) {
        MCDataOutput.super.writeCoord(c);
        return this;
    }

    public PacketCustom writeNBTTagCompound(NBTTagCompound tag) {
        MCDataOutput.super.writeNBTTagCompound(tag);
        return this;
    }

    public PacketCustom writeItemStack(ItemStack stack) {
        MCDataOutput.super.writeItemStack(stack);
        return this;
    }

    public PacketCustom writeItemStack(ItemStack stack, boolean large) {
        MCDataOutput.super.writeItemStack(stack, large);
        return this;
    }

    public PacketCustom writeFluidStack(FluidStack fluid) {
        MCDataOutput.super.writeFluidStack(fluid);
        return this;
    }

    // ---- reading

    public long readLong() {
        return buf.readLong();
    }

    public int readInt() {
        return buf.readInt();
    }

    public short readShort() {
        return buf.readShort();
    }

    public int readUShort() {
        return buf.readUnsignedShort();
    }

    public byte readByte() {
        return buf.readByte();
    }

    public short readUByte() {
        return buf.readUnsignedByte();
    }

    public double readDouble() {
        return buf.readDouble();
    }

    public float readFloat() {
        return buf.readFloat();
    }

    public boolean readBoolean() {
        return buf.readBoolean();
    }

    public char readChar() {
        return buf.readChar();
    }

    public int readVarShort() {
        int low = readUShort();
        int high = 0;
        if ((low & 0x8000) != 0) {
            low &= 0x7FFF;
            high = readUByte();
        }
        return high << 15 | low;
    }

    public int readVarInt() {
        int i = 0;
        int shift = 0;
        while (true) {
            int b = readUByte();
            i |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return i;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        }
    }

    public byte[] readByteArray(int length) {
        byte[] b = new byte[length];
        buf.readBytes(b);
        return b;
    }

    public String readString() {
        return new String(readByteArray(readVarInt()), java.nio.charset.StandardCharsets.UTF_8);
    }

    public String toString() {
        return "Packet(" + channel + ":" + type + ", " + buf.readableBytes() + " bytes)";
    }
}
