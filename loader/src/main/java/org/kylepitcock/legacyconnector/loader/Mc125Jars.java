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

    /** Jars that are the ModLoader / ModLoaderMP / Forge distribution itself: the layer provides that API, so they are skipped and reported. */
    private static final Map<Path, String> API_DISTRIBUTIONS = new ConcurrentHashMap<>();
    /** Default-package classes of the ModLoader and ModLoaderMP distributions (the shim provides them). */
    public static final Set<String> MODLOADER_API = Set.of("BaseMod", "ModLoader", "MLProp", "ModTextureStatic", "ModTextureAnimation", "EntityRendererProxy",
            "BaseModMp", "ModLoaderMp", "ISpawnable", "Packet230ModLoader", "NetClientHandlerEntity", "EntityTrackerEntry2");
    /** The {@code mod_*} entry classes of the API distributions (Forge's and ModLoaderMP's own loader hooks): not mods. */
    private static final Set<String> API_ENTRIES = Set.of("mod_MinecraftForge", "mod_ModLoaderMp");

    private Mc125Jars() {}

    /** The reason a jar was recognised as an API distribution (taken once), or null. */
    public static String takeApiDistribution(Path path) {
        return API_DISTRIBUTIONS.remove(path);
    }

    /** The line the compatibility report shows for such a jar. */
    public static String apiDistributionLine(String fileName, String what) {
        return fileName + ": " + what + "; Legacy Connector provides this API itself, so the file is not loaded (remove it)";
    }

    static void recordApiDistribution(Path path, String what) {
        API_DISTRIBUTIONS.put(path, what);
    }

    /**
     * The ModLoader, ModLoaderMP or Forge distribution zip: it carries ModLoader's own default-package classes (and Forge's {@code forge/} package, ModLoaderMP's {@code BaseModMp}) and no
     * mod of its own ({@code mod_MinecraftForge} and {@code mod_ModLoaderMp} are loader hooks). Null when the jar is something else; else a description.
     */
    static String apiDistribution(ZipFile zip) {
        int api = 0, forge = 0;
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            String n = en.nextElement().getName();
            if (!n.endsWith(".class")) continue;
            String c = n.substring(0, n.length() - 6);
            if (c.indexOf('/') < 0) {
                if (MODLOADER_API.contains(c)) api++;
                else if (c.startsWith("mod_") && c.indexOf('$') < 0 && !API_ENTRIES.contains(c)) return null; // a real mod
            } else if (c.startsWith("forge/")) forge++;
        }
        if (api == 0) return null;
        return forge > 0 ? "Minecraft Forge distribution (ModLoader + forge/*)" : "ModLoader / ModLoaderMP distribution";
    }

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
        /** {@code name desc} of every obfuscated member of any class: a reference through a subclass (javac names the receiver type, not the declaring class) matches here. */
        final Set<String> loose = new HashSet<>();
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
                        else if (p.length >= 5) {
                            side.members.add(p[2] + "." + p[3] + " " + p[4]);
                            side.loose.add(p[3] + " " + p[4]);
                        }
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
        return measure(zip)[0];
    }

    /** {share, side}: the share of {@link #score} and the side it was reached on (0 client, 1 server), {-1, 0} when it cannot be told. */
    static double[] measure(ZipFile zip) {
        Side[] s = sides();
        if (s[0].members.isEmpty()) return new double[] {-1, 0}; // table missing from this build
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
                                for (int i = 0; i < 2; i++) {
                                    // a member inherited from a superclass is named through the receiver's class (EntityPlayer.posX is declared in Entity)
                                    if (s[i].members.contains(key) || (s[i].classes.contains(owner) && s[i].loose.contains(mname + " " + mdesc))) hits[i]++;
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            } catch (IOException | RuntimeException e) {
                // an unreadable class does not count
            }
        }
        if (total[0] < MIN_REFS) return new double[] {-1, 0};
        return new double[] {Math.max(hits[0], hits[1]) / (double) total[0], hits[1] > hits[0] ? 1 : 0};
    }

    /**
     * Evidence text when the jar is a 1.2.5 jar (fingerprint at least {@link #MIN_SHARE} and clearly better than the 1.4.7, 1.5.2 and 1.6.4 tables), else null.
     * {@code others} are those tables' scores ({@code NotchFingerprint.scores}) or null.
     */
    /** Opt-in to host 1.2.5 jars through the 1.4.7 chain ({@code -Dlegacyconnector.mc125=true}); off by default. */
    public static boolean optedIn() {
        return Boolean.getBoolean("legacyconnector.mc125");
    }

    /** The side ('c' or 's') whose 1.2.5 names the jar fits better. */
    static char sideOf(ZipFile zip) {
        return measure(zip)[1] == 1 ? 's' : 'c';
    }

    static String detect(ZipFile zip, java.util.function.Supplier<double[]> othersSupplier) {
        double share = score(zip);
        if (share < MIN_SHARE) return null;
        double[] others = othersSupplier.get();
        if (others != null) for (double o : others) if (o + 0.15 >= share) return null;
        return "obfuscated names match Minecraft 1.2.5 (" + Math.round(share * 100) + "% of the member references)";
    }
}
