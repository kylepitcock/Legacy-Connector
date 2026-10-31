package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Machine-bound signatures of the translated jars in the cache (docs/wiki/Cache-and-Redistribution.md).
 *
 * <p>A random per-installation key is created on first use (outside the game folder, so copying the folder to another machine does not
 * copy it); every translated jar gets a sidecar {@code <jar>.sig} with an HMAC-SHA256 over its size, a content digest computed once when
 * the jar is written, the digest of its last 64 KiB (the zip central directory), the original's hash and the translator version.
 * Verifying on load reads the sidecar and the last 64 KiB of the jar: it never hashes the whole jar. A cache entry whose signature does not
 * verify is discarded and translated again, which is also what happens when the key is lost or the instance is copied to another machine.
 * Everything here is best effort: no failure to read or write a key or signature may stop the game.</p>
 */
final class CacheSignature {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector");
    private static final int TAIL = 64 * 1024;
    private static final String FORMAT = "1";
    private static volatile byte[] key;

    private CacheSignature() {}

    /** Kill switch for development and for measuring: {@code -Dlegacyconnector.cacheSignatures=false}. */
    static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("legacyconnector.cacheSignatures"));
    }

    static Path sigFile(Path jar) {
        return jar.resolveSibling(jar.getFileName() + ".sig");
    }

    /** Signs the jar that was just written. Never throws. */
    static void sign(Path jar, String originalSha256, Path gameDir) {
        if (!enabled()) return;
        try {
            long size = Files.size(jar);
            String digest = Provenance.sha256Hex(jar);
            String tail = tailDigest(jar, size);
            String text = "format=" + FORMAT + "\nsize=" + size + "\ndigest=" + digest + "\ntail=" + tail + "\norig=" + originalSha256
                    + "\ntranslator=" + translator() + "\n";
            String mac = mac(key(gameDir), text);
            Path sig = sigFile(jar);
            Path tmp = sig.resolveSibling(sig.getFileName() + ".tmp");
            Files.writeString(tmp, text + "hmac=" + mac + "\n", StandardCharsets.UTF_8);
            Files.move(tmp, sig, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | java.security.GeneralSecurityException e) {
            LOG.debug("Legacy Connector: could not sign {} (it will be translated again next start)", jar, e);
        }
    }

    /** True if the jar has a signature made on this installation for this original. Cheap: no full hash. Never throws. */
    static boolean verify(Path jar, String originalSha256, Path gameDir) {
        if (!enabled()) return true;
        try {
            Path sig = sigFile(jar);
            if (!Files.isRegularFile(sig)) return false;
            String text = Files.readString(sig, StandardCharsets.UTF_8);
            int at = text.lastIndexOf("hmac=");
            if (at < 0) return false;
            String body = text.substring(0, at);
            String stored = text.substring(at + 5).trim();
            long size = Files.size(jar);
            if (!body.contains("size=" + size + "\n") || !body.contains("orig=" + originalSha256 + "\n")
                    || !body.contains("translator=" + translator() + "\n") || !body.startsWith("format=" + FORMAT + "\n")) return false;
            String tail = tailDigest(jar, size);
            if (!body.contains("tail=" + tail + "\n")) return false;
            return MessageDigest.isEqual(mac(key(gameDir), body).getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException | java.security.GeneralSecurityException e) {
            LOG.debug("Legacy Connector: could not verify {}", jar, e);
            return false;
        }
    }

    private static String translator() {
        return Provenance.translatorVersion() + "/" + org.kylepitcock.legacyconnector.loader.transform.LegacyJarTransformer.TRANSFORMER_VERSION;
    }

    private static String tailDigest(Path jar, long size) throws IOException {
        int n = (int) Math.min(size, TAIL);
        byte[] buf = new byte[n];
        try (RandomAccessFile f = new RandomAccessFile(jar.toFile(), "r")) {
            f.seek(size - n);
            f.readFully(buf);
        }
        return HexFormat.of().formatHex(SplitPackageResolver.sha256().digest(buf));
    }

    private static String mac(byte[] key, String text) throws java.security.GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
    }

    // ---- the installation key ----

    private static synchronized byte[] key(Path gameDir) {
        if (key != null) return key;
        Path fallback = gameDir.resolve(".legacyconnector").resolve("installation.key");
        List<Path> candidates = new java.util.ArrayList<>();
        Path home = userDir();
        if (home != null) candidates.add(home.resolve("installation.key"));
        candidates.add(fallback);
        for (Path p : candidates) {
            byte[] k = read(p);
            if (k != null) return key = k;
        }
        byte[] fresh = new byte[32];
        new SecureRandom().nextBytes(fresh);
        for (Path p : candidates) {
            try {
                Files.createDirectories(p.getParent());
                byte[] hex = HexFormat.of().formatHex(fresh).getBytes(StandardCharsets.US_ASCII);
                try {
                    Files.write(p, hex, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (FileAlreadyExistsException raced) {
                    byte[] k = read(p);
                    if (k != null) return key = k;
                    continue;
                }
                restrict(p);
                LOG.debug("Legacy Connector: created the installation key {}", p);
                return key = fresh;
            } catch (IOException | RuntimeException e) {
                LOG.debug("Legacy Connector: cannot store the installation key at {}", p, e);
            }
        }
        // nowhere to keep it: signatures are valid for this run only, jars are translated again next start
        return key = fresh;
    }

    private static byte[] read(Path p) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                if (!Files.isRegularFile(p)) return null;
                String s = Files.readString(p, StandardCharsets.US_ASCII).trim();
                if (s.length() == 64) return HexFormat.of().parseHex(s);
                Thread.sleep(50); // another process is still writing it
            } catch (IOException | RuntimeException e) {
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /** The per-user folder, outside any game folder: copying an instance to another machine leaves the key behind. */
    private static Path userDir() {
        String override = System.getProperty("legacyconnector.keyDir");
        if (override != null && !override.isBlank()) return Path.of(override);
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (home == null) return null;
        if (os.contains("win")) {
            String base = System.getenv("LOCALAPPDATA");
            return Path.of(base != null ? base : home, "LegacyConnector");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "LegacyConnector");
        String xdg = System.getenv("XDG_DATA_HOME");
        return xdg != null && !xdg.isBlank() ? Path.of(xdg, "legacyconnector") : Path.of(home, ".local", "share", "legacyconnector");
    }

    /** Owner-only access as far as the OS allows. */
    private static void restrict(Path p) {
        try {
            if (p.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
                return;
            }
            AclFileAttributeView acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
            if (acl != null) {
                acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(acl.getOwner())
                        .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("Legacy Connector: could not restrict access to {}", p, e);
        }
    }
}
