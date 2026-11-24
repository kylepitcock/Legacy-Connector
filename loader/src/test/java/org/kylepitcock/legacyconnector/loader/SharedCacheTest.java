package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The shared translation cache: the key, a publish/fetch round trip, a corrupt entry, racing publishers and the copy fallback. */
class SharedCacheTest {
    @TempDir
    Path dir;

    private Map<String, Path> translation(Path where, String name, String body) throws IOException {
        Files.createDirectories(where);
        Path out = where.resolve(name + ".jar");
        Files.writeString(out, body);
        Files.writeString(where.resolve(name + ".jar.synth.txt"), "synth");
        Files.writeString(where.resolve(name + ".jar.links.txt"), "0 unresolved");
        return LegacyModLocator.sharedParts(out);
    }

    private LegacyJar jar(String file, String content) throws Exception {
        Path p = dir.resolve(file);
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        return new LegacyJar(p);
    }

    private static String key(LegacyJar jar, byte[] modSet) throws Exception {
        Method m = LegacyModLocator.class.getDeclaredMethod("sharedKey", LegacyJar.class, byte[].class);
        m.setAccessible(true);
        return (String) m.invoke(null, jar, modSet);
    }

    @Test
    void theKeyIsTheFullDigestAndFollowsTheModSet() throws Exception {
        LegacyJar j = jar("mod.jar", "abc");
        String a = key(j, new byte[] {1});
        assertTrue(a.matches("mod-[0-9a-f]{64}"), a);
        assertEquals(a, key(j, new byte[] {1}));
        assertNotEquals(a, key(j, new byte[] {2}));
        assertNotEquals(a, key(jar("mod.jar", "abcd"), new byte[] {1}));
    }

    @Test
    void publishThenFetchRestoresEveryPart() throws Exception {
        SharedCache c = new SharedCache(dir.resolve("shared"));
        Map<String, Path> src = translation(dir.resolve("a"), "m", "translated bytes");
        assertTrue(c.publish("k1", src));
        assertTrue(c.has("k1"));
        Files.createDirectories(dir.resolve("b"));
        Map<String, Path> dst = LegacyModLocator.sharedParts(dir.resolve("b").resolve("m.jar"));
        assertTrue(c.fetch("k1", dst));
        assertEquals("translated bytes", Files.readString(dst.get("jar")));
        assertEquals("synth", Files.readString(dst.get("synth")));
        assertEquals("0 unresolved", Files.readString(dst.get("links")));
        assertFalse(Files.exists(dst.get("risk")), "an absent optional part stays absent");
        assertEquals(1, c.hits.get());
        assertEquals(1, c.published.get());
        assertFalse(c.publish("k1", src), "an entry is never overwritten");
        assertFalse(c.fetch("nope", dst));
        assertEquals(1, c.misses.get());
    }

    @Test
    void aCorruptEntryIsIgnored() throws Exception {
        SharedCache c = new SharedCache(dir.resolve("shared"));
        assertTrue(c.publish("k1", translation(dir.resolve("a"), "m", "good")));
        Files.writeString(dir.resolve("shared").resolve("k1").resolve("jar"), "evil"); // same size, other content
        Files.createDirectories(dir.resolve("b"));
        Map<String, Path> dst = LegacyModLocator.sharedParts(dir.resolve("b").resolve("m.jar"));
        assertFalse(c.fetch("k1", dst));
        assertEquals(1, c.corrupt.get());
        assertFalse(Files.exists(dst.get("jar")));
        Files.writeString(dir.resolve("shared").resolve("k1").resolve("jar"), "goo"); // other size
        assertFalse(c.fetch("k1", dst));
        assertEquals(2, c.corrupt.get());
    }

    @Test
    void racingPublishersLeaveOneCompleteEntry() throws Exception {
        SharedCache c = new SharedCache(dir.resolve("shared"));
        int n = 8;
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Path> src = translation(dir.resolve("w" + i), "m", "bytes of worker " + i);
            Thread t = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException ignored) {
                }
                if (new SharedCache(dir.resolve("shared")).publish("k", src)) wins.incrementAndGet();
            });
            ts.add(t);
            t.start();
        }
        go.countDown();
        for (Thread t : ts) t.join();
        assertEquals(1, wins.get());
        Files.createDirectories(dir.resolve("b"));
        Map<String, Path> dst = LegacyModLocator.sharedParts(dir.resolve("b").resolve("m.jar"));
        assertTrue(c.fetch("k", dst));
        assertTrue(Files.readString(dst.get("jar")).startsWith("bytes of worker "));
        try (var s = Files.list(dir.resolve("shared"))) {
            assertEquals(List.of("k"), s.map(p -> p.getFileName().toString()).toList(), "no temporary directory is left behind");
        }
    }

    @Test
    void fallsBackToCopyWhenLinkingFails() throws Exception {
        AtomicInteger tried = new AtomicInteger();
        SharedCache c = new SharedCache(dir.resolve("shared"), (link, existing) -> {
            tried.incrementAndGet();
            throw new IOException("cross-device");
        });
        assertTrue(c.publish("k", translation(dir.resolve("a"), "m", "x")));
        Files.createDirectories(dir.resolve("b"));
        Map<String, Path> dst = LegacyModLocator.sharedParts(dir.resolve("b").resolve("m.jar"));
        assertTrue(c.fetch("k", dst));
        assertTrue(tried.get() > 0);
        assertEquals("x", Files.readString(dst.get("jar")));
        try (var s = Files.list(dir.resolve("b"))) {
            assertEquals(3, s.count(), "no temporary file is left behind");
        }
    }
}
