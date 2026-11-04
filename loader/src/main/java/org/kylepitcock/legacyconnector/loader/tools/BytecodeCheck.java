package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.transform.Hierarchy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * Offline bytecode verification of a translated jar: runs ASM's data-flow analyzer over every method, with type relations answered by
 * the layer's {@link Hierarchy} (translated mod, shim, synthesized API, libraries). It finds what the JVM would reject as a
 * {@code VerifyError} when the class first loads (a retyped value used as the wrong type, a popped slot of the wrong kind), which a
 * symbol-existence link check cannot see. Methods that touch a type nobody knows (a mod that is not installed) are skipped.
 *
 * <p>Approach after PortKit's {@code BytecodeVerifier} (MIT; https://github.com/portkit), reimplemented against this project's hierarchy.
 */
public final class BytecodeCheck {
    /** Failing methods (class.method+desc: reason), at most {@link #MAX_LISTED}. */
    public final List<String> failures = new ArrayList<>();
    public int failureCount;
    public int methodsChecked;
    private static final int MAX_LISTED = 25;

    private final Hierarchy hierarchy;

    public BytecodeCheck(Hierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    /** Reads every class of the translated jar into the hierarchy first (mods extend their own classes), then verifies them. */
    public static BytecodeCheck run(Path translatedJar, Hierarchy hierarchy) throws IOException {
        List<ClassNode> nodes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(translatedJar.toFile())) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in.readAllBytes()).accept(node, ClassReader.SKIP_FRAMES);
                    nodes.add(node);
                } catch (RuntimeException e) {
                    // unreadable class: the loader reports it elsewhere
                }
            }
        }
        for (ClassNode n : nodes) {
            ClassInfo info = new ClassInfo(n.name, n.superName, n.interfaces == null ? List.of() : n.interfaces, n.access);
            for (MethodNode m : n.methods) info.methods.put(m.name + m.desc, m.access);
            hierarchy.addLocal(info);
        }
        BytecodeCheck check = new BytecodeCheck(hierarchy);
        for (ClassNode n : nodes) check.check(n);
        return check;
    }

    private void check(ClassNode cls) {
        for (MethodNode m : cls.methods) {
            if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || m.instructions.size() == 0) continue;
            methodsChecked++;
            try {
                new Analyzer<BasicValue>(new Verifier(cls)).analyze(cls.name, m);
            } catch (AnalyzerException e) {
                if (unknown(e)) continue;
                failureCount++;
                if (failures.size() < MAX_LISTED) {
                    String why = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
                    failures.add(cls.name + "." + m.name + m.desc + ": " + why);
                }
            } catch (RuntimeException e) {
                // analyzer limits (huge methods): not a defect of the translation
            }
        }
    }

    private static boolean unknown(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) if (t instanceof UnknownType) return true;
        return false;
    }

    private static final class UnknownType extends RuntimeException {
        UnknownType(String name) {
            super(name, null, false, false);
        }
    }

    private final class Verifier extends SimpleVerifier {
        Verifier(ClassNode current) {
            super(Opcodes.ASM9, Type.getObjectType(current.name), current.superName == null ? null : Type.getObjectType(current.superName),
                    current.interfaces.stream().map(Type::getObjectType).toList(), (current.access & Opcodes.ACC_INTERFACE) != 0);
        }

        private ClassInfo info(Type type) {
            ClassInfo c = hierarchy.find(type.getInternalName());
            if (c == null) throw new UnknownType(type.getInternalName());
            return c;
        }

        @Override
        protected boolean isInterface(Type type) {
            return (info(type).access & Opcodes.ACC_INTERFACE) != 0;
        }

        @Override
        protected Type getSuperClass(Type type) {
            String sup = info(type).superName;
            return sup == null ? null : Type.getObjectType(sup);
        }

        @Override
        protected boolean isAssignableFrom(Type type, Type other) {
            if (type.equals(other)) return true;
            if (type.getSort() == Type.ARRAY) {
                if (other.getSort() != Type.ARRAY) return false;
                Type a = Type.getType(type.getDescriptor().substring(1));
                Type b = Type.getType(other.getDescriptor().substring(1));
                boolean refs = (a.getSort() == Type.OBJECT || a.getSort() == Type.ARRAY) && (b.getSort() == Type.OBJECT || b.getSort() == Type.ARRAY);
                return refs ? isAssignableFrom(a, b) : a.equals(b);
            }
            if (type.getInternalName().equals("java/lang/Object")) return true;
            if (other.getSort() == Type.ARRAY) return isInterface(type); // arrays implement Cloneable/Serializable
            if (isInterface(type)) return true; // the JVM verifier treats interface-typed values as Object
            for (String c = other.getInternalName(); c != null; c = info(Type.getObjectType(c)).superName) {
                if (c.equals(type.getInternalName())) return true;
            }
            return false;
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new UnknownType(type.getInternalName());
        }
    }
}
