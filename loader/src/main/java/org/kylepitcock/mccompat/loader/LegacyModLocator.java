package org.kylepitcock.mccompat.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.kylepitcock.mccompat.loader.shim.ShimIndex;
import org.kylepitcock.mccompat.loader.transform.LegacyJarTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the loader half, discovered by FML as an {@link IModFileCandidateLocator} service.
 *
 * <p>Runs before the built-in mods-folder locator (higher priority), claims every legacy Forge jar in the mods
 * folder via {@link ILaunchContext#addLocated(Path)} so FML does not reject it as "made for an older Forge",
 * translates it into a cached NeoForge jar and submits that instead. The original jar is never modified.</p>
 */
public final class LegacyModLocator implements IModFileCandidateLocator {
    private static final Logger LOG = LoggerFactory.getLogger("MCCompat");
    /** Nested runtime (shim) mod inside the distributed loader jar. Absent in development. */
    static final String EMBEDDED_RUNTIME = "/META-INF/mccompat/mccompat-runtime.jar";
    static final String SHIM_INDEX_PROPERTY = "mccompat.shimIndex";

    @Override
    public int getPriority() {
        return 100; // before ModsFolderLocator (0)
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path modsDir = FMLPaths.MODSDIR.get();
        Path cacheDir = gameDir.resolve(".mccompat").resolve("cache");
        try {
            Files.createDirectories(cacheDir);
            Path runtimeJar = extractRuntime(cacheDir);
            if (runtimeJar != null) {
                pipeline.addPath(runtimeJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }

            ShimIndex index = loadShimIndex(runtimeJar);
            LOG.info("MC Compatibility: shim index has {} legacy API classes", index.size());

            List<LegacyJar> legacy = new ArrayList<>();
            if (Files.isDirectory(modsDir)) {
                try (Stream<Path> files = Files.list(modsDir)) {
                    for (Path p : files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".jar"))
                            .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase())).toList()) {
                        if (context.isLocated(p)) continue;
                        try {
                            LegacyJar jar = LegacyJarInspector.inspect(p);
                            if (jar != null) legacy.add(jar);
                        } catch (IOException | RuntimeException e) {
                            LOG.debug("MC Compatibility: could not inspect {}", p, e);
                        }
                    }
                }
            }
            if (legacy.isEmpty()) return;

            LegacyJarTransformer transformer = new LegacyJarTransformer(index, LegacyModLocator.class.getClassLoader());
            Map<LegacyJar, String> outcomes = new LinkedHashMap<>();
            List<Path> translated = new ArrayList<>();
            for (LegacyJar jar : legacy) {
                // Claim the original even if we end up skipping it, so FML does not raise its own
                // "this is an old Forge mod" error on top of ours.
                context.addLocated(jar.path);
                String skip = skipReason(jar);
                if (skip != null) {
                    outcomes.put(jar, skip);
                    // Unknown translation keys are displayed verbatim by FML's error screen.
                    pipeline.addIssue(ModLoadingIssue.warning("MC Compatibility skipped " + jar.fileName() + ": " + skip.replace("'", ""))
                            .withAffectedPath(jar.path));
                    continue;
                }
                try {
                    Path out = cacheDir.resolve(cacheName(jar, index));
                    Path linkReport = out.resolveSibling(out.getFileName() + ".links.txt");
                    if (!Files.exists(out)) {
                        long start = System.nanoTime();
                        LegacyJarTransformer.Result result = transformer.transform(jar, out);
                        LOG.info("MC Compatibility: translated {} ({} classes, {} JDK fixes, {} virtual fields) in {} ms",
                                jar.fileName(), result.classes, result.compatRedirects, result.virtualFieldRedirects,
                                (System.nanoTime() - start) / 1_000_000);
                        StringBuilder text = new StringBuilder();
                        for (String n : result.notes) text.append("  note: ").append(n).append('\n');
                        text.append("  ").append(result.links.errorCount()).append(" unresolved link(s)")
                                .append(result.links.errorCount() == 0 ? " - fully covered by the shim\n" : ":\n");
                        text.append(result.links.render());
                        Files.writeString(linkReport, text);
                        if (result.links.errorCount() > 0) {
                            LOG.warn("MC Compatibility: {} uses {} legacy API(s) the shim does not provide yet; see logs/mccompat-report.txt",
                                    jar.fileName(), result.links.errorCount());
                        }
                    }
                    outcomes.put(jar, Files.exists(linkReport) ? Files.readString(linkReport) : "  (cached, no link report)\n");
                    translated.add(out);
                } catch (Exception e) {
                    LOG.error("MC Compatibility: failed to translate {}", jar.fileName(), e);
                    outcomes.put(jar, "translation failed: " + e);
                    pipeline.addIssue(ModLoadingIssue.error("MC Compatibility could not translate " + jar.fileName())
                            .withAffectedPath(jar.path).withCause(e));
                }
            }

            SplitPackageResolver.Outcome split = SplitPackageResolver.resolve(translated, cacheDir);
            if (split.sharedLibrary() != null) {
                LOG.info("MC Compatibility: {} packages shipped by several legacy jars were merged into {}",
                        split.sharedPackages().size(), split.sharedLibrary().getFileName());
                pipeline.addPath(split.sharedLibrary(), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            for (Path p : split.jars()) {
                pipeline.addPath(p, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
            }
            writeReport(gameDir, outcomes, split);
        } catch (IOException e) {
            LOG.error("MC Compatibility: legacy mod discovery failed", e);
        }
    }

    private static String skipReason(LegacyJar jar) {
        if (!jar.hasMods() && jar.coreModClass != null) {
            return "coremod-only jar (" + jar.coreModClass + "); legacy coremods patch "
                    + jar.version.id() + " Minecraft bytecode that does not exist in 1.21.1";
        }
        if (!jar.hasMods() && jar.tweakClass != null) {
            return "LaunchWrapper tweaker (" + jar.tweakClass + ") with no @Mod; tweakers cannot run on modlauncher";
        }
        return null;
    }

    /** Content hash + transformer version + shim index hash: any change re-translates. */
    private static String cacheName(LegacyJar jar, ShimIndex index) throws IOException {
        MessageDigest md = SplitPackageResolver.sha256();
        md.update(Files.readAllBytes(jar.path));
        md.update(("v" + LegacyJarTransformer.TRANSFORMER_VERSION + ":" + index.size()).getBytes(StandardCharsets.UTF_8));
        if (indexDigest != null) md.update(indexDigest);
        String name = jar.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.endsWith(".jar")) name = name.substring(0, name.length() - 4);
        return name + "-" + HexFormat.of().formatHex(md.digest()).substring(0, 16) + ".jar";
    }

    private static byte[] indexDigest;

    private static Path extractRuntime(Path cacheDir) throws IOException {
        try (InputStream in = LegacyModLocator.class.getResourceAsStream(EMBEDDED_RUNTIME)) {
            if (in == null) return null; // development: the runtime mod is on the classpath already
            byte[] bytes = in.readAllBytes();
            String hash = HexFormat.of().formatHex(SplitPackageResolver.sha256().digest(bytes)).substring(0, 16);
            Path target = cacheDir.resolve("mccompat-runtime-" + hash + ".jar");
            if (!Files.exists(target)) Files.write(target, bytes);
            return target;
        }
    }

    private static ShimIndex loadShimIndex(Path runtimeJar) throws IOException {
        byte[] data = null;
        String override = System.getProperty(SHIM_INDEX_PROPERTY);
        if (override != null && Files.exists(Path.of(override))) {
            data = Files.readAllBytes(Path.of(override));
        } else if (runtimeJar != null) {
            try (ZipFile zip = new ZipFile(runtimeJar.toFile())) {
                ZipEntry e = zip.getEntry(ShimIndex.RESOURCE);
                if (e != null) try (InputStream in = zip.getInputStream(e)) {
                    data = in.readAllBytes();
                }
            }
        }
        if (data == null) {
            try (InputStream in = LegacyModLocator.class.getClassLoader().getResourceAsStream(ShimIndex.RESOURCE)) {
                if (in != null) data = in.readAllBytes();
            }
        }
        if (data == null) {
            LOG.warn("MC Compatibility: no shim index found; link checking and duplicate-class removal are disabled");
            return ShimIndex.empty();
        }
        indexDigest = SplitPackageResolver.sha256().digest(data);
        return ShimIndex.read(new java.io.ByteArrayInputStream(data));
    }

    private static void writeReport(Path gameDir, Map<LegacyJar, String> outcomes, SplitPackageResolver.Outcome split) {
        Path report = gameDir.resolve("logs").resolve("mccompat-report.txt");
        try {
            Files.createDirectories(report.getParent());
            try (Writer w = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
                w.write("MC Compatibility legacy mod report - " + LocalDateTime.now() + "\n");
                w.write("=".repeat(78) + "\n\n");
                for (var e : outcomes.entrySet()) {
                    LegacyJar jar = e.getKey();
                    w.write(jar.fileName() + "  [" + jar.version.id() + ", " + jar.versionEvidence + "]\n");
                    for (LegacyModDecl mod : jar.mods) {
                        w.write("  mod " + mod.legacyId + " -> " + mod.modernId() + "  (" + mod.name + " " + mod.version + ")\n");
                    }
                    if (jar.coreModClass != null) {
                        w.write("  WARNING: ships a coremod (" + jar.coreModClass + ") which is NOT executed. Features that\n"
                                + "           depend on patching Minecraft code will be missing.\n");
                    }
                    if (jar.mixinConfigs != null) w.write("  WARNING: ships mixins (" + jar.mixinConfigs + ") targeting legacy Minecraft; not applied.\n");
                    if (jar.accessTransformer != null) w.write("  NOTE: legacy access transformer " + jar.accessTransformer + " ignored (shim members are public).\n");
                    for (String n : jar.notes) w.write("  note: " + n + "\n");
                    String outcome = e.getValue();
                    w.write(outcome.startsWith("  ") ? outcome : "  " + outcome + "\n");
                    w.write("\n");
                }
                if (!split.sharedPackages().isEmpty()) {
                    w.write("Packages shared by several jars (moved to " + split.sharedLibrary().getFileName() + "):\n");
                    for (String p : split.sharedPackages()) w.write("  " + p.replace('/', '.') + "\n");
                }
            }
            LOG.info("MC Compatibility: wrote compatibility report to {}", report);
        } catch (IOException e) {
            LOG.warn("MC Compatibility: could not write report", e);
        }
    }

    @Override
    public String toString() {
        return "MC Compatibility legacy mod locator";
    }
}
