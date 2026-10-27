package legacyconnector.v1_12_2.net.minecraftforge.server.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.command.CommandBase;
import legacyconnector.v1_12_2.net.minecraft.command.CommandException;
import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/** Forge 1.12.2's command with sub-commands: the first argument picks the sub-command, which gets the remaining arguments. */
public abstract class CommandTreeBase extends CommandBase {
    private final Map<String, ICommand> commandMap = new HashMap<>();
    private final Map<String, ICommand> commandAliasMap = new HashMap<>();

    static {
        Map<String, String> en = org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations.table("en_us");
        en.putIfAbsent("commands.tree_base.invalid_cmd.list_subcommands", "Invalid subcommand '%s'! Available subcommands: %s");
        en.putIfAbsent("commands.tree_base.available_subcommands", "Available subcommands: %s");
        en.putIfAbsent("commands.forge.usage.help", "/%s help [subcommand]");
    }

    public void addSubcommand(ICommand command) {
        commandMap.put(command.getName(), command);
        List<String> aliases = command.getAliases();
        if (aliases != null) for (String alias : aliases) commandAliasMap.put(alias, command);
    }

    public Collection<ICommand> getSubCommands() {
        return commandMap.values();
    }

    public ICommand getSubCommand(String command) {
        ICommand cmd = commandMap.get(command);
        return cmd != null ? cmd : commandAliasMap.get(command);
    }

    public Map<String, ICommand> getCommandMap() {
        return Collections.unmodifiableMap(commandMap);
    }

    public List<ICommand> getSortedCommandList() {
        List<ICommand> list = new ArrayList<>(getSubCommands());
        Collections.sort(list);
        return list;
    }

    private static String[] shiftArgs(String[] s) {
        if (s == null || s.length == 0) return new String[0];
        String[] out = new String[s.length - 1];
        System.arraycopy(s, 1, out, 0, out.length);
        return out;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> keys = new ArrayList<>();
            for (ICommand c : getSubCommands()) if (c.checkPermission(server, sender)) keys.add(c.getName());
            keys.sort(null);
            return getListOfStringsMatchingLastWord(args, keys);
        }
        ICommand cmd = getSubCommand(args[0]);
        if (cmd != null) return cmd.getTabCompletions(server, sender, shiftArgs(args), pos);
        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (index > 0 && args.length > 1) {
            ICommand cmd = getSubCommand(args[0]);
            if (cmd != null) return cmd.isUsernameIndex(shiftArgs(args), index - 1);
        }
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws Exception {
        if (args.length < 1) {
            sender.sendMessage(new TextComponentTranslation("commands.tree_base.available_subcommands", getAvailableSubCommandsString(server, sender)));
            return;
        }
        ICommand cmd = getSubCommand(args[0]);
        if (cmd == null) {
            throw new CommandException("commands.tree_base.invalid_cmd.list_subcommands", args[0], getAvailableSubCommandsString(server, sender));
        } else if (!cmd.checkPermission(server, sender)) {
            throw new CommandException("commands.generic.permission");
        } else {
            cmd.execute(server, sender, shiftArgs(args));
        }
    }

    private String getAvailableSubCommandsString(MinecraftServer server, ICommandSender sender) {
        Collection<String> available = new ArrayList<>();
        for (ICommand command : getSubCommands()) if (command.checkPermission(server, sender)) available.add(command.getName());
        return CommandBase.joinNiceStringFromCollection(available);
    }
}
