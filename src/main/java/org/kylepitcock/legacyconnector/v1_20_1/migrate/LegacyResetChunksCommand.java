package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Stands in for vanilla's {@code ResetChunksCommand} (1.20.1; a debug command that regenerates the chunks around the player, removed in 1.21.x).
 * A mod that registers it next to its own commands (Lost Cities) links; the command itself is NOT provided, so {@code /resetchunks} does not exist.
 */
public final class LegacyResetChunksCommand {
    private LegacyResetChunksCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // intentionally empty: regenerating chunks in place is not implemented
    }
}
