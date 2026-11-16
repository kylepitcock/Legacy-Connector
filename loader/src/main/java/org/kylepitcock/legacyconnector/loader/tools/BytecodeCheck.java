package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.transform.Hierarchy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

/**
 * Offline bytecode verification of translated classes: runs ASM's data-flow analyzer over every method, with type relations answered by
 * the layer's {@link Hierarchy} (translated mod and the other jars of the set, shim, synthesized API, and the classpath the check runs on:
 * JDK, libraries, Minecraft/NeoForge, the layer's runtime). It finds what the JVM would reject as a {@code VerifyError} when the class
 * first loads (a retyped value used as the wrong type, a popped slot of the wrong kind), which a symbol-existence link check cannot see.
 * Client-only classes are checked like any other: the hierarchy is read from class files, nothing is loaded or initialised.
 *
 * <p>Unresolvable types are not errors (the link check owns missing classes): an unknown type is treated as assignable and counted in
 * {@link #unknownTypes}; a method whose merge of control-flow paths needs an unknown class is skipped ({@link #methodsSkipped}).
 * Findings are grouped by (expected type, found type): one group with a site count and a few example sites.
 *
 * <p>Approach after PortKit's {@code BytecodeVerifier} (MIT; https://github.com/portkit), reimplemented against this project's hierarchy.
 */
public final class BytecodeCheck {
    /** One failing instruction. {@code insn} is the instruction index in the method (not the byte offset). */
    public record Finding(String cls, String method, int insn, int line, String instruction, String expected, String found, String message) {
        public String site() {
            return cls + "." + method + " insn #" + insn + (line > 0 ? " (line " + line + ")" : "") + " " + instruction;
        }
    }

    /** Findings with the same expected/found pair (or the same message when the analyzer gave no types). */
    public static final class Group {
        public final String expected;
        public final String found;
        public final String message;
        public int sites;
        public final List<Finding> examples = new ArrayList<>();

        Group(String expected, String found, String message) {
            this.expected = expected;
            this.found = found;
            this.message = message;
        }

        public String describe() {
            return expected.isEmpty() ? message : found + " is not assignable to " + expected;
        }
    }

    /** Failing methods (class.method+desc: reason), at most {@link #MAX_LISTED}. */
    public final List<String> failures = new ArrayList<>();
    /** Methods with a finding (the analyzer stops at the first error of a method, so also the number of sites). */
    public int failureCount;
    public int methodsChecked;
    public int classesChecked;
    /** Methods not analysed because a merge needed an unknown class, or the analyzer hit its limits. */
    public int methodsSkipped;
    public int findingCount;
    /** Names of types that could not be resolved (treated as assignable). */
    public final Set<String> unknownTypes = new TreeSet<>();
    public final Map<String, Group> groups = new LinkedHashMap<>();
    private static final int MAX_LISTED = 25;
    private static final int MAX_EXAMPLES = 3;
    private static final Pattern EXPECTED_FOUND = Pattern.compile("[Ee]xpected (.+?), but found (.+)$");

    private final Hierarchy hierarchy;

