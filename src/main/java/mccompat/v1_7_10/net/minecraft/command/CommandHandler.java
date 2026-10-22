package mccompat.v1_7_10.net.minecraft.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.kylepitcock.mccompat.v1_7_10.bridge.CommandBridge1710;

/** Command registry of a server view: registering adds the command to the modern dispatcher. */
public class CommandHandler implements ICommandManager {
    /** Commands registered before a server exists (mods do it in init); registered when the server starts. */
    private static final List<ICommand> PENDING = new ArrayList<>();
    private final Map<String, ICommand> commands = new LinkedHashMap<>();
    private final net.minecraft.server.MinecraftServer mcc$server;

    public CommandHandler() {
        this(null);
    }

    public CommandHandler(net.minecraft.server.MinecraftServer server) {
        this.mcc$server = server;
    }

    public ICommand registerCommand(ICommand command) {
        commands.put(command.getCommandName(), command);
        List<?> aliases = command.getCommandAliases();
        if (aliases != null) for (Object alias : aliases) commands.put(String.valueOf(alias), command);
        if (mcc$server != null) CommandBridge1710.register(mcc$server, command);
        else synchronized (PENDING) {
            PENDING.add(command);
        }
        return command;
    }

    public static void mcc$flushPending(net.minecraft.server.MinecraftServer server) {
        List<ICommand> todo;
        synchronized (PENDING) {
            todo = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (ICommand c : todo) CommandBridge1710.register(server, c);
    }

    @Override
    public int executeCommand(ICommandSender sender, String command) {
        String line = command.startsWith("/") ? command.substring(1) : command;
        String[] parts = line.trim().split(" +");
        ICommand c = commands.get(parts[0]);
        if (c == null || !c.canCommandSenderUseCommand(sender)) return 0;
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        c.processCommand(sender, args);
        return 1;
    }

    @Override
    public List getPossibleCommands(ICommandSender sender, String prefix) {
        List<String> out = new ArrayList<>();
        for (String name : commands.keySet()) if (name.startsWith(prefix)) out.add(name);
        return out;
    }

    @Override
    public List getPossibleCommands(ICommandSender sender) {
        return new ArrayList<>(commands.values());
    }

    @Override
    public Map getCommands() {
        return commands;
    }
}
