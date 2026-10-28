package org.kylepitcock.legacyconnector.core.pack;

import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Folders 1.7.10 served as assets: {@code <game dir>/resources/<domain>/<path>} (Custom Main Menu keeps its images in
 * {@code resources/mainmenu}) and TXLoader's {@code config/txloader/load|forceload/<domain>/<path>} (GT New Horizons puts
 * its main menu texts there). Each is a pack that only exists when its folder does. Names match regardless of case, as the
 * legacy resource system did, and {@code lang/<locale>.lang} files are served as the {@code .json} files 1.13+ reads.
 */
public final class GameResourcesFolderPack extends AbstractPackResources {
    private final Path root;
    /** Per namespace: lower-cased path (as the modern resource system names it) -> file. */
    private final Map<String, Map<String, Path>> files = new HashMap<>();

    private GameResourcesFolderPack(PackLocationInfo location, Path root) {
        super(location);
        this.root = root;
        index();
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path game = FMLPaths.GAMEDIR.get();
        add(event, "legacyconnector_resources_folder", "resources folder", game.resolve("resources"));
        add(event, "legacyconnector_txloader_load", "TXLoader load folder", game.resolve("config/txloader/load"));
        add(event, "legacyconnector_txloader_forceload", "TXLoader forceload folder", game.resolve("config/txloader/forceload"));
    }

    private static void add(AddPackFindersEvent event, String id, String title, Path dir) {
        if (!Files.isDirectory(dir)) return;
        PackLocationInfo info = new PackLocationInfo(id, Component.literal("Legacy Connector: " + title), PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GameResourcesFolderPack(location, dir);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GameResourcesFolderPack(location, dir);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    private void index() {
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory).forEach(d -> {
                String ns = d.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!ResourceLocation.isValidNamespace(ns)) return;
                Map<String, Path> out = files.computeIfAbsent(ns, k -> new HashMap<>());
                try (Stream<Path> walk = Files.walk(d)) {
                    walk.filter(Files::isRegularFile).forEach(f -> {
                        String rel = d.relativize(f).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
                        if (rel.startsWith("lang/") && rel.endsWith(".lang")) rel = rel.substring(0, rel.length() - ".lang".length()) + ".json";
                        // a real .json next to a .lang wins
                        if (!out.containsKey(rel) || f.toString().endsWith(".json")) out.put(rel, f);
                    });
                } catch (IOException ignored) {
                    // an unreadable folder offers nothing
                }
            });
        } catch (IOException ignored) {
            // nothing to serve
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) {
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            byte[] meta = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"1.7.10 resources folder\"}}").getBytes(StandardCharsets.UTF_8);
            return () -> new ByteArrayInputStream(meta);
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        Map<String, Path> ns = files.get(location.getNamespace());
        Path file = ns == null ? null : ns.get(location.getPath());
        return file == null ? null : open(file);
    }

    private static IoSupplier<InputStream> open(Path file) {
        if (file.toString().endsWith(".lang")) return () -> new ByteArrayInputStream(langToJson(file));
        return () -> Files.newInputStream(file);
    }

    /** A 1.7.10 {@code key=value} lang file as a 1.13+ JSON lang file. */
    private static byte[] langToJson(Path file) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isEmpty() && line.charAt(0) == '﻿') line = line.substring(1);
            if (line.isBlank() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq > 0) entries.put(line.substring(0, eq), line.substring(eq + 1));
        }
        JsonObject json = new JsonObject();
        entries.forEach(json::addProperty);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        Map<String, Path> ns = files.get(namespace);
        if (ns == null) return;
        String prefix = path.endsWith("/") ? path : path + "/";
        ns.forEach((rel, file) -> {
            if (!rel.startsWith(prefix)) return;
            ResourceLocation rl = ResourceLocation.tryBuild(namespace, rel);
            if (rl != null) output.accept(rl, open(file));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? new HashSet<>(files.keySet()) : new HashSet<>();
    }

    @Override
    public void close() {
    }
}
