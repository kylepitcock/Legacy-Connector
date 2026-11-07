package org.kylepitcock.legacyconnector.loader.tools;

import org.kylepitcock.legacyconnector.loader.resource.DataConverter1201;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Runs only the data and resource conversion of the 1.20.1 translation over jars (no code), writing jars that hold the converted resources:
 * {@code Data1201Tool <outDir> <jar|dir>...}; {@code tools/compat/data1201.py <outDir>} then measures what is left.
 */
public final class Data1201Tool {
    private Data1201Tool() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        DataConverter1201.Stats total = new DataConverter1201.Stats();
        for (int i = 1; i < args.length; i++) {
            Path p = Path.of(args[i]);
            if (Files.isDirectory(p)) {
                try (var s = Files.list(p)) {
                    for (Path j : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".jar")).sorted()::iterator) one(j, out, total);
                }
            } else {
                one(p, out, total);
            }
        }
        System.out.println("converted: " + total);
    }

    private static void one(Path jar, Path out, DataConverter1201.Stats total) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory() || e.getName().endsWith(".class")) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    entries.put(e.getName(), in.readAllBytes());
                }
            }
        } catch (IOException e) {
            System.out.println("skipped " + jar.getFileName() + ": " + e);
            return;
        }
        DataConverter1201.Stats stats = new DataConverter1201.Stats();
        Map<String, byte[]> converted = DataConverter1201.convertAll(entries, stats);
        // the mod metadata the transformer rewrites: only its presence matters to the checker
        if (converted.containsKey("META-INF/mods.toml")) converted.put("META-INF/neoforge.mods.toml", converted.remove("META-INF/mods.toml"));
        try (OutputStream o = Files.newOutputStream(out.resolve(jar.getFileName().toString())); ZipOutputStream z = new ZipOutputStream(o)) {
            for (var e : converted.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue());
                z.closeEntry();
            }
        }
        stats.counts.forEach((k, v) -> total.counts.merge(k, v, Integer::sum));
    }
}
