package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.transform.Notch125To147;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Over the real 1.2.5 archives (gitignored, {@code -Dmc125.corpus=<dir>}, default the main checkout's test-mods-mcarchive/1.2.5) and with the join table ({@code -Pmappings125}): how many mod
 * methods override a game method whose signature changed in 1.3, and how many of those get a 1.4.7-shaped bridge. Skipped without either. Writes {@code build/reports/mc125-bridges.txt}.
 */
class Mc125CorpusBridgesTest {
    @Test
    void realOverridesOfChangedSignaturesAreBridged() throws IOException {
        assumeTrue(Notch125To147.available(), "built without -Pmappings125");
        Path dir = Path.of(System.getProperty("mc125.corpus", "Y:/Programming/Minecraft/Retrofit/test-mods-mcarchive/1.2.5"));
        assumeTrue(Files.isDirectory(dir), "no corpus at " + dir);
        int changed = 0, bridged = 0, units = 0, never = 0;
        Map<String, Integer> adaptedJars = new TreeMap<>(), unadaptedJars = new TreeMap<>();
        Map<String, Integer> reasons = new TreeMap<>();
        StringBuilder report = new StringBuilder();
        List<Path> files = new ArrayList<>();
        try (var s = Files.list(dir)) {
            s.filter(p -> p.toString().toLowerCase().endsWith(".zip") || p.toString().toLowerCase().endsWith(".jar")).sorted().forEach(files::add);
        }
        for (Path p : files) {
            if (p.getFileName().toString().contains("-src")) continue;
            LegacyJar jar = new LegacyJar(p);
            Map<String, byte[]> classes = new LinkedHashMap<>();
            try (ZipFile zip = new ZipFile(p.toFile())) {
                if (Mc125Jars.apiDistribution(zip) != null || Mc125Jars.isMcpNamedServerBuild(zip)) continue;
                if (Mc125Jars.score(zip) < Mc125Jars.MIN_SHARE) continue;
                jar.notchSide = Mc125Jars.sideOf(zip);
                var en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
                    try (InputStream in = zip.getInputStream(e)) {
                        classes.put(e.getName().substring(0, e.getName().length() - 6), in.readAllBytes());
                    }
                }
            } catch (IOException | RuntimeException e) {
                continue;
            }
            Notch125To147.apply(jar, classes);
            int[] c = Notch125To147.lastBridgeCounts();
            units++;
            changed += c[0];
            bridged += c[1];
            never += Notch125To147.lastUnbridged().size();
            for (String k : Notch125To147.lastAdaptedCalls().keySet()) adaptedJars.merge(k, 1, Integer::sum);
            for (String k : Notch125To147.lastUnadaptedCalls().keySet()) unadaptedJars.merge(k, 1, Integer::sum);
            for (String u : Notch125To147.lastUnbridged()) {
                String reason = u.substring(u.indexOf(": ") + 2);
                reasons.merge(reason.replaceAll("\\d+", "N"), 1, Integer::sum);
                report.append(p.getFileName()).append(": ").append(u).append('\n');
            }
            if (c[0] > 0) report.append(p.getFileName()).append(": ").append(c[0]).append(" overrides, ").append(c[1]).append(" bridged\n");
        }
        String summary = units + " units: " + changed + " overrides of changed-signature methods, " + bridged + " bridged; overrides never called: " + never + " (by reason " + reasons + "); calls of changed-signature game methods adapted: " + adaptedJars.size() + " distinct, not adaptable: " + unadaptedJars.size() + " distinct";
        System.out.println("MC125 BRIDGES: " + summary);
        Path out = Path.of("build", "reports", "mc125-bridges.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, summary + "\n" + report);
        assertTrue(bridged * 10 >= changed * 7, "most overrides should be bridgeable: " + summary);
    }
}
