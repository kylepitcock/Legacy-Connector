package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/**
 * Commands by name and alias. The server's handler ({@code MinecraftServer.getCommandManager()}) also gives each command a modern
 * command of the same name ({@code CommandBridge1122}); other handlers (Forge's client command handler) only keep them.
 */
public class CommandHandler implements ICommandManager {
    private final Map<String, ICommand> commandMap = new HashMap<>();
    private final Set<ICommand> commandSet = new LinkedHashSet<>();

    @Override
    public int executeCommand(ICommandSender sender, String rawCommand) {
        String raw = rawCommand == null ? "" : rawCommand.trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] parts = raw.split(" ");
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        ICommand command;
        synchronized (this) {
            command = commandMap.get(parts[0]);
        }
        if (command == null) {
            sender.sendMessage(error("commands.generic.notFound"));
            return 0;
        }
        if (!command.checkPermission(getServer(), sender)) {
            sender.sendMessage(error("commands.generic.permission"));
            return 0;
        }
        return tryExecute(sender, args, command, raw) ? 1 : 0;
    }

    protected boolean tryExecute(ICommandSender sender, String[] args, ICommand command, String input) {
        try {
            command.execute(getServer(), sender, args);
            return true;
        } catch (Exception e) {
            sender.sendMessage(org.kylepitcock.legacyconnector.v1_12_2.bridge.CommandBridge1122.errorText(e));
            return false;
        }
    }

    private static TextComponentTranslation error(String key) {
        TextComponentTranslation text = new TextComponentTranslation(key);
        text.getStyle().setColor(TextFormatting.RED);
        return text;
    }

    protected MinecraftServer getServer() {
        return MinecraftServer.mcc$instance();
    }

    public ICommand registerCommand(ICommand command) {
        String name = command.getName();
        synchronized (this) {
            commandMap.put(name, command);
            commandSet.add(command);
            List<String> aliases = command.getAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    ICommand existing = commandMap.get(alias);
                    if (existing == null || !existing.getName().equals(alias)) commandMap.put(alias, command);
                }
            }
        }
        org.kylepitcock.legacyconnector.v1_12_2.bridge.CommandBridge1122.registered(this, command);
        return command;
    }

    @Override
    public List<String> getTabCompletions(ICommandSender sender, String input, BlockPos pos) {
        String[] parts = (input == null ? "" : input).split(" ", -1);
        String name = parts[0];
        List<String> out = new ArrayList<>();
        if (parts.length == 1) {
            synchronized (this) {
                for (Map.Entry<String, ICommand> e : commandMap.entrySet()) {
                    if (e.getKey().regionMatches(true, 0, name, 0, name.length()) && e.getValue().checkPermission(getServer(), sender)) out.add(e.getKey());
                }
            }
            return out;
        }
        ICommand command;
        synchronized (this) {
            command = commandMap.get(name);
        }
        if (command != null && command.checkPermission(getServer(), sender)) {
            List<String> completions = command.getTabCompletions(getServer(), sender, Arrays.copyOfRange(parts, 1, parts.length), pos);
            if (completions != null) out.addAll(completions);
        }
        return out;
    }

    @Override
    public List<ICommand> getPossibleCommands(ICommandSender sender) {
        List<ICommand> out = new ArrayList<>();
        synchronized (this) {
            for (ICommand c : commandSet) if (c.checkPermission(getServer(), sender)) out.add(c);
        }
        return out;
    }

    @Override
    public synchronized Map<String, ICommand> getCommands() {
        return commandMap;
    }

    /** The registered commands, each once. */
    public synchronized List<ICommand> mcc$commands() {
        return new ArrayList<>(commandSet);
    }
}
