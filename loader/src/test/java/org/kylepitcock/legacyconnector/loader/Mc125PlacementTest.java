package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A 1.2.5 mod is a 1.2.5 mod by its content wherever the player put it: in mods/, in mods/1.2.5/ or in mods/1.4.7/ (an older build sorted such jars into that folder, and the
 * folder used to stop the detection: RedPower 2.0pr5b2 then loaded as a 1.4.7 mod and failed on forge/NetworkMod). Over the RedPower zips of the real archive
 * (gitignored, {@code -Dmc.corpus=<dir>}).
 */
class Mc125PlacementTest {
    private static Path root() {
        // the RedPower 2.0pr5b2 zips live in test-mods-issues/312 (not in the MCArchive corpus: the Beta suite's "RedPowerCore" fragment would pick them)
        return Path.of(System.getProperty("mc.issue312", "Y:/Programming/Minecraft/Retrofit/test-mods-issues/312/mods"));
    }

    @Test
    void redPowerIsDetectedInEveryPlacement() throws IOException {
        Path dir = root().resolve("1.2.5");
        assumeTrue(Files.isDirectory(dir), "no corpus at " + dir);
        List<Path> zips = new ArrayList<>();
        try (var s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith("RedPower") && p.toString().endsWith(".zip")).sorted().forEach(zips::add);
        }
        assumeTrue(org.kylepitcock.legacyconnector.loader.transform.Notch125To147.available(), "no 1.2.5 name table in this build (-Pmappings125)");
        assumeTrue(zips.size() >= 7, "RedPower zips not in the corpus");
        Path tmp = Files.createTempDirectory("mc125placement");
        List<String> wrong = new ArrayList<>();
        for (String folder : new String[] {"", "1.2.5", "1.4.7"}) {
            Path target = folder.isEmpty() ? tmp : tmp.resolve(folder);
            Files.createDirectories(target);
            for (Path z : zips) {
                Path copy = target.resolve(z.getFileName());
                Files.copy(z, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LegacyJar jar = LegacyJarInspector.inspect(copy);
                if (jar == null || jar.sourceVersion != LegacyVersion.V1_2_5) wrong.add((folder.isEmpty() ? "root" : folder) + "/" + z.getFileName() + " -> " + (jar == null ? "null" : jar.versionEvidence));
            }
        }
        System.out.println("Mc125PlacementTest: " + zips.size() + " RedPower zips x 3 placements, " + wrong.size() + " not taken as 1.2.5");
        assertTrue(wrong.isEmpty(), "not detected as 1.2.5: " + wrong);
    }
}
