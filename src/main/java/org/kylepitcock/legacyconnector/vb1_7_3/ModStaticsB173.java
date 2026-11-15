package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.netty.buffer.Unpooled;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Beta mod keeps game state in plain static fields of its mod class (a fine a lawyer charges, drawn over his head). In Beta's single player
 * the renderer read the very field the game changed; a dedicated server's client has its own copy. The server sends the values of each mod
 * class's non-final static number, boolean and string fields to every client when they change (and all of them when a player joins), and the
 * client sets them on its copy of the class. An integrated server shares the classes with its client and sends nothing.
 */
public final class ModStaticsB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final String FAMILY = "b1.7.3-statics";
    private static final int MAX_FIELDS = 256, MAX_STRING = 1024, MAX_BYTES = 65_536;
    private static final Map<Class<?>, List<Field>> FIELDS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Class<?>, CompoundTag> SENT = new HashMap<>();
    private static boolean installed;

    private ModStaticsB173() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        LegacyNetwork.registerReceiver(FAMILY, ModStaticsB173::receive);
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            if (event.getServer().isDedicatedServer() && event.getServer().getTickCount() % 5 == 0) broadcast(event.getServer(), false, null);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player && player.server.isDedicatedServer()) broadcast(player.server, true, player);
        });
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> { synchronized (ModStaticsB173.class) { SENT.clear(); } });
    }

    /** The fields mirrored for a mod class: its own non-final static numbers, booleans, chars and strings. */
    private static List<Field> fields(Class<?> type) {
        return FIELDS.computeIfAbsent(type, t -> {
            var out = new ArrayList<Field>();
            for (Field field : t.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.isSynthetic() || field.getName().startsWith("mcc$")) continue;
                Class<?> kind = field.getType();
                if (!(kind.isPrimitive() || kind == String.class)) continue;
                try { field.setAccessible(true); } catch (RuntimeException e) { continue; }
                out.add(field);
                if (out.size() >= MAX_FIELDS) break;
            }
            return List.copyOf(out);
        });
    }

    private static CompoundTag read(Class<?> type) {
        var tag = new CompoundTag();
        for (Field field : fields(type)) {
            try {
                Object value = field.get(null);
                Class<?> kind = field.getType();
                String name = field.getName();
                if (kind == int.class) tag.putInt(name, (Integer) value);
                else if (kind == long.class) tag.putLong(name, (Long) value);
                else if (kind == boolean.class) tag.putBoolean(name, (Boolean) value);
                else if (kind == float.class) tag.putFloat(name, (Float) value);
                else if (kind == double.class) tag.putDouble(name, (Double) value);
                else if (kind == short.class) tag.putShort(name, (Short) value);
                else if (kind == byte.class) tag.putByte(name, (Byte) value);
                else if (kind == char.class) tag.putInt(name, (Character) value);
                else if (value instanceof String text && text.length() <= MAX_STRING) tag.putString(name, text);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // a field that cannot be read is not mirrored
            }
        }
        return tag;
    }

    private static synchronized void broadcast(MinecraftServer server, boolean all, ServerPlayer only) {
        for (BaseMod mod : ContentB173.loadedMods()) {
            Class<?> type = mod.getClass();
            CompoundTag now = read(type);
            CompoundTag previous = SENT.get(type);
            CompoundTag changes = new CompoundTag();
            for (String key : now.getAllKeys()) if (all || previous == null || !now.get(key).equals(previous.get(key))) changes.put(key, now.get(key).copy());
            if (!all) SENT.put(type, now);
            if (changes.isEmpty() || previous == null && !all) continue; // the first look is the baseline both sides loaded with
            var payload = packet(type.getName(), changes);
            if (payload == null) continue;
            if (only != null) { if (hasChannel(only)) PacketDistributor.sendToPlayer(only, payload); }
            else for (ServerPlayer player : server.getPlayerList().getPlayers()) if (hasChannel(player)) PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static boolean hasChannel(ServerPlayer player) {
        try { return player.connection != null && player.connection.hasChannel(LegacyPayload.TYPE); }
        catch (NullPointerException e) { return false; }
    }

    private static LegacyPayload packet(String type, CompoundTag values) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeNbt(values);
            if (buffer.readableBytes() > MAX_BYTES) return null;
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return new LegacyPayload(FAMILY, type, 0, bytes);
        } finally { buffer.release(); }
    }

    private static void receive(LegacyPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!context.flow().isClientbound() || payload.data().length > MAX_BYTES) return;
        CompoundTag values;
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        try { values = buffer.readNbt(); } catch (RuntimeException e) { return; } finally { buffer.release(); }
        if (values == null) return;
        context.enqueueWork(() -> apply(payload.channel(), values));
    }

    private static void apply(String typeName, CompoundTag values) {
        Class<?> type = null;
        for (BaseMod mod : ContentB173.loadedMods()) if (mod.getClass().getName().equals(typeName)) type = mod.getClass();
        if (type == null) return;
        for (Field field : fields(type)) {
            String name = field.getName();
            if (!values.contains(name)) continue;
            try {
                Class<?> kind = field.getType();
                if (kind == int.class) field.setInt(null, values.getInt(name));
                else if (kind == long.class) field.setLong(null, values.getLong(name));
                else if (kind == boolean.class) field.setBoolean(null, values.getBoolean(name));
                else if (kind == float.class) field.setFloat(null, values.getFloat(name));
                else if (kind == double.class) field.setDouble(null, values.getDouble(name));
                else if (kind == short.class) field.setShort(null, values.getShort(name));
                else if (kind == byte.class) field.setByte(null, values.getByte(name));
                else if (kind == char.class) field.setChar(null, (char) values.getInt(name));
                else if (kind == String.class) field.set(null, values.getString(name));
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.debug("b1.7.3 mod field {}.{} not mirrored: {}", typeName, name, e.toString());
            }
        }
    }
}
