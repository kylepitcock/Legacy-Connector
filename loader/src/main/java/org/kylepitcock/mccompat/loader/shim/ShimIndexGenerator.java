package org.kylepitcock.mccompat.loader.shim;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Build-time tool: {@code ShimIndexGenerator <output file> <classes dir>...}.
 * Indexes every class below {@code mccompat/} (the relocated legacy API surface).
 */
public final class ShimIndexGenerator {
    public static final String SHIM_ROOT = "mccompat/";

    private ShimIndexGenerator() {}

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args[0]);
        Map<String, ClassInfo> classes = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            Path root = Path.of(args[i]);
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : (Iterable<Path>) walk::iterator) {
                    String rel = root.relativize(p).toString().replace('\\', '/');
                    if (!rel.startsWith(SHIM_ROOT) || !rel.endsWith(".class")) continue;
                    try (InputStream in = Files.newInputStream(p)) {
                        ClassInfo info = read(new ClassReader(in));
                        classes.put(info.name, info);
                    }
                }
            }
        }
        List<String> violations = checkPhantomFieldAccess(classes, args);
        if (!violations.isEmpty()) {
            violations.forEach(v -> System.err.println("phantom field access: " + v));
            throw new IllegalStateException(violations.size() + " shim method(s) access phantom legacy fields directly; use the mcc$ accessors");
        }
        Files.createDirectories(output.getParent());
        try (Writer w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            new ShimIndex(classes).write(w);
        }
        System.out.println("Indexed " + classes.size() + " shim classes -> " + output);
    }

    /**
     * Phantom fields are legacy fields the loader redirects to {@code mcc$get$/mcc$set$} accessors in mod code. Shim
     * code is compiled normally, so a direct access there would read a dead field: reject it at build time.
     */
    private static List<String> checkPhantomFieldAccess(Map<String, ClassInfo> classes, String[] args) throws IOException {
        List<String> out = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path root = Path.of(args[i]);
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : (Iterable<Path>) walk::iterator) {
                    if (!p.toString().endsWith(".class")) continue;
                    ClassReader reader;
                    try (InputStream in = Files.newInputStream(p)) {
                        reader = new ClassReader(in);
                    }
                    String self = reader.getClassName();
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String mname, String mdesc, String sig, String[] ex) {
                            // the accessors themselves may obviously not be checked against themselves
                            if (mname.startsWith(ShimIndex.GETTER_PREFIX) || mname.startsWith(ShimIndex.SETTER_PREFIX)) return null;
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitFieldInsn(int opcode, String owner, String fname, String fdesc) {
                                    for (ClassInfo c = classes.get(owner); c != null; c = classes.get(c.superName)) {
                                        if (c.methods.keySet().stream().anyMatch(k -> k.startsWith(ShimIndex.GETTER_PREFIX + fname + "("))) {
                                            out.add(self + "." + mname + " -> " + owner + "." + fname);
                                            return;
                                        }
                                        if (c.hasField(fname, fdesc) && !c.name.equals(owner)) break;
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        return out;
    }

    public static ClassInfo read(ClassReader reader) {
        ClassInfo[] holder = new ClassInfo[1];
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                holder[0] = new ClassInfo(name, superName, interfaces == null ? List.of() : new ArrayList<>(List.of(interfaces)), access);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if ((access & Opcodes.ACC_SYNTHETIC) == 0) holder[0].fields.put(name + ":" + descriptor, access);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                // Bridge methods are kept: legacy code compiled against erased signatures links to them.
                holder[0].methods.put(name + descriptor, access);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return holder[0];
    }
}
