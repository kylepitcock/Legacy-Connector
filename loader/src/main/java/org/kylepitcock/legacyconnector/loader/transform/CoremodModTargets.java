package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Coremod targets that are classes of ANOTHER translated jar of the same mod set (Logistics Pipes' transformer extends Thermal Dynamics' {@code TileTDBase} and
 * {@code TravelingItem}), as opposed to Minecraft's own classes ({@link CoremodHookScan}). Nothing needs to be "found" first: the translation hands every class
 * of every jar of a legacy version to that version's transformers ({@code LegacyModLocator}'s patcher, {@link LegacyCoreModTransformers#apply}), exactly as
 * LaunchWrapper offered every class to every transformer, so a structural change (interface, field, method) to another mod's class is already part of that
 * class when its jar is translated and the translation's link check, peer scan and mixin generation see it. The result of translating the target jar
 * depends on the coremod jar, which is why the translation cache is keyed by the digest of the WHOLE set of jars of a version
 * ({@code LegacyModLocator.modSetDigests}), not by the target jar alone.
 *
 * <p>This class is the discovery side: it shows which classes of the set's jars the transformers change, with before and after, so the changes can be listed
 * ({@code CoremodHookProbe}), logged at translation, and checked offline.
 */
public final class CoremodModTargets {
    private CoremodModTargets() {}

    /** Every class of {@code jars} (that use the transformers' API flavour) a transformer changes, as before/after nodes; {@code transformer} is "set". */
    public static List<CoremodHookScan.Change> scan(LegacyCoreModTransformers transformers, List<LegacyJar> jars) {
        List<CoremodHookScan.Change> out = new ArrayList<>();
        if (transformers.isEmpty()) return out;
        for (LegacyJar jar : jars) {
            if (!LegacyCoreModTransformers.supports(jar.version)) continue;
            try (ZipFile zip = new ZipFile(jar.existing().toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String n = e.getName();
                    if (e.isDirectory() || !n.endsWith(".class")) continue;
                    String name = n.substring(0, n.length() - 6);
                    byte[] bytes;
                    try (InputStream in = zip.getInputStream(e)) {
                        bytes = in.readAllBytes();
                    }
                    byte[] changed = transformers.apply(name, bytes);
                    if (changed == bytes || java.util.Arrays.equals(changed, bytes)) continue;
                    out.add(new CoremodHookScan.Change("set", name, false, node(bytes), node(changed)));
                }
            } catch (IOException | RuntimeException ex) {
                // an unreadable jar is reported by the translation itself
            }
        }
        return out;
    }

    /** The types the changed classes now reference that are not Minecraft/Java platform classes: what the target jar needs from the coremod's jar. */
    public static Set<String> addedReferences(CoremodHookScan.Change c) {
        Set<String> out = new TreeSet<>();
        for (CoremodHookDiscovery.Addition a : CoremodHookDiscovery.additions(c.before(), c.after())) {
            if (a.kind().equals("interface")) out.add(a.name());
            else if (!a.desc().isEmpty()) {
                for (org.objectweb.asm.Type t : a.kind().equals("field") ? new org.objectweb.asm.Type[] {org.objectweb.asm.Type.getType(a.desc())}
                        : concat(org.objectweb.asm.Type.getArgumentTypes(a.desc()), org.objectweb.asm.Type.getReturnType(a.desc()))) {
                    if (t.getSort() == org.objectweb.asm.Type.ARRAY) t = t.getElementType();
                    if (t.getSort() == org.objectweb.asm.Type.OBJECT) out.add(t.getInternalName());
                }
            }
        }
        out.removeIf(n -> n.startsWith("java/") || n.startsWith("net/minecraft/"));
        return out;
    }

    private static org.objectweb.asm.Type[] concat(org.objectweb.asm.Type[] a, org.objectweb.asm.Type b) {
        org.objectweb.asm.Type[] r = java.util.Arrays.copyOf(a, a.length + 1);
        r[a.length] = b;
        return r;
    }

    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }
}
