package org.kylepitcock.legacyconnector;

import org.kylepitcock.legacyconnector.core.flags.Flags;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * Shim classes must load on a dedicated server: legacy mods construct renderers and GUIs there, and NeoForge's RuntimeDistCleaner refuses
 * client-only classes ({@code net.minecraft.client}, {@code @OnlyIn(Dist.CLIENT)}). The bytecode verifier loads such a class when it has to check
 * that it is assignable to another type (an argument, a field store, a return, a frame merge): {@code RendererLivingEntity.canRenderName} passed a
 * {@code LocalPlayer} where a {@code Player} was expected and every legacy renderer failed on servers. Client code belongs in nested holder classes.
 *
 * <p>The compiled shim classes of every legacy version are run through ASM's verifier, whose assignability test is replaced by one that works
 * on class files (nothing is loaded) and notes each check that would force a client-only class to load. The hits that exist are frozen in
 * {@code tools/dev/shim-client-baseline.txt} (class, tab, client type); the list may only shrink: a new hit fails, and so does a baseline line that
 * no longer occurs (remove it; {@code -Dshim.client.update=true} (Gradle: see the test task) rewrites the file with the current, smaller set).</p>
 */
class ShimClientTypesTest {
    private static final String[] ROOTS = {"legacyconnector/v1_7_10/", "legacyconnector/v1_6_4/", "legacyconnector/v1_12_2/", "legacyconnector/vb1_7_3/"};
    private static final String[] CLIENT_PACKAGES = {"net/minecraft/client/", "com/mojang/blaze3d/", "net/neoforged/neoforge/client/"};
    private static final Path BASELINE = Path.of("tools/dev/shim-client-baseline.txt");

    private final Map<String, ClassNode> classes = new HashMap<>();
    private final Map<String, Boolean> clientOnly = new HashMap<>();

