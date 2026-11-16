package org.kylepitcock.legacyconnector.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minecraft 1.2.5 (ModLoader / early Forge) jars: detection only, see {@code docs/wiki/Legacy-1.2.5.md}. Such a jar names the game by obfuscated
 * (notch) names that only match 1.2.5 (the obfuscation is re-randomized with every release), so the share of its member references that resolve in the
 * 1.2.5 table ({@code detect/notch-1.2.5.txt.gz}, client and server, written by {@code tools/gen_notch_125.py}) tells it from 1.4.7, 1.5.2, 1.6.4 and
 * Beta 1.7.3 jars. A match is reported as "1.2.5 (not supported yet)" and the jar is not hosted; without this the 1.4.7 chooser would take it
 * (any bare {@code BaseMod} jar without other evidence is "the oldest supported version").
 */
public final class Mc125Jars {
    /** Folder of {@code mods/} whose jars are taken as 1.2.5 without the fingerprint. */
    public static final String FOLDER = "1.2.5";
    static final String RESOURCE = "legacyconnector/detect/notch-1.2.5.txt.gz";
    /** Share of obfuscated member references that must resolve in the 1.2.5 names. */
    static final double MIN_SHARE = 0.6;
    /** Fewer obfuscated member references than this say nothing about the version. */
    static final int MIN_REFS = 15;

    /** Jars found to be 1.2.5, with the reason (reported by the locator; the jar is not hosted). */
    private static final Map<Path, String> UNSUPPORTED = new ConcurrentHashMap<>();

    private Mc125Jars() {}

    /** The reason a jar was recognised as 1.2.5 (taken once), or null. */
    public static String takeUnsupported(Path path) {
        return UNSUPPORTED.remove(path);
    }

    /** The line the compatibility report shows for such a jar. */
    public static String reportLine(String fileName, String evidence) {
        return fileName + ": Minecraft 1.2.5 (not supported yet): " + evidence + "; not loaded";
    }

    static void record(Path path, String evidence) {
        UNSUPPORTED.put(path, evidence);
    }

    /** The obfuscated names of one side. */
    static final class Side {
        final Set<String> classes = new HashSet<>();
        final Set<String> members = new HashSet<>();
    }

    private static volatile Side[] sides;

    private static Side[] sides() {
        Side[] s = sides;
        if (s != null) return s;
        synchronized (Mc125Jars.class) {
            if (sides != null) return sides;
            Side client = new Side(), server = new Side();
            try (InputStream raw = Mc125Jars.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (raw != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
                    for (String line = r.readLine(); line != null; line = r.readLine()) {
                        String[] p = line.split("\t");
                        if (p.length < 3) continue;
                        Side side = p[0].equals("c") ? client : server;
                        if (p[1].equals("C")) side.classes.add(p[2]);
                        else if (p.length >= 5) side.members.add(p[2] + "." + p[3] + " " + p[4]);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt " + RESOURCE, e);
            }
            sides = new Side[] {client, server};
            return sides;
        }
    }

    /**
     * The share of the jar's references to obfuscated game members that resolve in the 1.2.5 names of the better-fitting side (client or server), or -1
     * when it has too few such references to say. References to classes of the jar itself and to named (non-default-package) owners do not count.
     */
    static double score(ZipFile zip) {
        Side[] s = sides();
        if (s[0].members.isEmpty()) return -1; // table missing from this build
        Set<String> own = new HashSet<>();
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            String n = en.nextElement().getName();
            if (n.endsWith(".class")) own.add(n.substring(0, n.length() - 6));
        }
        int[] hits = new int[2];
        int[] total = {0};
        en = zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry entry = en.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
            try (InputStream in = zip.getInputStream(entry)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String mname, String mdesc, boolean itf) {
                                count(owner, mname, mdesc);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String fname, String fdesc) {
                                count(owner, fname, fdesc);
                            }

                            private void count(String owner, String mname, String mdesc) {
                                if (owner.indexOf('/') >= 0 || own.contains(owner) || mname.length() > 3 || mname.startsWith("<")) return;
                                total[0]++;
                                String key = owner + "." + mname + " " + mdesc;
                                for (int i = 0; i < 2; i++) if (s[i].members.contains(key)) hits[i]++;
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            } catch (IOException | RuntimeException e) {
                // an unreadable class does not count
            }
        }
        if (total[0] < MIN_REFS) return -1;
        return Math.max(hits[0], hits[1]) / (double) total[0];
    }

    /**
     * Evidence text when the jar is a 1.2.5 jar (fingerprint at least {@link #MIN_SHARE} and clearly better than the 1.4.7, 1.5.2 and 1.6.4 tables), else null.
     * {@code others} are those tables' scores ({@code NotchFingerprint.scores}) or null.
     */
    static String detect(ZipFile zip, java.util.function.Supplier<double[]> othersSupplier) {
        double share = score(zip);
        if (share < MIN_SHARE) return null;
        double[] others = othersSupplier.get();
        if (others != null) for (double o : others) if (o + 0.15 >= share) return null;
        return "obfuscated names match Minecraft 1.2.5 (" + Math.round(share * 100) + "% of the member references)";
    }
}
