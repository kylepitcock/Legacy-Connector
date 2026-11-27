package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import org.slf4j.Logger;

/**
 * 1.20.1 command-function calls whose signatures changed in 1.21.1.
 */
public final class CommandFunctionCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MISSING_FREEZE_EFFECT =
            "Can't find element 'alienevo:freeze_effect' of type 'minecraft:mob_effect'";
    private static final String LEGACY_FREEZE_EFFECT_CLEAR =
            "effect clear @s alienevo:freeze_effect";

    private CommandFunctionCompat() {}

    /**
     * 1.20.1 accepted a concrete CommandSourceStack; 1.21.1's generic
     * CommandFunction.fromLines erases its source parameter to ExecutionCommandSource.
     * The type bound is satisfied by CommandSourceStack, so keep the old call surface.
     */
    public static CommandFunction<CommandSourceStack> fromLines(
            ResourceLocation id,
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            List<String> lines) {
        try {
            return CommandFunction.fromLines(id, dispatcher, source, lines);
        } catch (IllegalArgumentException failure) {
            // AlienEvo dev.24 still supplies a cleanup for its removed freeze effect.
            // Clearing an effect that is not registered is a no-op; 1.21.1 rejects the
            // entire function before execution, so omit only that exact obsolete clear
            // command and retry. Do not suppress other missing effects or command errors.
            List<String> retryLines = withoutMissingAlienEvoFreezeEffectClear(lines, failure.getMessage());
            List<String> reportedLines = lines;
            IllegalArgumentException reportedFailure = failure;
            if (retryLines != null) {
                LOGGER.warn(
                        "Skipping obsolete effect clear for alienevo:freeze_effect in legacy command function {} "
                                + "because that mob effect is not registered",
                        id);
                try {
                    return CommandFunction.fromLines(id, dispatcher, source, retryLines);
                } catch (IllegalArgumentException retryFailure) {
                    retryFailure.addSuppressed(failure);
                    reportedLines = retryLines;
                    reportedFailure = retryFailure;
                }
            }

            // Palladium logs the Brigadier error position but drops the source line.
            // Preserve the parser failure and record enough context to identify any
            // remaining legacy command that no longer parses under 1.21.1.
            LOGGER.error(
                    "Failed to parse legacy command function {}. Source command lines: {}",
                    id,
                    reportedLines,
                    reportedFailure);
            throw reportedFailure;
        }
    }

    /**
     * Returns a retry copy only when the parser specifically reports that the
     * registered mob-effect lookup rejected alienevo:freeze_effect and the
     * source contains its exact legacy clear command. A clear against an absent
     * effect has no runtime effect, so dropping that one line preserves the
     * behavior of every command the current registry can execute.
     *
     * Package-private for a pure regression test; a null result means do not retry.
     */
    static List<String> withoutMissingAlienEvoFreezeEffectClear(List<String> lines, String parseError) {
        if (lines == null || parseError == null || !parseError.contains(MISSING_FREEZE_EFFECT)) {
            return null;
        }

        List<String> retry = new ArrayList<>(lines);
        boolean removed = retry.removeIf(line ->
                line != null && LEGACY_FREEZE_EFFECT_CLEAR.equals(line.trim()));
        return removed ? List.copyOf(retry) : null;
    }

    /**
     * 1.20.1 executed functions synchronously and returned a command count.
     * 1.21.1 queues the work and returns void. Legacy callers that use the old
     * integer result are not rewritten; the transformer only selects this helper
     * when that result is immediately discarded.
     */
    public static void execute(
            ServerFunctionManager manager,
            CommandFunction<CommandSourceStack> function,
            CommandSourceStack source) {
        manager.execute(function, source);
    }
}
