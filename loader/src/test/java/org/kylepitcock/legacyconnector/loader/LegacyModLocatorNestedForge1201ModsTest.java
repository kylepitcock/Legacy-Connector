package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyModLocatorNestedForge1201ModsTest {
    @TempDir
    Path tempDir;

    @Test
    void includesOnlyNestedForge1201ModIdsInFacadeEventRouting() throws Exception {
        byte[] palladiumCore = forgeModJar("palladiumcore", "[1.20.1,1.21)", false);
        byte[] olderForge = forgeModJar("oldnestedmod", "[1.19,1.20)", false);
        byte[] nativeNeoForge = forgeModJar("native_nested_mod", "[1.20.1,1.21)", true);

        Path parent = tempDir.resolve("palladium.jar");
        try (ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(parent))) {
            put(jar, "META-INF/jarjar/metadata.json",
                    ("{\"jars\":[{\"identifier\":{\"group\":\"net.threetag\","
                            + "\"artifact\":\"palladiumcore\",\"version\":\"1.0.1\"},"
                            + "\"version\":{\"range\":\"[1.0.1,2.0.0)\"},"
                            + "\"path\":\"META-INF/jarjar/palladiumcore.jar\"}]}")
                            .getBytes(StandardCharsets.UTF_8));
            put(jar, "META-INF/jarjar/palladiumcore.jar", palladiumCore);
            put(jar, "META-INF/jarjar/old-nested.jar", olderForge);
            put(jar, "META-INF/jars/native-nested.jar", nativeNeoForge);
        }

        Path scanDir = tempDir.resolve("scan");
        assertEquals(Set.of("palladiumcore"),
                LegacyModLocator.nestedForge1201ModIds(new LegacyJar(parent), scanDir));
        try (var files = Files.list(scanDir)) {
            assertEquals(0, files.count(), "temporary nested jars must be removed after inspection");
        }
    }

    private static byte[] forgeModJar(String modId, String minecraftRange, boolean neoForge) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String metadata = """
                modLoader="javafml"
                loaderVersion="[47,)"
                license="MIT"

                [[mods]]
                modId="%s"
                version="1.0.0"

                [[dependencies.%s]]
                modId="minecraft"
                mandatory=true
                versionRange="%s"
                ordering="NONE"
                side="BOTH"
                """.formatted(modId, modId, minecraftRange);
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            put(jar, "META-INF/mods.toml", metadata.getBytes(StandardCharsets.UTF_8));
            if (neoForge) {
                put(jar, "META-INF/neoforge.mods.toml",
                        "modLoader=\"javafml\"".getBytes(StandardCharsets.UTF_8));
            }
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
