package org.kylepitcock.legacyconnector.core.pack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

/**
 * perf.packSpill: the content of a generated pack moves to one scratch file once it has been read. A generated pack lives as long as
 * the resource manager that opened it (the whole session, until the next reload), and its files are byte arrays the contributors made
 * eagerly (every block model, blockstate, recolored texture and translated recipe: tens of MB with large packs), read once by the
 * reload. After the first read an entry keeps only its offset in the file; a later read (a texture bound late, ModernFix's dynamic
 * model loading) reads it back from the file, which the OS cache usually still holds.
 *
 * <p>The file is unlinked as soon as it is open where the OS allows (it lives on through the handle), else it is deleted when the
 * handle closes; files left by a crashed process are removed on the next start. Any I/O failure leaves the entry in memory.
 */
final class PackSpill {
    /** Entries smaller than this stay in memory (the entry and its offset would cost about as much). */
    private static final int MIN_BYTES = 128;
    private static final Cleaner CLEANER = Cleaner.create();
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static volatile boolean disabled;
    private static boolean cleaned;

    private final String name;
    private final AtomicLong end = new AtomicLong();
    private volatile Handle handle;

    /** The open file, closed when the spill is unreachable (a pack may still be read after the resource manager closed it). */
    private static final class Handle implements Runnable {
        final FileChannel channel;

        Handle(FileChannel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                channel.close();
            } catch (IOException ignored) {
                // the OS removes it with the process
            }
        }
    }

    PackSpill(String name) {
        this.name = name;
    }

    /** Wraps a pack file's content: read from {@code source} the first time, from the scratch file after that. */
    Supplier<byte[]> wrap(Supplier<byte[]> source) {
        return disabled ? source : new Entry(source);
    }

    private static Path dir() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve(".legacyconnector").resolve("pack-spill");
    }

    /** Removes the scratch files of processes that no longer run (named {@code <pid>-<n>-<pack>.bin}). */
    private static synchronized void cleanStale(Path dir) {
        if (cleaned) return;
        cleaned = true;
        try (var files = Files.list(dir)) {
            files.forEach(f -> {
                String n = f.getFileName().toString();
                int dash = n.indexOf('-');
                try {
                    long pid = Long.parseLong(dash < 0 ? "" : n.substring(0, dash));
                    if (pid != ProcessHandle.current().pid() && ProcessHandle.of(pid).isEmpty()) Files.deleteIfExists(f);
                } catch (NumberFormatException | IOException | SecurityException ignored) {
                    // not ours, or in use
                }
            });
        } catch (IOException | RuntimeException ignored) {
            // nothing to clean
        }
    }

    private FileChannel channel() throws IOException {
        Handle h = handle;
        if (h != null) return h.channel;
        synchronized (this) {
            if (handle != null) return handle.channel;
            Path dir = dir();
            Files.createDirectories(dir);
            cleanStale(dir);
            Path file = dir.resolve(ProcessHandle.current().pid() + "-" + COUNTER.incrementAndGet() + "-" + name.replaceAll("[^A-Za-z0-9_]", "_") + ".bin");
            FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE,
                    StandardOpenOption.DELETE_ON_CLOSE);
            try {
                Files.deleteIfExists(file); // unlinked while open where the OS allows it; DELETE_ON_CLOSE otherwise
            } catch (IOException | SecurityException ignored) {
                // removed when the handle closes
            }
            h = new Handle(channel);
            CLEANER.register(this, h);
            handle = h;
            return channel;
        }
    }

    /** Appends the bytes; their offset, or -1 when the file cannot be written (the caller keeps them in memory). */
    private long write(byte[] bytes) {
        if (disabled) return -1;
        try {
            FileChannel channel = channel();
            long pos = end.getAndAdd(bytes.length);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer, pos + buffer.position());
            return pos;
        } catch (IOException | RuntimeException e) {
            disabled = true;
            LoggerFactory.getLogger("LegacyConnector/Pack").warn("Generated pack files stay in memory: the scratch file in {} cannot be written ({})", dir(), e.toString());
            return -1;
        }
    }

    private byte[] read(long pos, int length) throws IOException {
        byte[] out = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(out);
        FileChannel channel = channel();
        while (buffer.hasRemaining()) {
            if (channel.read(buffer, pos + buffer.position()) < 0) throw new IOException("generated pack scratch file ended early");
        }
        return out;
    }

    private final class Entry implements Supplier<byte[]> {
        /** The contributor's content until it has been written to the file. */
        private Supplier<byte[]> source;
        private long pos = -1;
        private int length;

        Entry(Supplier<byte[]> source) {
            this.source = source;
        }

        @Override
        public synchronized byte[] get() {
            if (source == null) {
                try {
                    return read(pos, length);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            byte[] bytes = source.get();
            if (bytes == null || bytes.length < MIN_BYTES) return bytes;
            long at = write(bytes);
            if (at >= 0) {
                pos = at;
                length = bytes.length;
                source = null; // the contributor's array is free now
            }
            return bytes;
        }
    }
}
