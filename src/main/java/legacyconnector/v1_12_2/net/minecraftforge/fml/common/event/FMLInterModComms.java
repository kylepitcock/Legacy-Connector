package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import org.kylepitcock.legacyconnector.v1_12_2.LegacyModContainer1122;

/** Inter-mod messages between legacy mods, delivered at the IMC phase (after init, before postInit). */
public class FMLInterModComms {
    private static final Map<String, List<IMCMessage>> QUEUE = new ConcurrentHashMap<>();

    public static boolean sendMessage(String modId, String key, NBTTagCompound value) {
        return enqueue(modId, new IMCMessage(key, value));
    }

    public static boolean sendMessage(String modId, String key, ItemStack value) {
        return enqueue(modId, new IMCMessage(key, value));
    }

    public static boolean sendMessage(String modId, String key, String value) {
        return enqueue(modId, new IMCMessage(key, value));
    }

    public static boolean sendMessage(String modId, String key, legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation value) {
        return enqueue(modId, new IMCMessage(key, value));
    }

    public static void sendRuntimeMessage(Object sourceMod, String modId, String key, String value) {
        enqueue(modId, new IMCMessage(key, value));
    }

    public static void sendRuntimeMessage(Object sourceMod, String modId, String key, NBTTagCompound value) {
        enqueue(modId, new IMCMessage(key, value));
    }

    public static boolean sendFunctionMessage(String modId, String key, String functionClassName) {
        return enqueue(modId, new IMCMessage(key, functionClassName));
    }

    private static boolean enqueue(String modId, IMCMessage message) {
        if (!Loader.isModLoaded(modId)) return false;
        var active = LegacyModContainer1122.active();
        message.sender = active == null ? "unknown" : active.getModId();
        QUEUE.computeIfAbsent(modId.toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>()).add(message);
        return true;
    }

    public static com.google.common.collect.ImmutableList<IMCMessage> fetchRuntimeMessages(Object forMod) {
        String id = null;
        for (var record : org.kylepitcock.legacyconnector.core.LegacyMods.all()) if (record.instance == forMod) id = record.legacyId;
        if (id == null) return com.google.common.collect.ImmutableList.of();
        List<IMCMessage> list = QUEUE.remove(id.toLowerCase(java.util.Locale.ROOT));
        return list == null ? com.google.common.collect.ImmutableList.of() : com.google.common.collect.ImmutableList.copyOf(list);
    }

    public static List<IMCMessage> mcc$drain(String legacyId) {
        List<IMCMessage> list = QUEUE.remove(legacyId.toLowerCase(java.util.Locale.ROOT));
        return list == null ? new ArrayList<>() : list;
    }

    public static class IMCEvent extends FMLEvent {
        private final List<IMCMessage> messages;

        public IMCEvent(List<IMCMessage> messages) {
            this.messages = messages;
        }

        public com.google.common.collect.ImmutableList<IMCMessage> getMessages() {
            return com.google.common.collect.ImmutableList.copyOf(messages);
        }
    }

    public static final class IMCMessage {
        public final String key;
        private final Object value;
        private String sender;

        IMCMessage(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getSender() {
            return sender;
        }

        public String getStringValue() {
            return (String) value;
        }

        public NBTTagCompound getNBTValue() {
            return (NBTTagCompound) value;
        }

        public ItemStack getItemStackValue() {
            return (ItemStack) value;
        }

        public legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getResourceLocationValue() {
            return (legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation) value;
        }

        public boolean isResourceLocationMessage() {
            return value instanceof legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
        }

        public boolean isFunctionMessage() {
            return value instanceof java.util.function.Function<?, ?> || functionClass() != null;
        }

        /** A function message names its class (the mod instantiates it on the receiving side). */
        private Class<?> functionClass() {
            if (!(value instanceof String name) || !key.toLowerCase(java.util.Locale.ROOT).contains("function") && !name.contains(".")) return null;
            try {
                Class<?> c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
                return java.util.function.Function.class.isAssignableFrom(c) ? c : null;
            } catch (ClassNotFoundException | LinkageError e) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public <T, V> java.util.Optional<java.util.function.Function<T, V>> getFunctionValue(Class<T> inType, Class<V> outType) {
            if (value instanceof java.util.function.Function<?, ?> f) return java.util.Optional.of((java.util.function.Function<T, V>) f);
            Class<?> c = functionClass();
            if (c != null) {
                try {
                    var ctor = c.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    return java.util.Optional.of((java.util.function.Function<T, V>) ctor.newInstance());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // absent
                }
            }
            return java.util.Optional.empty();
        }

        public Class<?> getMessageType() {
            return value.getClass();
        }

        public boolean isStringMessage() {
            return value instanceof String;
        }

        public boolean isNBTMessage() {
            return value instanceof NBTTagCompound;
        }

        public boolean isItemStackMessage() {
            return value instanceof ItemStack;
        }
    }
}
