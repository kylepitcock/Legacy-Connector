package org.kylepitcock.legacyconnector.core.degraded;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.core.degraded.Degraded.Category;

class DegradedTest {
    @BeforeEach
    @AfterEach
    void clear() {
        Degraded.reset();
        Degraded.setQuiet(true);
    }

    @Test
    void countsEveryOccurrenceButKeepsOneEntryPerKey() {
        for (int i = 0; i < 5; i++) Degraded.note(Category.GL_EMULATION, "modA", "glGetInteger(0x1234)", "read the real context");
        Degraded.note(Category.GL_EMULATION, "modA", "glGetFloat(0x1)", "read the real context");
        assertEquals(5, Degraded.count(Category.GL_EMULATION, "modA", "glGetInteger(0x1234)"));
        assertEquals(1, Degraded.count(Category.GL_EMULATION, "modA", "glGetFloat(0x1)"));
        assertEquals(2, Degraded.distinct(Category.GL_EMULATION, "modA"));
        assertEquals(2, Degraded.totalEvents());
        assertEquals(0, Degraded.count(Category.MIXIN, "modA", "glGetFloat(0x1)"));
    }

    @Test
    void messageIsBuiltOnlyTheFirstTime() {
        AtomicInteger built = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            Degraded.note(Category.RENDER_TYPE, "m", "type 26", () -> {
                built.incrementAndGet();
                return "no handler";
            });
        }
        assertEquals(1, built.get());
        assertEquals(10, Degraded.count(Category.RENDER_TYPE, "m", "type 26"));
        assertTrue(Degraded.seen(Category.RENDER_TYPE, "m", "type 26"));
        assertFalse(Degraded.seen(Category.RENDER_TYPE, "m", "type 27"));
    }

    @Test
    void keysPerOwnerAreBoundedAndOverflowIsCounted() {
        for (int i = 0; i < Degraded.MAX_KEYS_PER_OWNER + 30; i++) Degraded.note(Category.ASSET_CAP, "big", "k" + i, "m");
        assertEquals(Degraded.MAX_KEYS_PER_OWNER, Degraded.distinct(Category.ASSET_CAP, "big"));
        assertEquals(Degraded.MAX_KEYS_PER_OWNER + 30, Degraded.totalEvents());
        List<String> lines = Degraded.summaryLines(3);
        assertTrue(lines.stream().anyMatch(l -> l.contains("230 distinct")), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.contains("(+227 more)")), lines.toString());
    }

    @Test
    void summaryGroupsByOwnerThenCategory() {
        Degraded.note(Category.GL_EMULATION, "zeta", "a", "one");
        Degraded.note(Category.GL_EMULATION, "zeta", "a", "one");
        Degraded.note(Category.RENDER_TYPE, "zeta", "type 7", "two");
        Degraded.note(Category.MIXIN, "alpha", "SomeMixin", "three");
        List<String> lines = Degraded.summaryLines(5);
        int alpha = lines.indexOf("  alpha");
        int zeta = lines.indexOf("  zeta");
        assertTrue(alpha > 0 && zeta > alpha, lines.toString());
        assertTrue(lines.get(0).contains("3 distinct event(s) in 2 place(s)"), lines.get(0));
        assertTrue(lines.contains("    " + Category.GL_EMULATION.label + ": 1 distinct, 2 time(s)"), lines.toString());
        assertTrue(lines.contains("    " + Category.RENDER_TYPE.label + ": 1 distinct, 1 time(s)"), lines.toString());
        assertTrue(lines.contains("      a x2: one"), lines.toString());
        assertTrue(Degraded.summaryLines(5).size() == lines.size());
    }

    @Test
    void emptyChannelHasNoSummary() {
        assertTrue(Degraded.summaryLines(5).isEmpty());
        assertEquals(0, Degraded.totalEvents());
    }

    @Test
    void threadsCountExactly() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) {
            pool.submit(() -> {
                for (int i = 0; i < 1000; i++) Degraded.note(Category.OTHER, "mt", "same", "m");
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        assertEquals(8000, Degraded.count(Category.OTHER, "mt", "same"));
        assertEquals(1, Degraded.distinct(Category.OTHER, "mt"));
    }

    @Test
    void nullOwnerGoesToOtherAndNullKeyIsIgnored() {
        Degraded.note(Category.OTHER, null, "k", "m");
        Degraded.note(Category.OTHER, "x", null, "m");
        assertEquals(1, Degraded.count(Category.OTHER, Degraded.OTHER, "k"));
        assertEquals(1, Degraded.totalEvents());
    }

    @Test
    void finishWritesTheFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("logs").resolve(Degraded.FILE);
        Degraded.setFileForTests(file);
        Degraded.note(Category.MIXIN, "alpha", "M", "not applied");
        Degraded.finish("test");
        String text = Files.readString(file);
        assertTrue(text.contains("alpha") && text.contains("not applied"), text);
    }

    @Test
    void legacyCallReturnsFallbackAndNotesOncePerSiteAndClass() {
        Object legacy = new Object();
        for (int i = 0; i < 4; i++) {
            int v = LegacyCall.get("getLightBlock", legacy, 7, () -> {
                throw new IllegalStateException("boom");
            });
            assertEquals(7, v);
        }
        assertEquals(1, Degraded.distinct(Category.LEGACY_CALL, DegradedOwners.of(Object.class)));
        long n = Degraded.count(Category.LEGACY_CALL, DegradedOwners.of(Object.class), "getLightBlock of java.lang.Object");
        assertEquals(4, n);
        assertEquals("fine", LegacyCall.get("x", legacy, "fallback", () -> "fine"));
        assertEquals(1, Degraded.totalEvents());
    }

    @Test
    void failedInACatchBlockKeepsTheCallersFallback() {
        float f;
        try {
            throw new LinkageError("missing class");
        } catch (RuntimeException | LinkageError e) {
            f = LegacyCall.failed("getAmbientOcclusionLightValue", this, e, 0.5F);
        }
        assertEquals(0.5F, f);
        assertEquals(1, Degraded.totalEvents());
    }

    @Test
    void classNamesReduceToPackageRoot() {
        assertEquals("a.b.c", DegradedOwners.ofClassName("a.b.c.d.E"));
        assertEquals("E", DegradedOwners.ofClassName("E"));
        assertEquals(Degraded.OTHER, DegradedOwners.of((Class<?>) null));
    }
}
