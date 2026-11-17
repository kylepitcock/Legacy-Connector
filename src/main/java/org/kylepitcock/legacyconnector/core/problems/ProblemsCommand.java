package org.kylepitcock.legacyconnector.core.problems;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /legacyconnector problems}: the summary of {@link LegacyProblems} in chat. Available to everyone (it only reports). The
 * {@code /legacyconnector} root is shared with the development commands (permission level 2 per subcommand), which register it too;
 * Brigadier merges the two registrations.
 */
final class ProblemsCommand {
    private ProblemsCommand() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(Commands.literal("legacyconnector")
                .then(Commands.literal("problems").executes(ctx -> {
                    for (String line : LegacyProblems.chatLines(12)) ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }))));
    }
}
