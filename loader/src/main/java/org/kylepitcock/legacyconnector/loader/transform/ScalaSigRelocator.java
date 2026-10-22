package org.kylepitcock.legacyconnector.loader.transform;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * Rewrites the pickled Scala signature of a translated class so that the Minecraft/Forge classes it names are the
 * relocated ones. Libraries that read Scala signatures reflectively (ForgeMultipart's mixin compiler compares the
 * signature's JVM descriptors with the class file's) otherwise see {@code net.minecraft.util.AxisAlignedBB} where
 * the class file now says {@code legacyconnector/v1_7_10/net/minecraft/util/AxisAlignedBB}.
 *
 * <p>The pickle's external references (class = EXTref with an owner chain of EXTMODCLASSref packages) are pointed at
 * new package chains appended to the entry table; nothing else changes.</p>
 */
public final class ScalaSigRelocator {
    private static final String SIG = "Lscala/reflect/ScalaSignature;";
    private static final String LONG_SIG = "Lscala/reflect/ScalaLongSignature;";

    private ScalaSigRelocator() {}

    /** @param relocate legacy internal class name to the name the class has after translation */
    public static int apply(ClassNode node, UnaryOperator<String> relocate) {
        if (node.visibleAnnotations == null) return 0;
        int changed = 0;
        for (AnnotationNode a : node.visibleAnnotations) {
            if (a.values == null) continue;
            boolean isLong = a.desc.equals(LONG_SIG);
            if (!isLong && !a.desc.equals(SIG)) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if (!"bytes".equals(a.values.get(i))) continue;
                Object v = a.values.get(i + 1);
                try {
                    if (!isLong && v instanceof String s) {
                        String out = rewrite(s, relocate);
                        if (out != null) {
                            a.values.set(i + 1, out);
                            changed++;
                        }
                    } else if (isLong && v instanceof List<?> parts) {
                        StringBuilder all = new StringBuilder();
                        for (Object p : parts) all.append((String) p);
                        String out = rewrite(all.toString(), relocate);
                        if (out != null) {
                            // the long form splits the string in chunks of 65535 bytes of modified UTF-8; keep chunks small
                            List<String> chunks = new ArrayList<>();
                            for (int at = 0; at < out.length(); at += 16000) chunks.add(out.substring(at, Math.min(out.length(), at + 16000)));
                            a.values.set(i + 1, chunks);
                            changed++;
                        }
                    }
                } catch (RuntimeException e) {
                    // leave an unreadable signature as it was
                }
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ byte codec (scala.reflect.internal.pickling.ByteCodecs)

    private static byte[] decode(String s) {
        int n = s.length();
        byte[] seven = new byte[n];
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            seven[i] = (byte) (c == 0 ? 0x7f : c - 1);
        }
        int outLen = n * 7 / 8;
        byte[] out = new byte[outLen];
        long acc = 0;
        int bits = 0, o = 0;
        for (int i = 0; i < n && o < outLen; i++) {
            acc |= (long) (seven[i] & 0x7f) << bits;
            bits += 7;
            while (bits >= 8 && o < outLen) {
                out[o++] = (byte) acc;
                acc >>>= 8;
                bits -= 8;
            }
        }
        return out;
    }

    private static String encode(byte[] data) {
        int outLen = (data.length * 8 + 6) / 7;
        StringBuilder sb = new StringBuilder(outLen);
        long acc = 0;
        int bits = 0, produced = 0;
        for (byte b : data) {
            acc |= (long) (b & 0xff) << bits;
            bits += 8;
            while (bits >= 7) {
                sb.append(fromSeven((int) (acc & 0x7f)));
                acc >>>= 7;
                bits -= 7;
                produced++;
            }
        }
        if (bits > 0 && produced < outLen) sb.append(fromSeven((int) (acc & 0x7f)));
        return sb.toString();
    }

    private static char fromSeven(int b) {
        return b == 0x7f ? (char) 0 : (char) (b + 1);
    }

    // ------------------------------------------------------------------ pickle

    private record Entry(int tag, byte[] payload) {}

    private static int[] pos = new int[1];

    private static int nat(byte[] buf, int[] at) {
        int x = 0;
        int b;
        do {
            b = buf[at[0]++];
            x = (x << 7) + (b & 0x7f);
        } while ((b & 0x80) != 0);
        return x;
    }

    private static void writeNat(ByteArrayOutputStream out, int x) {
        int groups = 1;
        for (int t = x >>> 7; t != 0; t >>>= 7) groups++;
        for (int g = groups - 1; g >= 0; g--) {
            int part = (x >>> (7 * g)) & 0x7f;
            out.write(g == 0 ? part : part | 0x80);
        }
    }

    private static String rewrite(String encoded, UnaryOperator<String> relocate) {
        byte[] pickle = decode(encoded);
        int[] at = {0};
        int major = nat(pickle, at), minor = nat(pickle, at), count = nat(pickle, at);
        List<Entry> entries = new ArrayList<>(count + 16);
        for (int i = 0; i < count; i++) {
            int tag = pickle[at[0]++] & 0xff;
            int len = nat(pickle, at);
            byte[] payload = new byte[len];
            System.arraycopy(pickle, at[0], payload, 0, len);
            at[0] += len;
            entries.add(new Entry(tag, payload));
        }
        // names and external refs
        String[] names = new String[count];
        int[] nameRef = new int[count], ownerRef = new int[count];
        java.util.Arrays.fill(ownerRef, -1);
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            if (e.tag == 1 || e.tag == 2) {
                names[i] = new String(e.payload, StandardCharsets.UTF_8);
            } else if (e.tag == 9 || e.tag == 10) {
                int[] p = {0};
                nameRef[i] = nat(e.payload, p);
                if (p[0] < e.payload.length) ownerRef[i] = nat(e.payload, p);
            }
        }
        Map<String, Integer> termNames = new HashMap<>();
        for (int i = 0; i < count; i++) if (entries.get(i).tag == 1) termNames.putIfAbsent(names[i], i);
        Map<String, Integer> chains = new HashMap<>();
        boolean any = false;
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            if (e.tag != 9 || ownerRef[i] < 0) continue;
            String className = names[nameRef[i]];
            if (className == null || entries.get(nameRef[i]).tag != 2) continue;
            String pkg = packagePath(entries, names, nameRef, ownerRef, ownerRef[i]);
            if (pkg == null) continue;
            String legacy = pkg.replace('.', '/') + "/" + className;
            if (!LegacyVersion.isRelocatedRoot(legacy)) continue;
            String relocated = relocate.apply(legacy);
            if (relocated == null || relocated.equals(legacy)) continue;
            int cut = relocated.lastIndexOf('/');
            if (cut < 0 || !relocated.substring(cut + 1).equals(className)) continue;
            int owner = chain(relocated.substring(0, cut), entries, termNames, chains);
            ByteArrayOutputStream p = new ByteArrayOutputStream();
            writeNat(p, nameRef[i]);
            writeNat(p, owner);
            entries.set(i, new Entry(9, p.toByteArray()));
            any = true;
        }
        if (!any) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeNat(out, major);
        writeNat(out, minor);
        writeNat(out, entries.size());
        for (Entry e : entries) {
            out.write(e.tag);
            writeNat(out, e.payload.length);
            out.write(e.payload, 0, e.payload.length);
        }
        return encode(out.toByteArray());
    }

    /** Qualified name of a chain of package references, or null when the chain is not purely packages. */
    private static String packagePath(List<Entry> entries, String[] names, int[] nameRef, int[] ownerRef, int ref) {
        StringBuilder sb = new StringBuilder();
        int cur = ref;
        int guard = 0;
        while (cur >= 0 && guard++ < 32) {
            Entry e = entries.get(cur);
            if (e.tag != 10) return null;
            String n = names[nameRef[cur]];
            if (n == null) return null;
            sb.insert(0, sb.length() == 0 ? n : n + ".");
            cur = ownerRef[cur];
        }
        return sb.toString();
    }

    private static int chain(String path, List<Entry> entries, Map<String, Integer> termNames, Map<String, Integer> chains) {
        Integer cached = chains.get(path);
        if (cached != null) return cached;
        int slash = path.lastIndexOf('/');
        String leaf = slash < 0 ? path : path.substring(slash + 1);
        int owner = slash < 0 ? -1 : chain(path.substring(0, slash), entries, termNames, chains);
        Integer name = termNames.get(leaf);
        if (name == null) {
            name = entries.size();
            entries.add(new Entry(1, leaf.getBytes(StandardCharsets.UTF_8)));
            termNames.put(leaf, name);
        }
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        writeNat(p, name);
        if (owner >= 0) writeNat(p, owner);
        int index = entries.size();
        entries.add(new Entry(10, p.toByteArray()));
        chains.put(path, index);
        return index;
    }
}
