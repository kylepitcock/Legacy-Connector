package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201CoremodAnalyzerTest {
    @TempDir
    Path tempDir;

    @Test
    void inventoriesDeclaredScriptsAndChecksOuterArchiveResourcePresence() throws Exception {
        Path jar = tempDir.resolve("coremods.jar");
        writeJar(jar, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH,
                """
                {
                  "zeta": "coremods/zeta.js",
                  "alpha": "coremods/alpha.js",
                  "missing": "coremods/missing.js"
                }
                """.getBytes(StandardCharsets.UTF_8),
                "coremods/zeta.js", "// inert fixture".getBytes(StandardCharsets.UTF_8),
                "coremods/alpha.js", "// inert fixture".getBytes(StandardCharsets.UTF_8)));

        var result = Forge1201CoremodAnalyzer.analyze(jar);

        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.PARSED, result.status());
        assertEquals(3, result.declarations().size());
        assertEquals(2, result.validDeclarationCount());
        assertEquals(1, result.missingScriptCount());
        assertEquals(0, result.malformedDeclarationCount());
        assertEquals("alpha", result.declarations().getFirst().name());
        assertEquals(Forge1201CoremodAnalyzer.ScriptStatus.RESOURCE_PRESENT,
                result.declarations().stream().filter(value -> value.name().equals("alpha"))
                        .findFirst().orElseThrow().scriptStatus());
        assertEquals(Forge1201CoremodAnalyzer.ScriptStatus.RESOURCE_MISSING,
                result.declarations().stream().filter(value -> value.name().equals("missing"))
                        .findFirst().orElseThrow().scriptStatus());
        assertTrue(result.sha256().matches("[0-9a-f]{64}"));
    }

    @Test
    void preservesDuplicateNamesAndRejectsMalformedOrUnsafeDeclarations() throws Exception {
        Path jar = tempDir.resolve("malformed-coremods.jar");
        writeJar(jar, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH,
                """
                {
                  "same": "first.js",
                  "same": "second.js",
                  "notString": 7,
                  "unsafe": "../escape.js"
                }
                """.getBytes(StandardCharsets.UTF_8),
                "first.js", new byte[] {1},
                "second.js", new byte[] {2}));

        var result = Forge1201CoremodAnalyzer.analyze(jar);

        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.PARTIALLY_PARSED, result.status());
        assertEquals(1, result.duplicateNameCount());
        assertEquals(2, result.malformedDeclarationCount());
        assertTrue(result.declarations().stream().anyMatch(value ->
                value.name().equals("same") && value.duplicateName()));
        assertTrue(result.declarations().stream().anyMatch(value ->
                value.name().equals("notString")
                        && value.scriptStatus() == Forge1201CoremodAnalyzer.ScriptStatus.INVALID_DECLARATION));
        assertTrue(result.declarations().stream().anyMatch(value ->
                value.name().equals("unsafe")
                        && value.scriptStatus() == Forge1201CoremodAnalyzer.ScriptStatus.UNSAFE_RESOURCE_PATH));
    }

    @Test
    void readsOnlyTheExactMetadataPathAndReportsInvalidInputs() throws Exception {
        Path absent = tempDir.resolve("absent.jar");
        writeJar(absent, Map.of("coremods.json", "{}".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.NOT_PRESENT,
                Forge1201CoremodAnalyzer.analyze(absent).status());

        Path invalidJson = tempDir.resolve("invalid-json.jar");
        writeJar(invalidJson, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH, "{not-json".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.INVALID_JSON,
                Forge1201CoremodAnalyzer.analyze(invalidJson).status());

        Path invalidRoot = tempDir.resolve("invalid-root.jar");
        writeJar(invalidRoot, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH, "[]".getBytes(StandardCharsets.UTF_8)));
        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.INVALID_JSON,
                Forge1201CoremodAnalyzer.analyze(invalidRoot).status());

        Path invalidUtf8 = tempDir.resolve("invalid-utf8.jar");
        writeJar(invalidUtf8, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH, new byte[] {(byte) 0xC3, (byte) 0x28}));
        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.INVALID_UTF8,
                Forge1201CoremodAnalyzer.analyze(invalidUtf8).status());

        Path oversized = tempDir.resolve("oversized.jar");
        writeJar(oversized, Map.of(
                Forge1201CoremodAnalyzer.RESOURCE_PATH, new byte[1024 * 1024 + 1]));
        assertEquals(Forge1201CoremodAnalyzer.ResultStatus.OVERSIZED,
                Forge1201CoremodAnalyzer.analyze(oversized).status());
    }

    private static void writeJar(Path path, Map<String, byte[]> entries) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }
}
