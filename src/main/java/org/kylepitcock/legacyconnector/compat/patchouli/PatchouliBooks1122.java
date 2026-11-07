package org.kylepitcock.legacyconnector.compat.patchouli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Patchouli books 1.12.2 mods ship as resources, handed to the modern Patchouli as data: the loader stages
 * {@code assets/<ns>/patchouli_books/**} of each translated jar under {@code legacyconnector_legacy/}, and this writes them to the generated
 * data pack as {@code data/<ns>/patchouli_books/**}, converted by {@link PatchouliBooks} (item, block, recipe and entity ids need the registries,
 * so this runs when the data pack is built). Active only with the modern Patchouli installed (the legacy one reads the originals in assets).
 */
public final class PatchouliBooks1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Patchouli");

    private PatchouliBooks1122() {}

    /** Whether the bridge is on: the modern Patchouli is loaded and {@code bridges.patchouli.enabled} is not false. */
    public static boolean active() {
        return net.neoforged.fml.ModList.get().isLoaded("patchouli") && LegacyConfig.bridgeEnabled("patchouli");
    }

    /** Makes the modern guide book usable by legacy code (called before any mod's preInit when the bridge is on). */
    public static void registerBridge() {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.bridgeModernItems("patchouli");
        BookStacks1122.register();
    }

    /** Resolver over the legacy registries. */
    static final PatchouliBooks.Resolver RESOLVER = new PatchouliBooks.Resolver() {
        @Override
        public String item(String spec) {
            return Ids1122.item(spec);
        }

        @Override
        public String block(String spec) {
            return Ids1122.block(spec);
        }

        @Override
        public String entity(String id) {
            return Ids1122.entity(id);
        }

        @Override
        public String recipe(String id) {
            int c = id.indexOf(':');
            String ns = c < 0 ? "minecraft" : id.substring(0, c);
            String path = c < 0 ? id : id.substring(c + 1);
            if (ns.equals("minecraft")) return "minecraft:" + Ids.normalizePath(path);
            return Ids.modernModId(ns) + ":" + Ids.normalizePath(path).replace('/', '_') + "_code";
        }

        @Override
        public String smelting(String inputSpec) {
            try {
                var input = Ids1122.stack(inputSpec);
                if (input == null || input.isEmpty()) return null;
                var result = legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingResult(input);
                if (result == null || result.isEmpty()) return null;
                var modern = Stacks1122.toModern(result);
                if (modern.isEmpty()) return null;
                var id = BuiltInRegistries.ITEM.getKey(modern.getItem());
                return "legacyconnector:smelting_1122/" + id.getNamespace() + "/" + id.getPath();
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
    };

    /** Adds the converted books of every 1.12.2 mod to the data pack. */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        if (!active()) return;
        Set<Object> seen = new HashSet<>();
        int books = 0;
        int files = 0;
        for (LegacyModRecord mod : LegacyMods.all()) {
            if (!"1.12.2".equals(mod.version) || mod.container == null) continue;
            var file = mod.container.getModInfo().getOwningFile().getFile();
            if (!seen.add(file)) continue;
            Path assets;
            try {
                assets = file.findResource("legacyconnector_legacy", "assets");
            } catch (RuntimeException e) {
                continue;
            }
            if (assets == null || !Files.isDirectory(assets)) continue;
            try (Stream<Path> namespaces = Files.list(assets)) {
                for (Path nsDir : (Iterable<Path>) namespaces.filter(Files::isDirectory)::iterator) {
                    Path booksDir = nsDir.resolve("patchouli_books");
                    if (!Files.isDirectory(booksDir)) continue;
                    String ns = nsDir.getFileName().toString();
                    int[] counts = convertNamespace(ns, booksDir, out);
                    books += counts[0];
                    files += counts[1];
                }
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cannot read the Patchouli books of {}: {}", mod.legacyId, e.toString());
            }
        }
        if (books > 0) LOG.info("Patchouli: {} books ({} files) of 1.12.2 mods converted for the modern Patchouli", books, files);
    }

    private static int[] convertNamespace(String ns, Path booksDir, Map<String, Supplier<byte[]>> out) throws IOException {
        int books = 0;
        int files = 0;
        try (Stream<Path> bookDirs = Files.list(booksDir)) {
            for (Path bookDir : (Iterable<Path>) bookDirs.filter(Files::isDirectory)::iterator) {
                String book = bookDir.getFileName().toString();
                if (!Files.isRegularFile(bookDir.resolve("book.json"))) continue;
                books++;
                try (Stream<Path> all = Files.walk(bookDir)) {
                    for (Path f : (Iterable<Path>) all.filter(Files::isRegularFile)::iterator) {
                        String rel = bookDir.relativize(f).toString().replace('\\', '/');
                        if (!PatchouliBooks.isJson(rel)) continue;
                        byte[] data = Files.readAllBytes(f);
                        byte[] converted;
                        try {
                            converted = PatchouliBooks.convert(rel, data, ns, RESOLVER);
                        } catch (RuntimeException | LinkageError e) {
                            LOG.warn("Cannot convert {}:{}/{}: {}", ns, book, rel, e.toString());
                            converted = data;
                        }
                        byte[] bytes = converted;
                        out.put("data/" + ns + "/patchouli_books/" + book + "/" + rel, () -> bytes);
                        files++;
                    }
                }
            }
        }
        return new int[] {books, files};
    }
}
