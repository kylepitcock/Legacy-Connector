package org.kylepitcock.mccompat.loader.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.kylepitcock.mccompat.loader.LegacyJar;
import org.kylepitcock.mccompat.loader.LegacyJarInspector;
import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.kylepitcock.mccompat.loader.transform.LegacyJarTransformer;

/**
 * Developer tool: {@code CheckJar <shim-index.txt> <legacy mod jar>...}. Translates jars offline and prints which
 * legacy APIs they use that the shim lacks, without starting the game.
 */
public final class CheckJar {
    private CheckJar() {}

    public static void main(String[] args) throws Exception {
        ShimIndex index;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            index = ShimIndex.read(in);
        }
        Path out = Files.createTempDirectory("mccompat-check");
        for (int i = 1; i < args.length; i++) {
            Path jarPath = Path.of(args[i]);
            LegacyJar jar = LegacyJarInspector.inspect(jarPath);
            if (jar == null) {
                System.out.println(jarPath.getFileName() + ": not a legacy Forge mod");
                continue;
            }
            var result = new LegacyJarTransformer(index, CheckJar.class.getClassLoader()).transform(jar, out.resolve("out-" + i + ".jar"));
            System.out.println(jar.fileName() + " [" + jar.version.id() + ", " + jar.versionEvidence + "]: " + result.classes + " classes, "
                    + result.links.errorCount() + " unresolved");
            if (jar.coreModClass != null) System.out.println("  coremod: " + jar.coreModClass + " (not executed)");
            System.out.print(result.links.render());
        }
    }
}
