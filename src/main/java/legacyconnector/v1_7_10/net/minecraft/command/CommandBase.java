package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

public abstract class CommandBase implements ICommand {
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(getRequiredPermissionLevel(), getCommandName());
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    public static int parseInt(ICommandSender sender, String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", s);
        }
    }

    public static int parseIntWithMin(ICommandSender sender, String s, int min) {
        return parseIntBounded(sender, s, min, Integer.MAX_VALUE);
    }

    public static int parseIntBounded(ICommandSender sender, String s, int min, int max) {
        int v = parseInt(sender, s);
        if (v < min) throw new NumberInvalidException("commands.generic.num.tooSmall", v, min);
        if (v > max) throw new NumberInvalidException("commands.generic.num.tooBig", v, max);
        return v;
    }

    public static double parseDouble(ICommandSender sender, String s) {
        try {
            double d = Double.parseDouble(s);
            if (!Double.isFinite(d)) throw new NumberInvalidException("commands.generic.num.invalid", s);
            return d;
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", s);
        }
    }

    public static boolean parseBoolean(ICommandSender sender, String s) {
        if (s.equals("true") || s.equals("1")) return true;
        if (s.equals("false") || s.equals("0")) return false;
        throw new CommandException("commands.generic.boolean.invalid", s);
    }

    public static EntityPlayerMP getCommandSenderAsPlayer(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP p) return p;
        throw new PlayerNotFoundException("You must specify which player you wish to perform this action on.");
    }

    /** A player by selector ({@code @p}...) or name. */
    public static EntityPlayerMP getPlayer(ICommandSender sender, String name) {
        EntityPlayerMP p = PlayerSelector.matchOnePlayer(sender, name);
        if (p == null) p = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(name);
        if (p == null) throw new PlayerNotFoundException();
        return p;
    }

    /** The name a selector or name argument stands for (selectors that match nobody fail; other names pass as they are). */
    public static String getPlayerName(ICommandSender sender, String name) {
        EntityPlayerMP p = PlayerSelector.matchOnePlayer(sender, name);
        if (p != null) return p.getCommandSenderName();
        if (PlayerSelector.hasArguments(name)) throw new PlayerNotFoundException();
        return name;
    }

    public static double parseDoubleWithMin(ICommandSender sender, String s, double min) {
        return parseDoubleBounded(sender, s, min, Double.MAX_VALUE);
    }

    public static double parseDoubleBounded(ICommandSender sender, String s, double min, double max) {
        double v = parseDouble(sender, s);
        if (v < min) throw new NumberInvalidException("commands.generic.double.tooSmall", v, min);
        if (v > max) throw new NumberInvalidException("commands.generic.double.tooBig", v, max);
        return v;
    }

    /** A coordinate argument: absolute, or relative to {@code base} with {@code ~}; whole absolute numbers are block centres. */
    public static double clamp_coord(ICommandSender sender, double base, String s) {
        return clamp_double(sender, base, s, -30000000, 30000000);
    }

    /** {@link #clamp_coord} within {@code min}..{@code max} (no bounds when both are 0). */
    public static double clamp_double(ICommandSender sender, double base, String s, int min, int max) {
        boolean relative = s.startsWith("~");
        if (relative && Double.isNaN(base)) throw new NumberInvalidException("commands.generic.num.invalid", base);
        double value = relative ? base : 0.0D;
        if (!relative || s.length() > 1) {
            boolean decimal = s.contains(".");
            if (relative) s = s.substring(1);
            value += parseDouble(sender, s);
            if (!decimal && !relative) value += 0.5D;
        }
        if (min != 0 || max != 0) {
            if (value < min) throw new NumberInvalidException("commands.generic.double.tooSmall", value, min);
            if (value > max) throw new NumberInvalidException("commands.generic.double.tooBig", value, max);
        }
        return value;
    }

    /** The arguments from {@code index} on as one string. */
    public static String getStringFromNthArg(ICommandSender sender, String[] args, int index) {
        return func_82360_a(sender, args, index);
    }

    public static IChatComponent getChatComponentFromNthArg(ICommandSender sender, String[] args, int index) {
        return getChatComponentFromNthArg(sender, args, index, false);
    }

    /** The arguments from {@code index} on as a chat component; with {@code selectors}, player selectors become the names they match. */
    public static IChatComponent getChatComponentFromNthArg(ICommandSender sender, String[] args, int index, boolean selectors) {
        ChatComponentText text = new ChatComponentText("");
        for (int i = index; i < args.length; i++) {
            if (i > index) text.appendText(" ");
            IChatComponent part = new ChatComponentText(args[i]);
            if (selectors) {
                IChatComponent names = PlayerSelector.func_150869_b(sender, args[i]);
                if (names != null) part = names;
                else if (PlayerSelector.hasArguments(args[i])) throw new PlayerNotFoundException();
            }
            text.appendSibling(part);
        }
        return text;
    }

    public static String joinNiceStringFromCollection(Collection collection) {
        return joinNiceString(collection.toArray(new Object[0]));
    }

    /** Components joined as "a, b and c". */
    public static IChatComponent joinNiceString(IChatComponent[] parts) {
        ChatComponentText text = new ChatComponentText("");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                if (i == parts.length - 1) text.appendText(" and ");
                else text.appendText(", ");
            }
            text.appendSibling(parts[i]);
        }
        return text;
    }

    public static String func_82360_a(ICommandSender sender, String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public static IChatComponent func_147178_a(ICommandSender sender, String[] args, int start) {
        return new ChatComponentText(func_82360_a(sender, args, start));
    }

    public static String joinNiceString(Object[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(i == parts.length - 1 ? " and " : ", ");
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    public static boolean doesStringStartWith(String prefix, String s) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static List getListOfStringsMatchingLastWord(String[] args, String... options) {
        String last = args[args.length - 1];
        List<String> out = new ArrayList<>();
        for (String o : options) if (doesStringStartWith(last, o)) out.add(o);
        return out;
    }

    public static List getListOfStringsFromIterableMatchingLastWord(String[] args, Iterable options) {
        String last = args[args.length - 1];
        List<String> out = new ArrayList<>();
        for (Object o : options) if (doesStringStartWith(last, o.toString())) out.add(o.toString());
        return out;
    }

    public static void func_152373_a(ICommandSender sender, ICommand command, String message, Object... args) {
        notifyOperators(sender, command, 0, message, args);
    }

    @Override
    public int compareTo(Object o) {
        return getCommandName().compareTo(((ICommand) o).getCommandName());
    }

    public int compareTo(ICommand other) {
        return getCommandName().compareTo(other.getCommandName());
    }

    public static void notifyOperators(ICommandSender sender, ICommand command, String message, Object... args) {
        notifyOperators(sender, command, 0, message, args);
    }

    /**
     * Reports a command's result as the server's command manager did: the other operators online see it grey and
     * italic as "[sender: message]" (when command feedback is on), and the sender sees the message unless bit 1 of
     * {@code flags} is set.
     */
    public static void notifyOperators(ICommandSender sender, ICommand command, int flags, String message, Object... args) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            var said = new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation(message, args).mcc$toModern();
            var notice = net.minecraft.network.chat.Component.translatable("chat.type.admin", sender.getCommandSenderName(), said)
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC);
            String senderName = sender.getCommandSenderName();
            for (var p : server.getPlayerList().getPlayers()) {
                if (p.getGameProfile().getName().equals(senderName) || !server.getPlayerList().isOp(p.getGameProfile())) continue;
                p.sendSystemMessage(notice);
            }
        }
        if ((flags & 1) != 1) sender.addChatMessage(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation(message, args));
    }
}
