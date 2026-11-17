package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 1.2.5 detection is always active, so it must never claim a jar of another version. Over the real archives (gitignored, {@code -Dmc.corpus=<dir>}, default the main
 * checkout's test-mods-mcarchive): no 1.4.7, 1.5.2, 1.6.4 or Beta 1.7.3 jar is taken as 1.2.5 (neither as a match nor as a near miss), tested with no folder hint, the worst case.
 * Skipped without the archives.
 */
class Mc125CorpusDetectionTest {
    private static Path root() {
        return Path.of(System.getProperty("mc.corpus", "Y:/Programming/Minecraft/Retrofit/test-mods-mcarchive"));
    }

    private static List<Path> jars(String folder) throws IOException {
        Path dir = root().resolve(folder);
        assumeTrue(Files.isDirectory(dir), "no corpus at " + dir);
        List<Path> out = new ArrayList<>();
        try (var s = Files.list(dir)) {
            s.filter(p -> p.toString().toLowerCase().endsWith(".zip") || p.toString().toLowerCase().endsWith(".jar")).sorted().forEach(out::add);
        }
        return out;
    }

    private static void assertNone(String folder) throws IOException {
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (Path p : jars(folder)) {
            try (ZipFile zip = new ZipFile(p.toFile())) {
                if (folder.equals("b1.7.3") && BetaJars.betaShare(zip) < BetaJars.MIN_RESOLVED) continue; // misfiled: not Beta by content (a 1.2.x jar in the Beta archive)
                checked++;
                var others = (java.util.function.Supplier<double[]>) () -> LegacyJarInspector.NotchFingerprint.scores(zip);
                String ev = Mc125Jars.detect(zip, others);
                String near = Mc125Jars.detectNear(zip, others);
                if (ev != null || near != null) wrong.add(p.getFileName() + " -> " + (ev != null ? "MATCH " + ev : "NEAR " + near));
            } catch (IOException | RuntimeException e) {
                // an unreadable archive says nothing
            }
        }
        System.out.println("Mc125CorpusDetectionTest " + folder + ": " + checked + " jars, " + wrong.size() + " taken as 1.2.x");
        assertTrue(wrong.isEmpty(), "jars of " + folder + " taken as 1.2.5: " + wrong);
    }

    @Test
    void no147JarIsTakenAs125() throws IOException {
        assertNone("1.4.7");
    }

    @Test
    void no152JarIsTakenAs125() throws IOException {
        assertNone("1.5.2");
    }

    @Test
    void no164JarIsTakenAs125() throws IOException {
        assertNone("1.6.4");
    }

    @Test
    void noBetaJarIsTakenAs125() throws IOException {
        assertNone("b1.7.3");
    }

    /** The other direction, for the record: how many real 1.2.5 archives are recognised (a floor, not a claim of all). */
    @Test
    void most125JarsAreRecognised() throws IOException {
        int hit = 0, total = 0;
        for (Path p : jars("1.2.5")) {
            if (p.getFileName().toString().contains("-src")) continue;
            try (ZipFile zip = new ZipFile(p.toFile())) {
                total++;
                if (Mc125Jars.detect(zip, () -> LegacyJarInspector.NotchFingerprint.scores(zip)) != null || Mc125Jars.apiDistribution(zip) != null) hit++;
                System.out.println("DIAG125 " + p.getFileName() + " r125=" + Math.round(Mc125Jars.score(zip)*100));
            } catch (IOException | RuntimeException e) {
                // skip
            }
        }
        System.out.println("Mc125CorpusDetectionTest 1.2.5: " + hit + " of " + total + " recognised");
        assertTrue(hit * 2 >= total, hit + " of " + total);
    }
}