    public BytecodeCheck(Hierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    /** Reads every class of the translated jar into the hierarchy first (mods extend their own classes), then verifies them. */
    public static BytecodeCheck run(Path translatedJar, Hierarchy hierarchy) throws IOException {
        return runClasses(readJar(translatedJar), hierarchy);
    }

    private static List<byte[]> readJar(Path jar) throws IOException {
        List<byte[]> classes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (var en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    classes.add(in.readAllBytes());
                }
            }
        }
        return classes;
    }

    /** Verifies class files given as bytes (the unit of the tests and of {@link #run}). */
    public static BytecodeCheck runClasses(Collection<byte[]> classFiles, Hierarchy hierarchy) {
        List<ClassNode> nodes = new ArrayList<>();
        for (byte[] bytes : classFiles) {
            try {
                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
                nodes.add(node);
            } catch (RuntimeException e) {
                // unreadable class: the loader reports it elsewhere
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

    /** {@code verify: N classes, M methods, K findings in J groups} (plus the unknown/skipped counts when there are any). */
    public String summary() {
        return "verify: " + classesChecked + " classes, " + methodsChecked + " methods, " + findingCount + " findings in " + groups.size() + " groups"
                + (unknownTypes.isEmpty() && methodsSkipped == 0 ? "" : " (" + unknownTypes.size() + " unknown types treated as assignable, "
                + methodsSkipped + " methods skipped)");
    }

    /** The record's {@code verify} object: counts, then one entry per group with its site count and examples. */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("classes", classesChecked);
        o.addProperty("methods", methodsChecked);
        o.addProperty("findings", findingCount);
        o.addProperty("groupCount", groups.size());
        o.addProperty("unknownTypes", unknownTypes.size());
        o.addProperty("methodsSkipped", methodsSkipped);
        JsonArray list = new JsonArray();
        for (Group g : groups.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("problem", g.describe());
            if (!g.expected.isEmpty()) {
                j.addProperty("expected", g.expected);
                j.addProperty("found", g.found);
            }
            j.addProperty("sites", g.sites);
            JsonArray ex = new JsonArray();
            g.examples.forEach(f -> ex.add(f.site()));
            j.add("examples", ex);
            list.add(j);
        }
        o.add("groups", list);
        return o;
    }

    private void check(ClassNode cls) {
        classesChecked++;
        for (MethodNode m : cls.methods) {
            if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || m.instructions.size() == 0) continue;
            methodsChecked++;
            try {
                new Analyzer<BasicValue>(new Verifier(cls)).analyze(cls.name, m);
            } catch (AnalyzerException e) {
                if (unknown(e)) {
                    methodsSkipped++;
                    continue;
                }
                record(cls, m, e);
            } catch (RuntimeException e) {
                // analyzer limits (huge methods): not a defect of the translation
                methodsSkipped++;
            }
        }
    }

    private void record(ClassNode cls, MethodNode m, AnalyzerException e) {
        String message = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
        if (e.getCause() instanceof AnalyzerException inner) message = String.valueOf(inner.getMessage()).lines().findFirst().orElse(message);
        else message = message.replaceFirst("^Error at instruction \\d+: ", "");
        String expected = "", found = "";
        Matcher mt = EXPECTED_FOUND.matcher(message);
        if (mt.find()) {
            expected = readable(mt.group(1));
            found = readable(mt.group(2));
        }
        AbstractInsnNode node = e.node;
        int index = node == null ? -1 : m.instructions.indexOf(node);
        int line = 0;
        for (AbstractInsnNode n = node; n != null; n = n.getPrevious()) {
            if (n instanceof LineNumberNode ln) {
                line = ln.line;
                break;
            }
        }
        Finding f = new Finding(cls.name, m.name + m.desc, index, line, describe(node), expected, found, message);
        failureCount++;
        findingCount++;
        if (failures.size() < MAX_LISTED) failures.add(cls.name + "." + m.name + m.desc + ": " + message);
        String key = expected.isEmpty() ? "msg:" + message : expected + " <- " + found;
        Group g = groups.computeIfAbsent(key, k -> new Group(f.expected(), f.found(), f.message()));
        g.sites++;
        if (g.examples.size() < MAX_EXAMPLES) g.examples.add(f);
    }

    /** {@code Lfoo/Bar;} to {@code foo.Bar}; arrays and primitives keep their descriptor form. */
    private static String readable(String desc) {
        String d = desc.trim();
        if (d.startsWith("L") && d.endsWith(";")) return d.substring(1, d.length() - 1).replace('/', '.');
        return d;
    }

    private static final Map<Integer, String> OPCODE_NAMES = new HashMap<>();

    static {
        for (java.lang.reflect.Field fld : Opcodes.class.getFields()) {
            String n = fld.getName();
            if (fld.getType() != int.class || n.startsWith("ACC_") || n.startsWith("T_") || n.startsWith("H_") || n.startsWith("F_")
                    || n.startsWith("V") || n.startsWith("ASM") || n.startsWith("SOURCE_")) continue;
            try {
                OPCODE_NAMES.putIfAbsent(fld.getInt(null), n);
            } catch (IllegalAccessException ignored) {
                // public constants
            }
        }
    }

    private static String describe(AbstractInsnNode n) {
        if (n == null) return "?";
        String op = OPCODE_NAMES.getOrDefault(n.getOpcode(), "op" + n.getOpcode());
        if (n instanceof MethodInsnNode mi) return op + " " + mi.owner + "." + mi.name + mi.desc;
        if (n instanceof FieldInsnNode fi) return op + " " + fi.owner + "." + fi.name + ":" + fi.desc;
        if (n instanceof TypeInsnNode ti) return op + " " + ti.desc;
        return op;
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

        /** Null (and recorded) for a type nobody knows. */
        private ClassInfo lookup(String name) {
            ClassInfo c = hierarchy.find(name);
            if (c == null) unknownTypes.add(name);
            return c;
        }

        private ClassInfo info(Type type) {
            ClassInfo c = lookup(type.getInternalName());
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
            ClassInfo target = lookup(type.getInternalName());
            if (target == null) return true; // unknown: assumed assignable (the link check reports the missing class)
            if (other.getSort() == Type.ARRAY) return (target.access & Opcodes.ACC_INTERFACE) != 0; // arrays implement Cloneable/Serializable
            if ((target.access & Opcodes.ACC_INTERFACE) != 0) return true; // the JVM verifier treats interface-typed values as Object
            for (String c = other.getInternalName(); c != null; ) {
                if (c.equals(type.getInternalName())) return true;
                ClassInfo ci = lookup(c);
                if (ci == null) return true;
                c = ci.superName;
            }
            return false;
        }

        /** ASM's version asks a class loader whether the expected class is an interface; here an unknown class is assignable and a known one decides from its flags. */
        @Override
        protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
            Type type = value.getType();
            Type expectedType = expected.getType();
            if (type == null || expectedType == null) return type == null && expectedType == null;
            if (type.equals(expectedType)) return true;
            if (expectedType.getSort() != Type.OBJECT && expectedType.getSort() != Type.ARRAY) return false;
            if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) return false; // a primitive where a reference is required
            if (type.getSort() == Type.OBJECT && type.getInternalName().equals("null")) return true;
            int dim = 0;
            if (type.getSort() == Type.ARRAY) {
                dim = type.getDimensions();
                type = type.getElementType();
                if (type.getSort() != Type.OBJECT) {
                    dim--;
                    type = Type.getObjectType("java/lang/Object");
                }
            }
            int expectedDim = 0;
            if (expectedType.getSort() == Type.ARRAY) {
                expectedDim = expectedType.getDimensions();
                expectedType = expectedType.getElementType();
                if (expectedType.getSort() != Type.OBJECT) return false;
            }
            if (dim < expectedDim) return false;
            if (dim > expectedDim) type = Type.getObjectType("java/lang/Object");
            return isAssignableFrom(expectedType, type);
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new UnknownType(type.getInternalName());
        }
    }

    /**
     * {@code BytecodeCheck <jar-or-class-dir>...}: verifies jars or class directories against the classpath of the JVM running it
     * ({@code ./gradlew verifyClasses -PverifyPath=...}). Exit code 1 when anything is found.
     */
    public static void main(String[] args) throws Exception {
        Hierarchy hierarchy = new Hierarchy(ShimIndex.empty(), BytecodeCheck.class.getClassLoader());
        List<byte[]> all = new ArrayList<>();
        for (String a : args) {
            Path p = Path.of(a);
            if (Files.isDirectory(p)) {
                try (var walk = Files.walk(p)) {
                    for (Path f : walk.filter(x -> x.toString().endsWith(".class")).toList()) all.add(Files.readAllBytes(f));
                }
            } else all.addAll(readJar(p));
        }
        BytecodeCheck check = runClasses(all, hierarchy);
        System.out.println(check.summary());
        for (Group g : check.groups.values()) {
            System.out.println("  " + g.sites + " site(s): " + g.describe());
            for (Finding f : g.examples) System.out.println("    " + f.site());
        }
        if (check.findingCount > 0) System.exit(1);
    }
}
