package mccompat.v1_7_10.net.minecraftforge.common.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import mccompat.v1_7_10.net.minecraft.item.EnumRarity;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemArmor;
import mccompat.v1_7_10.net.minecraft.util.EnumChatFormatting;
import org.kylepitcock.mccompat.compat.jdk.ReflectionCompat;
import org.slf4j.LoggerFactory;

/**
 * Forge's runtime enum extension. Works for any enum (shim, synthesized or the mod's own): the constant is built
 * through a method handle (reflection refuses to construct enums) and appended to {@code $VALUES}.
 */
public class EnumHelper {
    private EnumHelper() {}

    public static Item.ToolMaterial addToolMaterial(String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability) {
        return addEnum(Item.ToolMaterial.class, name, new Class<?>[] {int.class, int.class, float.class, float.class, int.class},
                new Object[] {harvestLevel, maxUses, efficiency, damage, enchantability});
    }

    public static ItemArmor.ArmorMaterial addArmorMaterial(String name, int durability, int[] reductionAmounts, int enchantability) {
        return addEnum(ItemArmor.ArmorMaterial.class, name, new Class<?>[] {int.class, int[].class, int.class},
                new Object[] {durability, reductionAmounts, enchantability});
    }

    public static EnumRarity addRarity(String name, EnumChatFormatting color, String displayName) {
        return addEnum(EnumRarity.class, name, new Object[] {color, displayName});
    }

    /** Parameter types inferred from the values (boxed types matched against primitive constructor parameters). */
    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Object... paramValues) {
        for (Constructor<?> c : enumType.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length != paramValues.length + 2) continue;
            Class<?>[] user = java.util.Arrays.copyOfRange(p, 2, p.length);
            if (matches(user, paramValues)) return addEnum(enumType, enumName, user, paramValues);
        }
        LoggerFactory.getLogger("MCCompat/EnumHelper").warn("No constructor of {} matches {} for {}", enumType.getName(),
                java.util.Arrays.toString(paramValues), enumName);
        return null;
    }

    public static <T extends Enum<?>> T addEnum(Class<?>[][] map, Class<T> enumType, String enumName, Object... paramValues) {
        for (Class<?>[] lookup : map) {
            if (lookup[0] == enumType) return addEnum(enumType, enumName, java.util.Arrays.copyOfRange(lookup, 1, lookup.length), paramValues);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Class<?>[] paramTypes, Object[] paramValues) {
        try {
            Field valuesField = valuesField(enumType);
            T[] previous = (T[]) valuesField.get(null);
            for (T existing : previous) if (existing.name().equals(enumName)) return existing;

            Class<?>[] all = new Class<?>[paramTypes.length + 2];
            all[0] = String.class;
            all[1] = int.class;
            System.arraycopy(paramTypes, 0, all, 2, paramTypes.length);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(enumType, MethodHandles.lookup());
            MethodHandle ctor = lookup.findConstructor(enumType, MethodType.methodType(void.class, all));
            Object[] args = new Object[all.length];
            args[0] = enumName;
            args[1] = previous.length;
            System.arraycopy(paramValues, 0, args, 2, paramValues.length);
            T value = (T) ctor.invokeWithArguments(args);

            T[] values = (T[]) Array.newInstance(enumType, previous.length + 1);
            System.arraycopy(previous, 0, values, 0, previous.length);
            values[previous.length] = value;
            ReflectionCompat.set(valuesField, null, values);
            resetEnumCaches(enumType);
            return value;
        } catch (Throwable t) {
            LoggerFactory.getLogger("MCCompat/EnumHelper").error("Could not add {} to {}", enumName, enumType.getName(), t);
            return null;
        }
    }

    private static Field valuesField(Class<?> enumType) throws NoSuchFieldException {
        for (Field f : enumType.getDeclaredFields()) {
            if (f.isSynthetic() && Modifier.isStatic(f.getModifiers()) && f.getType().isArray() && f.getType().getComponentType() == enumType) {
                f.setAccessible(true);
                return f;
            }
        }
        Field f = enumType.getDeclaredField("$VALUES");
        f.setAccessible(true);
        return f;
    }

    /** Class caches values() results; drop them so valueOf/EnumMap see the new constant. */
    private static void resetEnumCaches(Class<?> enumType) {
        for (String name : new String[] {"enumConstants", "enumConstantDirectory"}) {
            try {
                Field f = Class.class.getDeclaredField(name);
                ReflectionCompat.set(f, enumType, null);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // best effort; values() itself reads $VALUES
            }
        }
    }

    private static boolean matches(Class<?>[] types, Object[] values) {
        for (int i = 0; i < types.length; i++) {
            Object v = values[i];
            Class<?> t = types[i].isPrimitive() ? box(types[i]) : types[i];
            if (v != null && !t.isInstance(v)) return false;
            if (v == null && types[i].isPrimitive()) return false;
        }
        return true;
    }

    private static Class<?> box(Class<?> p) {
        return MethodType.methodType(p).wrap().returnType();
    }
}
