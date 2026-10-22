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

    public static EntityPlayerMP getPlayer(ICommandSender sender, String name) {
        EntityPlayerMP p = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(name);
        if (p == null) throw new PlayerNotFoundException();
        return p;
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
        sender.addChatMessage(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation(message, args));
    }

    @Override
    public int compareTo(Object o) {
        return getCommandName().compareTo(((ICommand) o).getCommandName());
    }
}
