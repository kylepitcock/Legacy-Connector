package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls of a 1.20.1 mod to a method that 1.21.1 made protected, on an object that is not the caller's own class ({@code state.getBlock().onRemove(...)}): the
 * loader sends them here (see {@code ProtectedCalls1201}). The method is the one the call named, found on the declaring Minecraft class, and virtual dispatch
 * is kept: an override in the receiver's class runs.
 */
public final class ProtectedAccess1201 {
    private ProtectedAccess1201() {}

    private static final Map<String, MethodHandle> HANDLES = new ConcurrentHashMap<>();

    public static Object invoke(Object receiver, Class<?> declaring, String name, String desc, Object[] args) throws Throwable {
        MethodHandle h = HANDLES.computeIfAbsent(declaring.getName() + "#" + name + desc, k -> find(declaring, name, desc));
        if (receiver == null) throw new NullPointerException("Cannot invoke \"" + declaring.getName() + "." + name + "\" on a null reference");
        Object[] all = new Object[args.length + 1];
        all[0] = receiver;
        System.arraycopy(args, 0, all, 1, args.length);
        return h.invokeWithArguments(all);
    }

    private static MethodHandle find(Class<?> declaring, String name, String desc) {
        for (Method m : declaring.getDeclaredMethods()) {
            if (!m.getName().equals(name) || !descriptor(m).equals(desc)) continue;
            try {
                m.setAccessible(true);
                return MethodHandles.lookup().unreflect(m).asType(MethodType.methodType(m.getReturnType(), declaring, m.getParameterTypes()).generic());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot reach " + declaring.getName() + "." + name + desc, e);
            }
        }
        throw new NoSuchMethodError(declaring.getName() + "." + name + desc);
    }

    private static String descriptor(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) sb.append(p.descriptorString());
        return sb.append(')').append(m.getReturnType().descriptorString()).toString();
    }
}
