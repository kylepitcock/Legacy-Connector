package org.kylepitcock.legacyconnector.v1_20_1.migrate;

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
            // Palladium logs the Brigadier error position but drops the source line.
            // Preserve the original exception while recording enough context to
            // identify a legacy command that no longer parses under 1.21.1.
            LOGGER.error(
                    "Failed to parse legacy command function {}. Source command lines: {}",
                    id,
                    lines,
                    failure);
            throw failure;
        }
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
