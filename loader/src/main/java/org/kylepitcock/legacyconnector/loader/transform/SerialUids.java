package org.kylepitcock.legacyconnector.loader.transform;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

/**
 * A serializable class that declares no {@code serialVersionUID} gets one computed from its structure (name, members, interfaces). The
 * translation renames and adds members, so the computed value changes and data the mod serialized itself (saved schematics, caches) no longer
 * reads: {@code InvalidClassException: local class incompatible: stream classdesc serialVersionUID = ..., local class serialVersionUID = ...}.
 * The value is computed from the ORIGINAL class, before anything is rewritten, and declared in the translated class.
 */
public final class SerialUids {
    private SerialUids() {}

    /** Internal names of the classes of the jar that implement {@link Serializable}, directly or through a superclass or interface. */
    public static Set<String> serializable(Map<String, byte[]> classes) {
        Map<String, String> supers = new HashMap<>();
        Map<String, String[]> interfaces = new HashMap<>();
        Set<String> plain = new HashSet<>();
        for (var e : classes.entrySet()) {
            if (!e.getKey().endsWith(".class") || e.getKey().startsWith("META-INF/")) continue;
            try {
                ClassReader r = new ClassReader(e.getValue());
                supers.put(r.getClassName(), r.getSuperName());
                interfaces.put(r.getClassName(), r.getInterfaces());
                if ((r.getAccess() & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM | Opcodes.ACC_ANNOTATION)) == 0) plain.add(r.getClassName());
            } catch (RuntimeException ex) {
                // unreadable classes are reported by the transformer itself
            }
        }
        Map<String, Boolean> memo = new HashMap<>();
        Set<String> out = new HashSet<>();
        for (String name : plain) if (isSerializable(name, supers, interfaces, memo, 0)) out.add(name);
        return out;
    }

    private static boolean isSerializable(String name, Map<String, String> supers, Map<String, String[]> interfaces, Map<String, Boolean> memo, int depth) {
        if (name == null || depth > 64) return false;
        if (name.equals("java/io/Serializable")) return true;
        Boolean known = memo.get(name);
        if (known != null) return known;
        boolean result = false;
        if (interfaces.containsKey(name)) {
            memo.put(name, false); // cycle guard
            for (String i : interfaces.get(name)) result |= isSerializable(i, supers, interfaces, memo, depth + 1);
            result |= isSerializable(supers.get(name), supers, interfaces, memo, depth + 1);
        } else if (name.startsWith("java/") || name.startsWith("javax/")) {
            try {
                result = Serializable.class.isAssignableFrom(Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                result = false;
            }
        }
        memo.put(name, result);
        return result;
    }

    /** The visitor to read the ORIGINAL class bytes with: adds the computed serialVersionUID when the class is in {@code serializable}. */
    public static ClassVisitor head(Set<String> serializable, byte[] original, ClassVisitor next) {
        try {
            if (serializable.contains(new ClassReader(original).getClassName())) return new SerialVersionUIDAdder(Opcodes.ASM9, next) {}; // the api constructor is protected
        } catch (RuntimeException e) {
            // fall through: the class is read (and reported) as before
        }
        return next;
    }
}
