package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForwardStageTest {
    private static final ForwardStage A = new ForwardStage("1.16.5", "1.18.2", "1.16.5", "p/a/");
    private static final ForwardStage B = new ForwardStage("1.18.2", "1.19.2", "1.18.2", "p/b/");
    private static final ForwardStage C = new ForwardStage("1.19.2", "1.20.1", "1.19.2", "p/c/");
    private static final ForwardStage D = new ForwardStage("1.20.1", "1.21.1", "1.20.1", "p/d/");

    @Test
    void chainStartsAtTheSourceVersionAndEndsAtTheHost() {
        List<ForwardStage> all = List.of(A, B, C, D);
        assertEquals(List.of(D), ForwardStage.chainFrom(all, "1.20.1"));
        assertEquals(List.of(C, D), ForwardStage.chainFrom(all, "1.19.2"));
        assertEquals(List.of(A, B, C, D), ForwardStage.chainFrom(all, "1.16.5"));
        assertTrue(ForwardStage.chainFrom(all, "1.12.2").isEmpty());
    }

    @Test
    void chainIsFollowedByTargetNotByListOrder() {
        // stages listed out of order still chain by target -> source; a gap ends the chain
        assertEquals(List.of(C, D), ForwardStage.chainFrom(List.of(C, D, A), "1.19.2"));
        assertEquals(List.of(A), ForwardStage.chainFrom(List.of(A, D), "1.16.5"));
    }

    @Test
    void registeredChainOf1201IsStageDOnly() {
        assertEquals(List.of(ForwardStage.D_1201), ForwardStage.chainFrom("1.20.1"));
        assertEquals(List.of(ForwardStage.C_1192, ForwardStage.D_1201), ForwardStage.chainFrom("1.19.2"));
    }

    @Test
    void renamesOfConsecutiveStagesCompose() {
        Map<String, String> c = Map.of("a/Old", "a/Mid", "a/Same", "a/Same2");
        Map<String, String> d = Map.of("a/Mid", "a/New", "a/Only", "a/OnlyNew", "a/Same2", "a/Final");
        Map<String, String> composed = ForwardStage.composeRenames(List.of(c, d));
        assertEquals("a/New", composed.get("a/Old"));       // two steps are one row
        assertEquals("a/Final", composed.get("a/Same"));
        assertEquals("a/OnlyNew", composed.get("a/Only"));  // a row of the later stage alone stays
        assertEquals("a/New", composed.get("a/Mid"));       // the intermediate name still maps (a 1.20.1 jar has it)
    }

    @Test
    void renameThatIsUndoneByALaterStageDisappears() {
        Map<String, String> composed = ForwardStage.composeRenames(List.of(Map.of("x/A", "x/B"), Map.of("x/B", "x/A")));
        assertFalse(composed.containsKey("x/A"));
        assertEquals("x/A", composed.get("x/B"));
    }

    @Test
    void singleStageComposesToItself() {
        Map<String, String> only = Map.of("x/A", "x/B");
        assertEquals(only, ForwardStage.composeRenames(List.of(only)));
    }

    @Test
    void tablesOfStageDAreFoundAndAnAbsentStageHasNone() {
        assertNotNull(ForwardStage.D_1201.open("mc-classes.tsv"));
        assertEquals("legacyconnector/forward/1.20.1/mc-classes.tsv", ForwardStage.D_1201.resource("mc-classes.tsv"));
        assertEquals(null, ForwardStage.C_1192.read("mc-classes.tsv"));
    }
}