    private ClassNode node(String name) {
        return classes.computeIfAbsent(name, n -> {
            try (InputStream in = ShimClientTypesTest.class.getClassLoader().getResourceAsStream(n + ".class")) {
                if (in == null) return new ClassNode(9 << 16); // not visible: treated as an unknown plain class
                ClassNode c = new ClassNode(9 << 16);
                new ClassReader(in).accept(c, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                return c;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private boolean known(String name) {
        return node(name).name != null;
    }

    private boolean isClientOnly(String name) {
        Boolean cached = clientOnly.get(name);
        if (cached != null) return cached;
        boolean result = false;
        // shim classes count by their own @OnlyIn(CLIENT) only (client holder classes); modern classes also by package
        if (!name.startsWith("legacyconnector/")) for (String p : CLIENT_PACKAGES) if (name.startsWith(p)) result = true;
        {
            if (!result) {
                ClassNode c = node(name);
                List<AnnotationNode> anns = new ArrayList<>();
                if (c.visibleAnnotations != null) anns.addAll(c.visibleAnnotations);
                if (c.invisibleAnnotations != null) anns.addAll(c.invisibleAnnotations);
                for (AnnotationNode a : anns) {
                    if (!"Lnet/neoforged/api/distmarker/OnlyIn;".equals(a.desc) || a.values == null) continue;
                    for (int i = 0; i + 1 < a.values.size(); i += 2) {
                        Object v = a.values.get(i + 1);
                        if (v instanceof String[] e && e.length == 2 && e[1].equals("CLIENT")) result = true;
                    }
                }
            }
        }
        clientOnly.put(name, result);
        return result;
    }

    private boolean isInterface(String name) {
        return (node(name).access & Opcodes.ACC_INTERFACE) != 0;
    }

    private boolean subclassOf(String sub, String sup) {
        for (int guard = 0; sub != null && guard < 64; guard++) {
            if (sub.equals(sup)) return true;
            ClassNode c = node(sub);
            if (c.name == null) return true; // unknown: give the benefit of the doubt, the real check is the baseline of what we can see
            if (isInterface(sup)) for (String i : c.interfaces) if (subclassOf(i, sup)) return true;
            sub = c.superName;
        }
        return false;
    }

    /** The verifier with an assignability test that reads class files and notes the checks that would load a client-only class. */
    private final class Noting extends SimpleVerifier {
        final Set<String> hits = new TreeSet<>();
        String method = "";

        Noting(Type current, Type sup, List<Type> itfs, boolean isItf) {
            super(9 << 16,  current, sup, itfs, isItf);
        }

        @Override
        protected boolean isInterface(Type t) {
            return t.getSort() == Type.OBJECT && ShimClientTypesTest.this.isInterface(t.getInternalName());
        }

        @Override
        protected Type getSuperClass(Type t) {
            if (t.getSort() != Type.OBJECT) return Type.getObjectType("java/lang/Object");
            String s = node(t.getInternalName()).superName;
            return s == null ? null : Type.getObjectType(s);
        }

        @Override
        protected boolean isAssignableFrom(Type target, Type source) {
            if (target.equals(source)) return true;
            if (target.getSort() != Type.OBJECT || source.getSort() != Type.OBJECT) {
                return target.getSort() == Type.OBJECT && target.getInternalName().equals("java/lang/Object") || target.equals(source) || source.getSort() == Type.ARRAY;
            }
            String t = target.getInternalName(), s = source.getInternalName();
            if (t.equals("java/lang/Object")) return true;
            // HotSpot loads the target to learn whether it is an interface, and the source only when the target is a class
            boolean targetItf = known(t) && ShimClientTypesTest.this.isInterface(t);
            if (isClientOnly(t) || (!targetItf && isClientOnly(s))) {
                hits.add((isClientOnly(t) ? t : s));
            }
            return targetItf || subclassOf(s, t);
        }
    }

    @Test
    void shimClassesDoNotNeedClientOnlyClassesToVerify() throws Exception {
        assertNotNull(ShimClientTypesTest.class.getClassLoader().getResource("net/minecraft/client/Minecraft.class"),
                "the game classes must be on the test class path, or this check sees nothing");
        Path main = Path.of(legacyconnector.v1_7_10.net.minecraft.block.Block.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Set<String> found = new TreeSet<>();
        int analysed = 0, failed = 0;
        for (String root : ROOTS) {
            Path dir = main.resolve(root);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files;
            try (Stream<Path> s = Files.walk(dir)) {
                files = s.filter(p -> p.toString().endsWith(".class")).sorted().toList();
            }
            for (Path file : files) {
                ClassNode c = new ClassNode(9 << 16);
                new ClassReader(Files.readAllBytes(file)).accept(c, ClassReader.SKIP_DEBUG);
                if (isClientOnly(c.name)) continue; // a shim class that is itself client-only is only reached from client code
                classes.put(c.name, c);
                for (MethodNode m : c.methods) {
                    if (m.instructions.size() == 0) continue;
                    Noting v = new Noting(Type.getObjectType(c.name), c.superName == null ? null : Type.getObjectType(c.superName),
                            c.interfaces.stream().map(Type::getObjectType).toList(), (c.access & Opcodes.ACC_INTERFACE) != 0);
                    try {
                        new Analyzer<BasicValue>(v).analyze(c.name, m);
                        analysed++;
                    } catch (Exception | LinkageError e) {
                        failed++;
                    }
                    for (String t : v.hits) found.add(c.name + "\t" + t);
                }
            }
        }
        Set<String> baseline = new TreeSet<>();
        if (Files.exists(BASELINE)) {
            for (String line : Files.readAllLines(BASELINE, StandardCharsets.UTF_8)) {
                if (!line.isBlank() && !line.startsWith("#")) baseline.add(line.strip());
            }
        }
        Set<String> added = new TreeSet<>(found);
        added.removeAll(baseline);
        Set<String> gone = new TreeSet<>(baseline);
        gone.removeAll(found);
        String update = Flags.SHIM_CLIENT_UPDATE.get(); // "true": only when nothing is new; "force": also adds (first run)
        if (update != null && (added.isEmpty() || update.equals("force"))) {
            List<String> out = new ArrayList<>();
            out.add("# Shim classes whose bytecode verification needs a client-only class (see ShimClientTypesTest): class<TAB>client type. Only ever shrinks.");
            out.addAll(found);
            Files.write(BASELINE, out, StandardCharsets.UTF_8);
            return;
        }
        StringBuilder msg = new StringBuilder();
        if (!added.isEmpty()) {
            msg.append("New shim classes would load a client-only class while verifying (a dedicated server refuses that; move the client code into a nested holder class, ")
                    .append("or pass/return the common type):\n");
            for (String a : added) msg.append("  ").append(a.replace("\t", "  ->  ")).append('\n');
        }
        if (!gone.isEmpty()) {
            msg.append("Fixed since the baseline (remove from tools/dev/shim-client-baseline.txt, or run with -Dshim.client.update=true):\n");
            for (String a : gone) msg.append("  ").append(a.replace("\t", "  ->  ")).append('\n');
        }
        if (msg.length() > 0) fail(msg + "(" + analysed + " methods analysed, " + failed + " skipped)");
    }
}
