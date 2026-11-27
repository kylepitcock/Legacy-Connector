package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression tests for the one obsolete optional AlienEvo effect-clear command. */
class CommandFunctionCompatTest {
    private static final String MISSING_FREEZE_EFFECT =
            "Whilst parsing command on line 4: Can't find element 'alienevo:freeze_effect' "
                    + "of type 'minecraft:mob_effect' at position 38";

    @Test
    void removesOnlyTheExactMissingFreezeEffectClearAndPreservesOtherCommands() {
        List<String> commands = List.of(
                "effect clear @s minecraft:poison",
                "effect give @s minecraft:water_breathing 1 255 true",
                "effect give @s minecraft:resistance 1 1 true",
                "effect clear @s alienevo:freeze_effect");

        assertEquals(List.of(
                        "effect clear @s minecraft:poison",
                        "effect give @s minecraft:water_breathing 1 255 true",
                        "effect give @s minecraft:resistance 1 1 true"),
                CommandFunctionCompat.withoutMissingAlienEvoFreezeEffectClear(
                        commands, MISSING_FREEZE_EFFECT));
    }

    @Test
    void doesNotRemoveARealEffectClearWhenTheFailureIsUnrelated() {
        List<String> commands = List.of(
                "effect clear @s minecraft:poison",
                "effect clear @s alienevo:freeze_effect");

        assertNull(CommandFunctionCompat.withoutMissingAlienEvoFreezeEffectClear(
                commands, "Unknown command at position 0"));
    }

    @Test
    void doesNotRemoveOtherUnknownEffectsOrApplyTheRuleToEffectGive() {
        List<String> commands = List.of(
                "effect give @s alienevo:freeze_effect 1 0 true",
                "effect clear @s alienevo:other_effect");

        assertNull(CommandFunctionCompat.withoutMissingAlienEvoFreezeEffectClear(
                commands, MISSING_FREEZE_EFFECT));
    }

    @Test
    void doesNotRetryWhenTheReportedEffectClearIsNotInTheFunction() {
        List<String> commands = List.of("effect clear @s minecraft:poison");

        assertNull(CommandFunctionCompat.withoutMissingAlienEvoFreezeEffectClear(
                commands, MISSING_FREEZE_EFFECT));
    }
}
