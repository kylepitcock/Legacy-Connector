package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 commands on the modern server: each command the server's {@link CommandHandler} registered becomes a Brigadier literal
 * (and one per alias) with one greedy argument split on spaces, which is the {@code String[] args} the legacy command expects;
 * permissions are the command's own {@code checkPermission}, tab completion its {@code getTabCompletions}. The commands are added
 * when the server's commands are built and, for later registrations, at once.
 */
public final class CommandBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Commands");
    private static boolean listening;
    /** The literals this bridge added, so a legacy command never takes over a modern command of the same name. */
    private static final Set<String> OURS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private CommandBridge1122() {}

    static {
        // 1.12's own command messages, which the modern language files do not have
        Map<String, String> en = org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations.table("en_us");
        String[][] keys = {
                {"commands.generic.usage", "Usage: %s"},
                {"commands.generic.notFound", "Unknown command. Try /help for a list of commands"},
                {"commands.generic.permission", "You do not have permission to use this command"},
                {"commands.generic.exception", "An unknown error occurred while attempting to perform this command"},
                {"commands.generic.player.notFound", "Player '%s' cannot be found"},
                {"commands.generic.player.unspecified", "You must specify which player you wish to perform this command on."},
                {"commands.generic.entity.notFound", "Entity '%s' cannot be found"},
                {"commands.generic.num.invalid", "'%s' is not a valid number"},
                {"commands.generic.num.tooSmall", "The number you have entered (%s) is too small, it must be at least %s"},
                {"commands.generic.num.tooBig", "The number you have entered (%s) is too big, it must be at most %s"},
                {"commands.generic.boolean.invalid", "'%s' is not true or false"},
                {"commands.generic.double.tooSmall", "The number you have entered (%s) is too small, it must be at least %s"},
                {"commands.generic.double.tooBig", "The number you have entered (%s) is too big, it must be at most %s"}};
        for (String[] k : keys) en.putIfAbsent(k[0], k[1]);
    }

    // ------------------------------------------------------------------ registration

    /** A command a handler registered: the server's commands become modern commands. */
    public static void registered(CommandHandler handler, ICommand command) {
        if (handler != MinecraftServer.mcc$instance().getCommandManager()) return; // the client's handler and mods' own ones only keep it
        ensureListener();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> {
            register(server.getCommands().getDispatcher(), command);
            for (var p : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(p);
        });
    }

    private static synchronized void ensureListener() {
        if (listening) return;
        listening = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.RegisterCommandsEvent.class, e -> {
            OURS.clear();
            if (!(MinecraftServer.mcc$instance().getCommandManager() instanceof CommandHandler handler)) return;
            for (ICommand c : handler.mcc$commands()) register(e.getDispatcher(), c);
        });
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, ICommand command) {
        List<String> names = new ArrayList<>();
        try {
            names.add(command.getName());
            List<String> aliases = command.getAliases();
            if (aliases != null) names.addAll(aliases);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Cannot read the name of legacy command {}: {}", command.getClass().getName(), e.toString());
        }
        for (String name : names) {
            if (name == null || name.isEmpty() || name.indexOf(' ') >= 0) continue;
            if (dispatcher.getRoot().getChild(name) != null && !OURS.contains(name)) {
                LOG.info("Legacy command /{} ({}) is not added: a modern command has that name", name, command.getClass().getName());
                continue;
            }
            OURS.add(name);
            dispatcher.register(literal(name, command));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name, ICommand command) {
        return Commands.literal(name)
                .requires(src -> allowed(src, command))
                .executes(ctx -> run(ctx, command, new String[0]))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining();
                            String[] args = remaining.split(" ", -1);
                            var sub = builder.createOffset(builder.getStart() + remaining.lastIndexOf(' ') + 1);
                            try {
                                ICommandSender sender = sender(ctx.getSource());
                                List<String> completions = command.getTabCompletions(MinecraftServer.mcc$instance(), sender, args, sender.getPosition());
                                if (completions != null) for (String s : completions) sub.suggest(s);
                            } catch (RuntimeException | LinkageError e) {
                                // no suggestions
                            }
                            return sub.buildFuture();
                        })
                        .executes(ctx -> run(ctx, command, StringArgumentType.getString(ctx, "args").trim().split(" +"))));
    }

    private static boolean allowed(CommandSourceStack src, ICommand command) {
        try {
            return command.checkPermission(MinecraftServer.mcc$instance(), sender(src));
        } catch (RuntimeException | LinkageError e) {
            return src.hasPermission(4);
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx, ICommand command, String[] args) {
        ICommandSender sender = sender(ctx.getSource());
        var lock = org.kylepitcock.legacyconnector.core.LegacyModLocks.of(command);
        lock.lock();
        try {
            command.execute(MinecraftServer.mcc$instance(), sender, args);
            return 1;
        } catch (Exception | LinkageError e) {
            if (e instanceof RuntimeException || e instanceof LinkageError) {
                LOG.error("Legacy command /{} failed", ctx.getInput(), e);
            }
            ctx.getSource().sendFailure(errorText(e).mcc$modern());
            return 0;
        } finally {
            lock.unlock();
        }
    }

    /** What 1.12's command handler told the sender about a failed command (the usage, the command's error, or a generic error). */
    public static ITextComponent errorText(Throwable e) {
        TextComponentTranslation text;
        if (isLegacy(e, "net.minecraft.command.WrongUsageException")) {
            text = new TextComponentTranslation("commands.generic.usage", new TextComponentTranslation(String.valueOf(e.getMessage()), errorObjects(e)));
        } else if (isLegacy(e, "net.minecraft.command.CommandException")) {
            text = new TextComponentTranslation(String.valueOf(e.getMessage()), errorObjects(e));
        } else {
            text = new TextComponentTranslation("commands.generic.exception");
        }
        text.getStyle().setColor(TextFormatting.RED);
        return text;
    }

    private static boolean isLegacy(Throwable e, String legacyClass) {
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            if (n.equals("legacyconnector_synth.v1_12_2." + legacyClass) || n.equals("legacyconnector.v1_12_2." + legacyClass)) return true;
        }
        return false;
    }

    private static Object[] errorObjects(Throwable e) {
        return e instanceof legacyconnector.v1_12_2.net.minecraft.command.CommandException c ? c.getErrorObjects() : new Object[0];
    }

    // ------------------------------------------------------------------ senders

    static ICommandSender sender(CommandSourceStack source) {
        if (source.getEntity() != null) {
            Entity view = EntityBridge1122.wrap(source.getEntity());
            if (view != null) return view;
        }
        return new SourceSender(source);
    }

    /** The console, a command block or another source without an entity. */
    private record SourceSender(CommandSourceStack source) implements ICommandSender {
        @Override
        public String getName() {
            return source.getTextName();
        }

        @Override
        public void sendMessage(ITextComponent component) {
            if (component != null) source.sendSystemMessage(component.mcc$modern());
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return source.hasPermission(permLevel);
        }

        @Override
        public BlockPos getPosition() {
            var p = source.getPosition();
            return new BlockPos(p.x, p.y, p.z);
        }

        @Override
        public Vec3d getPositionVector() {
            var p = source.getPosition();
            return new Vec3d(p.x, p.y, p.z);
        }

        @Override
        public World getEntityWorld() {
            return WorldBridge1122.wrap(source.getLevel());
        }

        @Override
        public boolean sendCommandFeedback() {
            return true;
        }

        @Override
        public MinecraftServer getServer() {
            return MinecraftServer.mcc$instance();
        }
    }
}
