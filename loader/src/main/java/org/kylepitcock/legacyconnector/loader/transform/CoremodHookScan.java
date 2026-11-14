package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The hooks the coremods of a mod set put into Minecraft 1.7.10's own classes. Those classes do not exist at run time, so the transformers are shown the
 * original ones (the 1.7.10 client jar, {@link VanillaJar1710}) once, when the mod set is translated: first under the obfuscated name, as LaunchWrapper did
 * before FML's deobfuscation; when a transformer leaves that alone, in SRG names, as a transformer sorted after the deobfuscation saw it. What they add is
 * read off by {@link CoremodHookDiscovery} and written to a list the run time replays on the modern game ({@code CoremodSites}).
 *
 * <p>Known limit: Forge's own patches of the 1.7.10 classes are not applied first, so a transformer that looks for an instruction Forge added finds nothing.
 */
public final class CoremodHookScan {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");

    private CoremodHookScan() {}

    /** One transformed class: which transformer, the class (SRG name), the form it was shown in, before and after (both in SRG names). */
    public record Change(String transformer, String className, boolean obfuscated, ClassNode before, ClassNode after) {}

    /** Every change the transformers make to the classes of {@code vanillaJar}. */
    public static List<Change> changes(LegacyCoreModTransformers transformers, Path vanillaJar) throws IOException {
        NotchNames notch = NotchNames.get("1.7.10");
        Map<String, byte[]> vanilla = new TreeMap<>();
        Map<String, String[]> supers = new HashMap<>();
        try (ZipFile zip = new ZipFile(vanillaJar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                String name = e.getName().substring(0, e.getName().length() - 6);
                if (name.contains("/") && !name.startsWith("net/minecraft/")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    byte[] bytes = in.readAllBytes();
                    vanilla.put(name, bytes);
                    ClassReader r = new ClassReader(bytes);
                    String[] ifaces = r.getInterfaces();
                    String[] sup = new String[ifaces.length + 1];
                    sup[0] = r.getSuperName();
                    System.arraycopy(ifaces, 0, sup, 1, ifaces.length);
                    supers.put(name, sup);
                }
            }
        }
        Remapper srg = new Remapper() {
            @Override
            public String map(String name) {
                String mcp = notch.mcpClass(name);
                return mcp != null ? mcp : name;
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String s = notch.field(owner, name, supers);
                return s != null ? s : name;
            }

            @Override
            public String mapMethodName(String owner, String name, String desc) {
                String s = notch.method(owner, name, desc, supers);
                return s != null ? s : name;
            }
        };
        List<Change> out = new ArrayList<>();
        for (var e : vanilla.entrySet()) {
            String obf = e.getKey();
            String deobf = srg.map(obf);
            byte[] original = e.getValue();
            byte[] originalSrg = remap(original, srg);
            Map<String, Boolean> seen = new HashMap<>();
            transformers.applyToVanilla(obf.replace('/', '.'), deobf.replace('/', '.'), original, (name, result) -> {
                seen.put(name, true);
                out.add(new Change(name, deobf, true, node(originalSrg), node(remap(result, srg))));
            });
            transformers.applyToVanilla(deobf.replace('/', '.'), deobf.replace('/', '.'), originalSrg, (name, result) -> {
                if (!seen.containsKey(name)) out.add(new Change(name, deobf, false, node(originalSrg), node(result)));
            });
        }
        return out;
    }

    /** The hooks of every change, each once. */
    public static List<CoremodHookDiscovery.Hook> hooks(List<Change> changes) {
        Map<String, CoremodHookDiscovery.Hook> out = new LinkedHashMap<>();
        for (Change c : changes) {
            try {
                for (CoremodHookDiscovery.Hook h : CoremodHookDiscovery.hooks(c.before(), c.after())) out.putIfAbsent(h.toString(), h);
            } catch (RuntimeException e) {
                LOG.debug("Legacy Connector: the changes of {} to {} could not be read", c.transformer(), c.className(), e);
            }
        }
        return new ArrayList<>(out.values());
    }

    /**
     * Scans and writes the hook list ({@code site TAB owner TAB name TAB descriptor} per line); an empty list removes the file. Never throws: without the list
     * the mods load as before, without their hooks in Minecraft's classes.
     */
    public static void write(LegacyCoreModTransformers transformers, Path vanillaJar, Path file) {
        try {
            long start = System.nanoTime();
            List<Change> changes = changes(transformers, vanillaJar);
            List<CoremodHookDiscovery.Hook> hooks = hooks(changes);
            if (hooks.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            StringBuilder text = new StringBuilder();
            for (CoremodHookDiscovery.Hook h : hooks) {
                text.append(h.site()).append('\t').append(h.owner()).append('\t').append(h.name()).append('\t').append(h.desc()).append('\n');
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            LOG.info("Legacy Connector: legacy coremods put {} hook call(s) into {} of Minecraft 1.7.10's classes ({} ms); the run time replays the ones it has a place for",
                    hooks.size(), changes.stream().map(Change::className).distinct().count(), (System.nanoTime() - start) / 1_000_000);
        } catch (IOException | RuntimeException | LinkageError e) {
            LOG.warn("Legacy Connector: the hooks legacy coremods put into Minecraft's classes could not be listed: {}", e.toString());
            LOG.debug("Legacy Connector: coremod hook scan failure", e);
        }
    }

    private static byte[] remap(byte[] bytes, Remapper remapper) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassRemapper(writer, remapper), 0);
        return writer.toByteArray();
    }

    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }
}
