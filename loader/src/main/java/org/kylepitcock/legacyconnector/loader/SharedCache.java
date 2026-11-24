package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional read-through translation cache shared by several game directories or worktrees ({@code -Dlegacyconnector.sharedCache=<dir>} or the
 * environment variable {@code LEGACYCONNECTOR_SHARED_CACHE}; off by default). The per-instance cache stays the one the game loads from: on a
 * local miss the entry of the same full key is copied (hard-linked when the volume allows it) into it, and a local translation is published
 * here for the others.
 *
 * <p>An entry is a directory {@code <root>/<key>/} with the files of one translated jar (the translated jar plus its {@code .synth.txt},
 * {@code .links.txt}, {@code .risk.txt} companions) and a manifest {@code entry.txt} listing {@code part, size, sha256} of each. Entries are
 * immutable: a publisher builds the directory under a temporary name next to the others and renames it into place atomically, never replacing
 * an existing entry (the loser of a race discards its copy), and no lock is held while translating. A reader verifies size and hash of every
 * file and ignores (and logs) an entry that does not match.</p>
 */
final class SharedCache {
    private static final Logger LOG = LoggerFactory.getLogger("Legacy Connector");
    static final String MANIFEST = "entry.txt";
    /** The parts a usable entry must have; the others are optional. */
    static final List<String> REQUIRED = List.of("jar", "synth");
    /** Restore order: the part whose presence marks a local hit last. */
    private static final List<String> RESTORE_ORDER = List.of("links", "risk", "jar", "synth");

    /** Creates a hard link {@code link} to {@code existing}; replaceable so tests can force the copy fallback. */
    @FunctionalInterface
    interface Linker {
        void link(Path link, Path existing) throws IOException;
    }

    private final Path root;
    private final Linker linker;
    final AtomicInteger hits = new AtomicInteger();
    final AtomicInteger misses = new AtomicInteger();
    final AtomicInteger published = new AtomicInteger();
    final AtomicInteger corrupt = new AtomicInteger();
    final AtomicLong bytesSaved = new AtomicLong();

    SharedCache(Path root) {
        this(root, Files::createLink);
    }

    SharedCache(Path root, Linker linker) {
        this.root = root;
        this.linker = linker;
    }

    /** The configured shared cache, or null when the switch and the environment variable are both unset or blank. */
    static SharedCache configured() {
        String dir = LoaderFlags.SHARED_CACHE.get();
        if (dir == null || dir.isBlank()) dir = System.getenv("LEGACYCONNECTOR_SHARED_CACHE");
        if (dir == null || dir.isBlank()) return null;
        try {
            Path root = Path.of(dir.trim());
            Files.createDirectories(root);
            return new SharedCache(root);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Legacy Connector: shared translation cache {} is not usable: {}", dir, e.toString());
            return null;
        }
    }

    Path root() {
        return root;
    }

    boolean has(String key) {
        return Files.isRegularFile(root.resolve(key).resolve(MANIFEST));
    }

    /**
     * Restores the entry of {@code key} into {@code targets} (part name to local path; parts the entry lacks or the caller does not name are
     * skipped). False on a miss or a corrupt entry; nothing is left half written under a target name.
     */
    boolean fetch(String key, Map<String, Path> targets) {
        Path dir = root.resolve(key);
        Path manifest = dir.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) {
            misses.incrementAndGet();
            return false;
        }
        try {
            Map<String, String[]> parts = new LinkedHashMap<>();
            for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
                String[] f = line.split("\t");
                if (f.length == 3) parts.put(f[0], f);
            }
            for (String r : REQUIRED) if (!parts.containsKey(r)) throw new IOException("manifest lacks " + r);
            long total = 0;
            for (var e : parts.entrySet()) {
                Path file = dir.resolve(e.getKey());
                long size = Long.parseLong(e.getValue()[1]);
                if (!Files.isRegularFile(file) || Files.size(file) != size) throw new IOException(e.getKey() + ": size differs from the manifest");
                if (!hash(file).equals(e.getValue()[2])) throw new IOException(e.getKey() + ": content hash differs from the manifest");
                total += size;
            }
            for (String name : RESTORE_ORDER) {
                Path target = targets.get(name);
                if (target == null || !parts.containsKey(name)) continue;
                restore(dir.resolve(name), target);
            }
            hits.incrementAndGet();
            bytesSaved.addAndGet(total);
            return true;
        } catch (IOException | RuntimeException e) {
            corrupt.incrementAndGet();
            LOG.warn("Legacy Connector: ignoring corrupt shared cache entry {} ({})", key, e.getMessage());
            return false;
        }
    }

    private void restore(Path source, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".shared-" + UUID.randomUUID().toString().substring(0, 8) + ".tmp");
        try {
            try {
                linker.link(tmp, source);
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            move(tmp, target);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Publishes the files of one translation (part name to existing local file) as the entry of {@code key}. Does nothing when the entry exists
     * already; when another process wins the rename its entry stays and this copy is discarded. Never throws: a shared cache that cannot be
     * written must not fail a translation.
     */
    boolean publish(String key, Map<String, Path> sources) {
        Path finalDir = root.resolve(key);
        if (has(key)) return false;
        for (String r : REQUIRED) if (sources.get(r) == null || !Files.isRegularFile(sources.get(r))) return false;
        Path tmp = root.resolve(".tmp-" + UUID.randomUUID());
        try {
            Files.createDirectories(tmp);
            StringBuilder manifest = new StringBuilder();
            for (var e : sources.entrySet()) {
                if (e.getValue() == null || !Files.isRegularFile(e.getValue())) continue;
                Path copy = tmp.resolve(e.getKey());
                Files.copy(e.getValue(), copy);
                manifest.append(e.getKey()).append('\t').append(Files.size(copy)).append('\t').append(hash(copy)).append('\n');
            }
            Files.writeString(tmp.resolve(MANIFEST), manifest, StandardCharsets.UTF_8);
            try {
                try {
                    Files.move(tmp, finalDir, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, finalDir);
                }
            } catch (IOException e) {
                if (has(key)) return false; // another process published the same key first
                throw e;
            }
            published.incrementAndGet();
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Legacy Connector: could not publish {} to the shared cache: {}", key, e.toString());
            return false;
        } finally {
            deleteTree(tmp);
        }
    }

    String summary() {
        return "shared cache " + root + ": " + hits + " hit(s), " + misses + " miss(es), " + published + " published"
                + (corrupt.get() > 0 ? ", " + corrupt + " corrupt entr" + (corrupt.get() == 1 ? "y" : "ies") + " ignored" : "")
                + ", " + (bytesSaved.get() / 1024) + " KiB of translated output reused";
    }

    static String hash(Path file) throws IOException {
        MessageDigest md = SplitPackageResolver.sha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static void deleteTree(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            List<Path> all = new ArrayList<>(s.toList());
            for (int i = all.size() - 1; i >= 0; i--) Files.deleteIfExists(all.get(i));
        } catch (IOException | RuntimeException ignored) {
            // a leftover .tmp- directory is harmless: readers only look at <key>/entry.txt
        }
    }
}
