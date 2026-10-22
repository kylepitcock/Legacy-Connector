package legacyconnector.v1_7_10.cpw.mods.fml.common;

import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.ReflectionHelper;

/** Reflection on legacy (shim) classes. SRG names in the arguments were already renamed by the loader. */
public class ObfuscationReflectionHelper {
    public static <T, E> T getPrivateValue(Class<? super E> cls, E instance, int fieldIndex) {
        return ReflectionHelper.getPrivateValue(cls, instance, fieldIndex);
    }

    public static <T, E> T getPrivateValue(Class<? super E> cls, E instance, String... names) {
        return ReflectionHelper.getPrivateValue(cls, instance, names);
    }

    public static <T, E> void setPrivateValue(Class<? super T> cls, T instance, E value, int fieldIndex) {
        ReflectionHelper.setPrivateValue(cls, instance, value, fieldIndex);
    }

    public static <T, E> void setPrivateValue(Class<? super T> cls, T instance, E value, String... names) {
        ReflectionHelper.setPrivateValue(cls, instance, value, names);
    }

    public static String[] remapFieldNames(String className, String... names) {
        return names;
    }
}
