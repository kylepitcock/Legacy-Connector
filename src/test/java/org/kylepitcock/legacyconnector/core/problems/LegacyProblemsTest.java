package org.kylepitcock.legacyconnector.core.problems;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.problems.LegacyProblems.Kind;

class LegacyProblemsTest {
    @BeforeEach
    @AfterEach
    void clear() {
        LegacyProblems.reset();
    }

    @Test
    void sameSubjectCountsOnceAndKindsAreSeparate() {
        LegacyProblems.record(Kind.MISSING_MODEL, "alpha", "alpha:block#inventory", "x");
        LegacyProblems.record(Kind.MISSING_MODEL, "alpha", "alpha:block#inventory", "again");
        LegacyProblems.record(Kind.MISSING_TEXTURE, "alpha", "alpha:block#inventory", "tex");
        assertEquals(1, LegacyProblems.count("alpha", Kind.MISSING_MODEL));
        assertEquals(1, LegacyProblems.count("alpha", Kind.MISSING_TEXTURE));
        assertEquals(2, LegacyProblems.total());
    }

    @Test
    void attributionIsPerNamespace() {
        LegacyProblems.record(Kind.NO_LANG_NAME, "alpha", "alpha:a", "");
        LegacyProblems.record(Kind.NO_LANG_NAME, "beta", "beta:a", "");
        LegacyProblems.record(Kind.NO_LANG_NAME, "beta", "beta:b", "");
        assertEquals(1, LegacyProblems.count("alpha", Kind.NO_LANG_NAME));
        assertEquals(2, LegacyProblems.count("beta", Kind.NO_LANG_NAME));
        assertEquals(0, LegacyProblems.count("gamma", Kind.NO_LANG_NAME));
    }

    @Test
    void capKeepsMemoryBoundedAndCountsOverflow() {
        int n = LegacyProblems.CAP + 25;
        for (int i = 0; i < n; i++) LegacyProblems.record(Kind.DROPPED_RECIPE, "alpha", "alpha:item" + i, "missing");
        assertEquals(n, LegacyProblems.count("alpha", Kind.DROPPED_RECIPE));
        List<String> lines = LegacyProblems.reportLines();
        long stored = lines.stream().filter(l -> l.startsWith("    dropped_recipe alpha:item")).count();
        assertEquals(LegacyProblems.CAP, stored);
        assertTrue(lines.contains("    dropped_recipe ...(+25 more)"), lines.toString());
        assertTrue(lines.contains("  problems alpha: missing_texture=0 missing_model=0 no_item_model=0 no_block_model=0 no_lang_name=0 dropped_recipe="
                + n + " nbt_failure=0 total=" + n));
    }

    @Test
    void namespacesPastTheLimitFallIntoOther() {
        for (int i = 0; i < LegacyProblems.MAX_MODS + 10; i++) LegacyProblems.record(Kind.NO_ITEM_MODEL, "ns" + i, "ns" + i + ":x", "");
        assertEquals(10, LegacyProblems.count(LegacyProblems.OTHER, Kind.NO_ITEM_MODEL));
    }

    @Test
    void concurrentRecordsOfOneSubjectCountOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) {
            pool.submit(() -> {
                for (int i = 0; i < 20; i++) LegacyProblems.record(Kind.MISSING_TEXTURE, "alpha", "alpha:m" + i, "");
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(20, LegacyProblems.count("alpha", Kind.MISSING_TEXTURE));
    }

    @Test
    void reportAndChatSummaries() {
        LegacyProblems.census("alpha", "blocks", 4);
        LegacyProblems.census("alpha", "blocks_with_model", 3);
        LegacyProblems.record(Kind.NO_BLOCK_MODEL, "alpha", "alpha:b", "");
        List<String> lines = LegacyProblems.reportLines();
        assertTrue(lines.get(0).startsWith(LegacyProblems.SECTION));
        assertTrue(lines.contains("  census alpha: blocks=4 blocks_with_model=3"), lines.toString());
        assertEquals("total: 1 problem(s) in 1 mod(s)", lines.get(lines.size() - 1));
        List<String> chat = LegacyProblems.chatLines(5);
        assertTrue(chat.get(0).contains("1 problem(s) in 1 mod(s)"));
        assertTrue(chat.get(1).contains("alpha") && chat.get(1).contains("1 block without a model"));
        assertTrue(chat.get(chat.size() - 1).startsWith("Full list: "));
    }

    @Test
    void anOldSectionIsReplacedNotDuplicated() {
        String head = "report head\n  mod x -> x\n";
        assertEquals(head, LegacyProblems.stripSection(head + "\n" + LegacyProblems.SECTION + " (written t)\n  problems a: total=1\n"));
        assertEquals(head, LegacyProblems.stripSection(head));
    }
}
