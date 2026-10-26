package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.ReflectionHelper;

/** The older name of the reflection helper (members given by SRG or MCP name). */
public class ObfuscationReflectionHelper {
    public static boolean isObfuscated() {
        return false;
    }

    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, int fieldIndex) {
        return ReflectionHelper.getPrivateValue(classToAccess, instance, fieldIndex);
    }

    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String... fieldNames) {
        return ReflectionHelper.getPrivateValue(classToAccess, instance, fieldNames);
    }

    /** The 1.12.2 (build 2768+) form: one SRG field name. */
    public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String fieldName) {
        return ReflectionHelper.getPrivateValue(classToAccess, instance, new String[] {fieldName});
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String fieldName) {
        ReflectionHelper.setPrivateValue(classToAccess, instance, value, new String[] {fieldName});
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String... fieldNames) {
        ReflectionHelper.setPrivateValue(classToAccess, instance, value, fieldNames);
    }

    public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, int fieldIndex) {
        ReflectionHelper.setPrivateValue(classToAccess, instance, value, fieldIndex);
    }

    public static String[] remapFieldNames(String className, String... fieldNames) {
        return fieldNames;
    }

    public static String remapMethodName(String className, String methodName, Class<?>... paramTypes) {
        return methodName;
    }

    public static Field findField(Class<?> clazz, String... fieldNames) {
        return ReflectionHelper.findField(clazz, fieldNames);
    }

    public static <E> Method findMethod(Class<? super E> clazz, E instance, String[] methodNames, Class<?>... methodTypes) {
        return ReflectionHelper.findMethod(clazz, instance, methodNames, methodTypes);
    }

    /** The 1.12.2 (build 2768+) form: SRG name, return type and parameter types. */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... parameterTypes) {
        return ReflectionHelper.findMethod((Class<Object>) clazz, null, new String[] {methodName}, parameterTypes);
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        return ReflectionHelper.findField(clazz, fieldName);
    }
}
