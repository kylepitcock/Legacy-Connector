package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.command.CommandException;
import legacyconnector.v1_7_10.net.minecraft.command.ICommand;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.LoggerFactory;

/**
 * Legacy {@code ICommand}s become Brigadier literals with one greedy string argument that is split on spaces,
 * which is exactly the {@code String[] args} the legacy command expects. Players who join afterwards receive
 * the updated command tree automatically.
 */
public final class CommandBridge1710 {
    private CommandBridge1710() {}

    /** The legacy commands of the running server: a data pack reload builds a new dispatcher, they are added to it again. */
    private static final Set<ICommand> REGISTERED = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());
    private static boolean listening;

    public static void register(net.minecraft.server.MinecraftServer server, ICommand command) {
        ensureListener();
        REGISTERED.add(command);
        addTo(server.getCommands().getDispatcher(), command);
        for (var p : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(p);
    }

    private static void addTo(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher, ICommand command) {
        dispatcher.register(build(command.getCommandName(), command));
        List<?> aliases = command.getCommandAliases();
        if (aliases != null) for (Object alias : aliases) dispatcher.register(build(String.valueOf(alias), command));
    }

    private static synchronized void ensureListener() {
        if (listening) return;
        listening = true;
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.neoforge.event.RegisterCommandsEvent.class, e -> {
            List<ICommand> all;
            synchronized (REGISTERED) {
                all = new java.util.ArrayList<>(REGISTERED);
            }
            for (ICommand c : all) {
                try {
                    addTo(e.getDispatcher(), c);
                } catch (RuntimeException | LinkageError ex) {
                    LoggerFactory.getLogger("LegacyConnector/Commands").warn("Could not register legacy command {} again: {}", c, ex.toString());
                }
            }
        });
        bus.addListener(net.neoforged.neoforge.event.server.ServerStoppedEvent.class, e -> REGISTERED.clear());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name, ICommand command) {
        return Commands.literal(name)
                .requires(src -> canUse(src, command))
                .executes(ctx -> run(ctx, command, new String[0]))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, command, StringArgumentType.getString(ctx, "args").trim().split(" +"))));
    }

    /** Players whose command tree is resent on the next tick, and players being resent right now (no second retry). */
    private static final Set<UUID> RESEND = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> RESENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    /**
     * 1.7.10 asked a command for permission only when a player typed it; 1.21 asks every command while it sends a
     * joining player the command tree, before that player is in the player list. Checks that look the sender up by
     * name there (WarpTheory) threw and the join failed with "Invalid player data". A failing check hides the command
     * and the player's tree is resent once on the next tick, when the player is listed.
     */
    private static boolean canUse(CommandSourceStack src, ICommand command) {
        try {
            return command.canCommandSenderUseCommand(sender(src));
        } catch (RuntimeException | LinkageError e) {
            if (REPORTED.add(command.getCommandName()))
                LoggerFactory.getLogger("LegacyConnector/Commands").warn("Permission check of legacy command /{} failed; hiding it for now",
                        command.getCommandName(), e);
            if (src.getEntity() instanceof ServerPlayer player && !RESENDING.contains(player.getUUID()) && RESEND.add(player.getUUID())) {
                var server = src.getServer();
                // tell() always queues (execute() would run it now, still inside placeNewPlayer)
                server.tell(new TickTask(server.getTickCount(), () -> {
                    UUID id = player.getUUID();
                    RESEND.remove(id);
                    if (server.getPlayerList().getPlayer(id) != player) return;
                    RESENDING.add(id);
                    try {
                        server.getCommands().sendCommands(player);
                    } finally {
                        RESENDING.remove(id);
                    }
                }));
            }
            return false;
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx, ICommand command, String[] args) {
        ICommandSender sender = sender(ctx.getSource());
        try {
            // like its world generators' state, a mod's commands ran on the server thread (CustomOreGen reloads its options)
            var lock = org.kylepitcock.legacyconnector.core.LegacyModLocks.of(command);
            lock.lock();
            try {
                command.processCommand(sender, args);
            } finally {
                lock.unlock();
            }
            return 1;
        } catch (CommandException e) {
            ctx.getSource().sendFailure(Component.translatable(e.getMessage(), e.getErrorOjbects()));
            return 0;
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Commands").error("Legacy command /{} failed", command.getCommandName(), e);
            ctx.getSource().sendFailure(Component.literal("An error occurred while executing legacy command: " + e));
            return 0;
        }
    }

    static ICommandSender sender(CommandSourceStack source) {
        if (source.getEntity() != null) {
            var view = EntityBridge.wrap(source.getEntity());
            if (view instanceof ICommandSender s) return s;
        }
        return MinecraftServer.mcc$wrap(source.getServer());
    }

    static boolean isPlayer(ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }
}
