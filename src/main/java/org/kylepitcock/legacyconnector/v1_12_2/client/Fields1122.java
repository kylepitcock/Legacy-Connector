package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes private fields of the game's client classes that 1.12.2 code had as fields of its own (the HUD's overlay message and
 * health animation, the chat's lines and scroll position). Lookups are cached; a field that is missing reads as null and ignores writes.
 */
public final class Fields1122 {
    private static final Map<String, Field> CACHE = new ConcurrentHashMap<>();
    private static final Field MISSING;

    static {
        try {
            MISSING = Fields1122.class.getDeclaredField("CACHE");
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Fields1122() {}

    private static Field field(Class<?> owner, String name) {
        Field f = CACHE.computeIfAbsent(owner.getName() + "." + name, k -> {
            try {
                Field found = owner.getDeclaredField(name);
                found.setAccessible(true);
                return found;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return MISSING;
            }
        });
        return f == MISSING ? null : f;
    }

    public static Object get(Class<?> owner, Object target, String name) {
        Field f = field(owner, name);
        try {
            return f == null || target == null ? null : f.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static int getInt(Class<?> owner, Object target, String name) {
        return get(owner, target, name) instanceof Number n ? n.intValue() : 0;
    }

    public static long getLong(Class<?> owner, Object target, String name) {
        return get(owner, target, name) instanceof Number n ? n.longValue() : 0L;
    }

    public static boolean getBoolean(Class<?> owner, Object target, String name) {
        return get(owner, target, name) instanceof Boolean b && b;
    }

    public static void set(Class<?> owner, Object target, String name, Object value) {
        Field f = field(owner, name);
        if (f == null || target == null) return;
        try {
            f.set(target, value);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            // a value of the wrong kind is ignored
        }
    }

    /** A modern chat component as a 1.12.2 one (through their shared JSON form). */
    public static legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent legacy(net.minecraft.network.chat.Component component) {
        if (component == null) return null;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.core.HolderLookup.Provider lookup = mc.level != null ? mc.level.registryAccess()
                    : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
            return legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(
                    net.minecraft.network.chat.Component.Serializer.toJson(component, lookup));
        } catch (RuntimeException e) {
            return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(component.getString());
        }
    }

    /** A line of the chat as a component (the modern chat keeps wrapped lines as styled character sequences). */
    public static net.minecraft.network.chat.Component component(net.minecraft.util.FormattedCharSequence seq) {
        var out = net.minecraft.network.chat.Component.empty();
        StringBuilder run = new StringBuilder();
        net.minecraft.network.chat.Style[] style = {null};
        seq.accept((index, s, codePoint) -> {
            if (style[0] != null && !s.equals(style[0])) {
                out.append(net.minecraft.network.chat.Component.literal(run.toString()).withStyle(style[0]));
                run.setLength(0);
            }
            style[0] = s;
            run.appendCodePoint(codePoint);
            return true;
        });
        if (run.length() > 0) out.append(net.minecraft.network.chat.Component.literal(run.toString()).withStyle(style[0]));
        return out;
    }
}
