import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Writes loader/src/main/resources/legacyconnector/legacy/1.7.10/static-arrays.txt: the vanilla 1.7.10 static fields initialised with
 * literal arrays (String[], int[], float[], float[][], ...), e.g. ItemDye's dye names and colours. Mods read them
 * (ItemDye.field_150923_a[meta]) and the shim classes do not run vanilla's static initialisers; the shim build
 * declares them with these values.
 * Line: owner(srg internal name) field(srg) descriptor value   (1D: a|b|c; 2D: a|b;c|d; escapes \p for |, \s for ;, \\ for \)
 *
 * usage (tools/extract_static_arrays.sh): java -cp asm:asm-tree ExtractStaticArrays.java <client.jar> <joined.srg> <out>
 */
public class ExtractStaticArrays {
    static final Object UNKNOWN = new Object();
    /** Stores seen per array (identity): arrays filled by loops or computed values are not literals and are skipped. */
    static final IdentityHashMap<Object, Integer> STORES = new IdentityHashMap<>();

    public static void main(String[] args) throws Exception {
        Map<String, String> classes = new HashMap<>();
        Map<String, String> fields = new HashMap<>(); // notchOwner/notchName -> srg name
        for (String line : Files.readAllLines(Path.of(args[1]))) {
            String[] p = line.split(" ");
            if (p[0].equals("CL:")) classes.put(p[1], p[2]);
            else if (p[0].equals("FD:")) fields.put(p[1], p[2].substring(p[2].lastIndexOf('/') + 1));
        }
        List<String> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(args[0])) {
            for (var e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                String srgClass = classes.get(node.name);
                if (srgClass == null || !srgClass.startsWith("net/minecraft/")) continue;
                MethodNode clinit = node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
                if (clinit == null) continue;
                Deque<Object> stack = new ArrayDeque<>();
                for (AbstractInsnNode insn : clinit.instructions) {
                    int op = insn.getOpcode();
                    if (op < 0) continue;
                    try {
                        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) stack.push(op - Opcodes.ICONST_0);
                        else if (op == Opcodes.FCONST_0 || op == Opcodes.FCONST_1 || op == Opcodes.FCONST_2) stack.push((float) (op - Opcodes.FCONST_0));
                        else if (op == Opcodes.DCONST_0 || op == Opcodes.DCONST_1) stack.push((double) (op - Opcodes.DCONST_0));
                        else if (op == Opcodes.ACONST_NULL) stack.push(UNKNOWN);
                        else if (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) stack.push(((IntInsnNode) insn).operand);
                        else if (op == Opcodes.LDC) stack.push(((LdcInsnNode) insn).cst);
                        else if (op == Opcodes.NEWARRAY) {
                            int n = (Integer) stack.pop();
                            int t = ((IntInsnNode) insn).operand;
                            stack.push(switch (t) {
                                case Opcodes.T_INT -> new int[n];
                                case Opcodes.T_FLOAT -> new float[n];
                                case Opcodes.T_DOUBLE -> new double[n];
                                case Opcodes.T_BOOLEAN -> new boolean[n];
                                case Opcodes.T_BYTE -> new byte[n];
                                case Opcodes.T_SHORT -> new short[n];
                                case Opcodes.T_LONG -> new long[n];
                                default -> UNKNOWN;
                            });
                        } else if (op == Opcodes.ANEWARRAY) {
                            int n = (Integer) stack.pop();
                            String t = ((TypeInsnNode) insn).desc;
                            stack.push(t.equals("java/lang/String") ? new String[n] : t.startsWith("[") ? new Object[n] : UNKNOWN);
                        } else if (op == Opcodes.DUP) {
                            stack.push(stack.peek());
                        } else if (op == Opcodes.IASTORE || op == Opcodes.FASTORE || op == Opcodes.DASTORE || op == Opcodes.BASTORE
                                || op == Opcodes.AASTORE || op == Opcodes.SASTORE || op == Opcodes.LASTORE) {
                            Object v = stack.pop();
                            int i = (Integer) stack.pop();
                            Object arr = stack.pop();
                            if (arr == UNKNOWN || v == UNKNOWN) continue;
                            STORES.merge(arr, 1, Integer::sum);
                            if (arr instanceof int[] a) a[i] = (Integer) v;
                            else if (arr instanceof float[] a) a[i] = ((Number) v).floatValue();
                            else if (arr instanceof double[] a) a[i] = ((Number) v).doubleValue();
                            else if (arr instanceof boolean[] a) a[i] = ((Integer) v) != 0;
                            else if (arr instanceof byte[] a) a[i] = (byte) (int) (Integer) v;
                            else if (arr instanceof short[] a) a[i] = (short) (int) (Integer) v;
                            else if (arr instanceof long[] a) a[i] = ((Number) v).longValue();
                            else if (arr instanceof String[] a) a[i] = (String) v;
                            else if (arr instanceof Object[] a) a[i] = v;
                        } else if (op == Opcodes.PUTSTATIC) {
                            FieldInsnNode f = (FieldInsnNode) insn;
                            Object v = stack.isEmpty() ? UNKNOWN : stack.pop();
                            if (f.owner.equals(node.name) && v != UNKNOWN && f.desc.startsWith("[")) {
                                String text = encode(v);
                                String srgField = fields.get(node.name + "/" + f.name);
                                if (text != null && srgField != null) out.add(srgClass + " " + srgField + " " + descriptor(f.desc, classes) + " " + text);
                            }
                            stack.clear();
                        } else {
                            stack.clear(); // anything else (calls, other stores): not a literal initialiser
                        }
                    } catch (RuntimeException ex) {
                        stack.clear();
                    }
                }
            }
        }
        Collections.sort(out);
        Files.createDirectories(Path.of(args[2]).getParent());
        Files.write(Path.of(args[2]), out);
        System.out.println(out.size() + " static arrays");
    }

    static String descriptor(String desc, Map<String, String> classes) {
        return desc;
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\p").replace(";", "\\s");
    }

    static boolean complete(Object v) {
        int length = java.lang.reflect.Array.getLength(v);
        if (length > 512) return false;
        if (v instanceof Object[] rows && !(v instanceof String[])) {
            for (Object row : rows) if (row == null || !complete(row)) return false;
        }
        return STORES.getOrDefault(v, 0) >= length;
    }

    static String encode(Object v) {
        if (!complete(v)) return null;
        if (v instanceof String[] a) {
            for (String s : a) if (s == null) return null;
            StringJoiner j = new StringJoiner("|");
            for (String s : a) j.add(esc(s));
            return j.toString();
        }
        if (v instanceof int[] a) return Arrays.toString(a).replaceAll("[\\[\\] ]", "").replace(",", "|");
        if (v instanceof float[] a) return join1(a);
        if (v instanceof double[] a) {
            StringJoiner j = new StringJoiner("|");
            for (double d : a) j.add(Double.toString(d));
            return j.toString();
        }
        if (v instanceof boolean[] a) {
            StringJoiner j = new StringJoiner("|");
            for (boolean d : a) j.add(d ? "1" : "0");
            return j.toString();
        }
        if (v instanceof Object[] rows) {
            StringJoiner j = new StringJoiner(";");
            for (Object row : rows) {
                if (!(row instanceof float[] f)) return null;
                j.add(join1(f));
            }
            return j.toString();
        }
        return null;
    }

    static String join1(float[] a) {
        StringJoiner j = new StringJoiner("|");
        for (float f : a) j.add(Float.toString(f));
        return j.toString();
    }
}
