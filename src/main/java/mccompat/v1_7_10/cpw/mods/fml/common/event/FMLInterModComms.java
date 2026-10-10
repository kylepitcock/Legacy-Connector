package mccompat.v1_7_10.cpw.mods.fml.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.cpw.mods.fml.common.Loader;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710;

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
        var active = LegacyModContainer1710.active();
        message.sender = active == null ? "unknown" : active.getModId();
        QUEUE.computeIfAbsent(modId.toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>()).add(message);
        return true;
    }

    public static List<IMCMessage> fetchRuntimeMessages(Object forMod) {
        var c = mccompat.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().findContainerFor(forMod);
        if (c == null) return new ArrayList<>();
        List<IMCMessage> list = QUEUE.remove(c.getModId().toLowerCase(java.util.Locale.ROOT));
        return list == null ? new ArrayList<>() : list;
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
