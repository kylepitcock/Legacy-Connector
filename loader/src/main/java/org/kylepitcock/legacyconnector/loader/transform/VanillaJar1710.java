package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the original Minecraft 1.7.10 client jar, which {@link CoremodHookScan} shows to legacy coremods. Legacy Connector does not ship Minecraft's code:
 * the jar is taken from where a launcher keeps it, from {@code .legacyconnector/vanilla/client-1.7.10.jar}, from the path in {@code coremods.vanillaJar},
 * or (only with {@code coremods.downloadVanilla=true}) downloaded once from Mojang's own server. Only the exact official file is accepted (SHA-1).
 */
public final class VanillaJar1710 {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    private static final String SHA1 = "e80d9b3bf5085002218d4be59e668bac718abbc6";
    private static final String URL = "https://piston-data.mojang.com/v1/objects/" + SHA1 + "/client.jar";

    private VanillaJar1710() {}

    /** The jar, or null (after one log line saying how to provide it). */
    public static Path find(Path gameDir, Properties config) {
        Path own = gameDir.resolve(".legacyconnector").resolve("vanilla").resolve("client-1.7.10.jar");
        List<Path> candidates = new ArrayList<>();
        String configured = System.getProperty("legacyconnector.vanilla1710", config.getProperty("coremods.vanillaJar", "")).trim();
        if (!configured.isEmpty()) candidates.add(Path.of(configured));
        candidates.add(own);
        String home = System.getProperty("user.home", "");
        String appData = System.getenv("APPDATA");
        String version = "versions/1.7.10/1.7.10.jar";
        String library = "libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar";
        if (appData != null) {
            candidates.add(Path.of(appData, ".minecraft", version));
            candidates.add(Path.of(appData, "PrismLauncher", library));
            candidates.add(Path.of(appData, "MultiMC", library));
        }
        if (!home.isEmpty()) {
            candidates.add(Path.of(home, ".minecraft", version));
            candidates.add(Path.of(home, "Library", "Application Support", "minecraft", version));
            candidates.add(Path.of(home, "Library", "Application Support", "PrismLauncher", library));
            candidates.add(Path.of(home, ".local", "share", "PrismLauncher", library));
            candidates.add(Path.of(home, "curseforge", "minecraft", "Install", version));
        }
        // Prism and MultiMC instances: <root>/instances/<name>/.minecraft
        Path root = gameDir.toAbsolutePath().normalize();
        for (int i = 0; i < 3 && root != null; i++) root = root.getParent();
        if (root != null) candidates.add(root.resolve(library));
        Path parent = gameDir.toAbsolutePath().normalize().getParent();
        if (parent != null) candidates.add(parent.resolve("build/devtools/mcp/client1710.jar")); // a development run
        for (Path p : candidates) {
            try {
                if (Files.isRegularFile(p) && SHA1.equals(sha1(p))) return p;
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: {} could not be read: {}", p, e.toString());
            }
        }
        if ("true".equalsIgnoreCase(config.getProperty("coremods.downloadVanilla", "false").trim())) {
            try {
                Files.createDirectories(own.getParent());
                Path part = own.resolveSibling(own.getFileName() + ".part");
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpResponse<Path> response = client.send(HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(60)).build(),
                        HttpResponse.BodyHandlers.ofFile(part));
                if (response.statusCode() == 200 && SHA1.equals(sha1(part))) {
                    Files.move(part, own, StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("Legacy Connector: downloaded the Minecraft 1.7.10 client jar from Mojang to {} (coremods.downloadVanilla)", own);
                    return own;
                }
                Files.deleteIfExists(part);
                LOG.warn("Legacy Connector: the Minecraft 1.7.10 client jar could not be downloaded (HTTP {} or a wrong checksum)", response.statusCode());
            } catch (IOException | RuntimeException e) {
                LOG.warn("Legacy Connector: the Minecraft 1.7.10 client jar could not be downloaded: {}", e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Legacy Connector: legacy coremods that patch Minecraft's own classes are present, but the original Minecraft 1.7.10 client jar was not found, so "
                + "their hooks in Minecraft's code are not replayed. Install 1.7.10 once in your launcher, copy its jar to {}, or set coremods.downloadVanilla=true.", own);
        return null;
    }

    private static String sha1(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) > 0;) digest.update(buffer, 0, n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
