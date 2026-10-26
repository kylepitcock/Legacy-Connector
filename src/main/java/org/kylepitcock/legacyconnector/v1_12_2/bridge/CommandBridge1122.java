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
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 commands on the modern server: each command the server's {@link CommandHandler} registered becomes a Brigadier literal
 * (and one per alias) with one greedy argument split on spaces, which is the {@code String[] args} the legacy command expects;
 * permissions are the command's own {@code checkPermission}, tab completion its {@code getTabCompletions}. The commands are added
 * when the server's commands are built and, for later registrations, at once. Also answers the methods of the synthesized
 * {@code CommandBase} mods' commands extend (permission level 4, argument parsing, player lookups, feedback).
 */
public final class CommandBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Commands");
    private static boolean listening;
    /** The literals this bridge added, so a legacy command never takes over a modern command of the same name. */
    private static final Set<String> OURS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** The translation arguments of the command exceptions made here (the synthesized exception classes do not keep them). */
    private static final Map<Throwable, Object[]> ERROR_OBJECTS = Collections.synchronizedMap(new java.util.WeakHashMap<>());

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
        Object[] known = ERROR_OBJECTS.get(e);
        if (known != null) return known;
        Object o = RuntimeApis1122.invoke(e, "getErrorObjects");
        return o instanceof Object[] a ? a : new Object[0];
    }

    /** A legacy command exception ({@code CommandException} or a subclass) with its translation key and arguments. */
    private static Throwable commandException(String legacyClass, String key, Object... args) {
        Throwable t = null;
        Class<?> c = RuntimeApis1122.synthClass(legacyClass);
        if (c != null) {
            try {
                t = (Throwable) c.getConstructor(String.class, Object[].class).newInstance(key, args);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // below
            }
        }
        if (t == null) t = new IllegalArgumentException(new TextComponentTranslation(key, args).getUnformattedText());
        ERROR_OBJECTS.put(t, args);
        return t;
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

    // ------------------------------------------------------------------ CommandBase

    private static EntityPlayerMP player(String name) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || name == null) return null;
        var p = server.getPlayerList().getPlayerByName(name);
        if (p == null) {
            try {
                p = server.getPlayerList().getPlayer(java.util.UUID.fromString(name));
            } catch (IllegalArgumentException e) {
                // not a UUID
            }
        }
        return p == null ? null : EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
    }

    private static EntityPlayerMP senderPlayer(Object sender) {
        if (sender instanceof EntityPlayerMP p) return p;
        if (sender instanceof ICommandSender s && s.getCommandSenderEntity() instanceof EntityPlayerMP p) return p;
        return null;
    }

    private static List<EntityPlayerMP> allPlayers() {
        List<EntityPlayerMP> out = new ArrayList<>();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return out;
        for (var p : server.getPlayerList().getPlayers()) if (EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp) out.add(mp);
        return out;
    }

    /** A player named by an argument: a name, a UUID, or the selectors @p, @s and @r. */
    private static EntityPlayerMP findPlayer(Object sender, String name) throws Throwable {
        EntityPlayerMP p = null;
        if ("@s".equals(name) || "@p".equals(name)) p = senderPlayer(sender);
        if (p == null && ("@p".equals(name) || "@r".equals(name))) {
            List<EntityPlayerMP> all = allPlayers();
            if (!all.isEmpty()) p = all.get("@r".equals(name) ? java.util.concurrent.ThreadLocalRandom.current().nextInt(all.size()) : 0);
        }
        if (p == null) p = player(name);
        if (p == null) throw commandException("net.minecraft.command.PlayerNotFoundException", "commands.generic.player.notFound", name);
        return p;
    }

    private static int parseInt(String s) throws Throwable {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.invalid", s);
        }
    }

    private static long parseLong(String s) throws Throwable {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException | NullPointerException e) {
            throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.invalid", s);
        }
    }

    private static double parseDouble(String s) throws Throwable {
        try {
            double d = Double.parseDouble(s);
            if (!Double.isFinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException | NullPointerException e) {
            throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.invalid", s);
        }
    }

    private static Object bounded(double value, double min, double max, String smallKey, String bigKey, Object shown) throws Throwable {
        if (value < min) throw commandException("net.minecraft.command.NumberInvalidException", smallKey, shown, min);
        if (value > max) throw commandException("net.minecraft.command.NumberInvalidException", bigKey, shown, max);
        return null;
    }

    /** CommandBase.parseDouble(base, input, [min, max,] centerBlock): a coordinate, "~" relative to the base. */
    private static double coordinate(double base, String input, int min, int max, boolean centerBlock) throws Throwable {
        boolean relative = input.startsWith("~");
        if (relative && Double.isNaN(base)) throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.invalid", base);
        double d = relative ? base : 0.0D;
        if (!relative || input.length() > 1) {
            boolean decimal = input.contains(".");
            if (relative) input = input.substring(1);
            d += parseDouble(input);
            if (!decimal && !relative && centerBlock) d += 0.5D;
        }
        if (min != 0 || max != 0) {
            bounded(d, min, max, "commands.generic.double.tooSmall", "commands.generic.double.tooBig", String.format("%.2f", d));
        }
        return d;
    }

    private static List<String> matching(String[] args, Collection<?> options) {
        String last = args == null || args.length == 0 ? "" : args[args.length - 1];
        List<String> out = new ArrayList<>();
        if (options == null) return out;
        for (Object o : options) {
            String s = String.valueOf(o);
            if (s.regionMatches(true, 0, last, 0, last.length())) out.add(s);
        }
        return out;
    }

    private static String joinNice(Object[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(i == items.length - 1 ? " and " : ", ");
            sb.append(items[i]);
        }
        return sb.toString();
    }

    private static String build(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, from); args != null && i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static int permissionLevel(Object command) {
        Object level = RuntimeApis1122.invoke(command, "getRequiredPermissionLevel");
        return level instanceof Integer i ? i : 4;
    }

    public static void registerCommandBase() {
        String cb = "net.minecraft.command.CommandBase.";
        Synth.addInstanceMethod(cb + "getRequiredPermissionLevel", (self, args) -> 4);
        Synth.addInstanceMethod(cb + "getAliases", (self, args) -> Collections.emptyList());
        Synth.addInstanceMethod(cb + "getTabCompletions", (self, args) -> Collections.emptyList());
        Synth.addInstanceMethod(cb + "isUsernameIndex", (self, args) -> false);
        Synth.addInstanceMethod(cb + "checkPermission", (self, args) -> args.length > 1 && args[1] instanceof ICommandSender s
                && self instanceof ICommand c && s.canUseCommand(permissionLevel(self), c.getName()));
        Synth.addInstanceMethod(cb + "compareTo", (self, args) -> {
            if (!(self instanceof ICommand c) || args.length < 1 || !(args[0] instanceof ICommand other)) return Synth.NONE;
            return String.valueOf(c.getName()).compareTo(String.valueOf(other.getName()));
        });

        Synth.addStaticMethod(cb + "getCommandSenderAsPlayer", args -> {
            EntityPlayerMP p = args.length > 0 ? senderPlayer(args[0]) : null;
            if (p == null) throw commandException("net.minecraft.command.PlayerNotFoundException", "commands.generic.player.unspecified");
            return p;
        });
        Synth.addStaticMethod(cb + "getPlayer", args -> args.length > 2 ? findPlayer(args[1], (String) args[2]) : Synth.NONE);
        Synth.addStaticMethod(cb + "getPlayers", args -> {
            if (args.length < 3) return Synth.NONE;
            if ("@a".equals(args[2])) return new ArrayList<>(allPlayers());
            List<EntityPlayerMP> one = new ArrayList<>();
            one.add(findPlayer(args[1], (String) args[2]));
            return one;
        });
        Synth.addStaticMethod(cb + "getEntity", args -> {
            if (args.length < 3) return Synth.NONE;
            String name = (String) args[2];
            Entity found = null;
            if ("@s".equals(name) && args[1] instanceof ICommandSender s) found = s.getCommandSenderEntity();
            if (found == null) found = player(name);
            if (found == null) {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                java.util.UUID id = null;
                try {
                    id = java.util.UUID.fromString(name);
                } catch (IllegalArgumentException | NullPointerException e) {
                    // not a UUID
                }
                if (server != null && id != null) {
                    for (var level : server.getAllLevels()) {
                        var e = level.getEntity(id);
                        if (e != null) {
                            found = EntityBridge1122.wrap(e);
                            break;
                        }
                    }
                }
            }
            if (found != null && args.length > 3 && args[3] instanceof Class<?> type && !type.isInstance(found)) found = null;
            if (found == null) throw commandException("net.minecraft.command.EntityNotFoundException", "commands.generic.entity.notFound", name);
            return found;
        });
        Synth.addStaticMethod(cb + "getPlayerName", args -> args.length > 2 ? findPlayer(args[1], (String) args[2]).getName() : Synth.NONE);
        Synth.addStaticMethod(cb + "getEntityName", args -> args.length > 2 ? args[2] : Synth.NONE);

        Synth.addStaticMethod(cb + "parseInt", args -> {
            int i = parseInt((String) args[0]);
            int min = args.length > 1 ? Synth.toInt(args[1]) : Integer.MIN_VALUE;
            int max = args.length > 2 ? Synth.toInt(args[2]) : Integer.MAX_VALUE;
            bounded(i, min, max, "commands.generic.num.tooSmall", "commands.generic.num.tooBig", i);
            return i;
        });
        Synth.addStaticMethod(cb + "parseLong", args -> {
            long l = parseLong((String) args[0]);
            if (args.length > 2) {
                long min = Synth.toLong(args[1]), max = Synth.toLong(args[2]);
                if (l < min) throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.tooSmall", l, min);
                if (l > max) throw commandException("net.minecraft.command.NumberInvalidException", "commands.generic.num.tooBig", l, max);
            }
            return l;
        });
        Synth.addStaticMethod(cb + "parseDouble", args -> {
            if (args.length > 0 && args[0] instanceof Double base) {
                // coordinate forms: (base, input, centerBlock) and (base, input, min, max, centerBlock)
                if (args.length == 3) return coordinate(base, (String) args[1], 0, 0, Synth.toBoolean(args[2]));
                if (args.length == 5) return coordinate(base, (String) args[1], Synth.toInt(args[2]), Synth.toInt(args[3]), Synth.toBoolean(args[4]));
                return Synth.NONE;
            }
            double d = parseDouble((String) args[0]);
            double min = args.length > 1 ? Synth.toDouble(args[1]) : -Double.MAX_VALUE;
            double max = args.length > 2 ? Synth.toDouble(args[2]) : Double.MAX_VALUE;
            bounded(d, min, max, "commands.generic.double.tooSmall", "commands.generic.double.tooBig", String.format("%.2f", d));
            return d;
        });
        Synth.addStaticMethod(cb + "parseBoolean", args -> {
            String s = (String) args[0];
            if ("true".equals(s) || "1".equals(s)) return true;
            if ("false".equals(s) || "0".equals(s)) return false;
            throw commandException("net.minecraft.command.CommandException", "commands.generic.boolean.invalid", s);
        });
        Synth.addStaticMethod(cb + "buildString", args -> build((String[]) args[0], Synth.toInt(args[1])));
        Synth.addStaticMethod(cb + "getChatComponentFromNthArg", args -> new TextComponentString(build((String[]) args[1], Synth.toInt(args[2]))));
        Synth.addStaticMethod(cb + "doesStringStartWith", args -> {
            String original = (String) args[0], region = (String) args[1];
            return original != null && region != null && region.regionMatches(true, 0, original, 0, original.length());
        });
        Synth.addStaticMethod(cb + "getListOfStringsMatchingLastWord", args -> {
            String[] typed = (String[]) args[0];
            if (args.length > 1 && args[1] instanceof Collection<?> c) return matching(typed, c);
            if (args.length > 1 && args[1] instanceof Object[] a) return matching(typed, java.util.Arrays.asList(a));
            return new ArrayList<String>();
        });
        Synth.addStaticMethod(cb + "joinNiceString", args -> args.length > 0 && args[0] instanceof Object[] a ? joinNice(a) : "");
        Synth.addStaticMethod(cb + "joinNiceStringFromCollection", args -> args.length > 0 && args[0] instanceof Collection<?> c ? joinNice(c.toArray()) : "");
        // notifyCommandListener(sender, command, [flags,] key, args...): the command's feedback to its sender (flag 1 hides it)
        Synth.addStaticMethod(cb + "notifyCommandListener", args -> {
            if (args.length < 4 || !(args[0] instanceof ICommandSender sender)) return null;
            boolean flagged = args.length == 5 && args[2] instanceof Integer;
            int flags = flagged ? (Integer) args[2] : 0;
            String key = (String) args[flagged ? 3 : 2];
            Object[] params = args[flagged ? 4 : 3] instanceof Object[] a ? a : new Object[0];
            if ((flags & 1) != 1) sender.sendMessage(new TextComponentTranslation(key, params));
            return null;
        });

        // CommandException.getErrorObjects for the exceptions made here
        Synth.addInstanceMethod("net.minecraft.command.CommandException.getErrorObjects", (self, args) -> {
            Object[] known = self instanceof Throwable t ? ERROR_OBJECTS.get(t) : null;
            return known != null ? known : Synth.NONE;
        });
    }
}
