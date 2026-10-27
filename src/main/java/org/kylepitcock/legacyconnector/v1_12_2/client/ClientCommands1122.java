package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraftforge.client.ClientCommandHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** The commands mods registered with Forge's {@link ClientCommandHandler} become the game's client commands (run by the local player). */
public final class ClientCommands1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Commands");

    private ClientCommands1122() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientCommands1122::onRegister);
    }

    private static void onRegister(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e) {
        if (!Client1122.present()) return;
        for (ICommand command : ClientCommandHandler.instance.mcc$commands()) {
            java.util.List<String> names = new java.util.ArrayList<>();
            try {
                names.add(command.getName());
                if (command.getAliases() != null) names.addAll(command.getAliases());
            } catch (RuntimeException | LinkageError ex) {
                continue;
            }
            for (String name : names) {
                if (name == null || name.isEmpty() || name.indexOf(' ') >= 0 || e.getDispatcher().getRoot().getChild(name) != null) continue;
                e.getDispatcher().register(Commands.literal(name)
                        .executes(ctx -> run(ctx, command, new String[0]))
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining();
                                    var sub = builder.createOffset(builder.getStart() + remaining.lastIndexOf(' ') + 1);
                                    var sender = sender();
                                    if (sender != null) {
                                        try {
                                            var list = command.getTabCompletions(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance(), sender,
                                                    remaining.split(" ", -1), sender.getPosition());
                                            if (list != null) for (String s : list) sub.suggest(s);
                                        } catch (RuntimeException | LinkageError ex) {
                                            // no suggestions
                                        }
                                    }
                                    return sub.buildFuture();
                                })
                                .executes(ctx -> run(ctx, command, StringArgumentType.getString(ctx, "args").trim().split(" +")))));
            }
        }
    }

    private static legacyconnector.v1_12_2.net.minecraft.client.entity.EntityPlayerSP sender() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
    }

    private static int run(CommandContext<CommandSourceStack> ctx, ICommand command, String[] args) {
        var sender = sender();
        if (sender == null) return 0;
        try {
            command.execute(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance(), sender, args);
            return 1;
        } catch (Exception | LinkageError e) {
            if (e instanceof RuntimeException || e instanceof LinkageError) LOG.error("Legacy client command /{} failed", ctx.getInput(), e);
            ctx.getSource().sendFailure(org.kylepitcock.legacyconnector.v1_12_2.bridge.CommandBridge1122.errorText(e).mcc$modern());
            return 0;
        }
    }
}
