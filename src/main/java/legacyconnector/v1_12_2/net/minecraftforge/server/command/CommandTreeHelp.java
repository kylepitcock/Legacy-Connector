package legacyconnector.v1_12_2.net.minecraftforge.server.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.command.CommandBase;
import legacyconnector.v1_12_2.net.minecraft.command.CommandException;
import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/** The {@code help} sub-command of a {@link CommandTreeBase}: the usage of every sub-command, or of the one named. */
public class CommandTreeHelp extends CommandTreeBase {
    private final ICommand parent;

    public CommandTreeHelp(CommandTreeBase parent) {
        this.parent = parent;
        for (ICommand command : parent.getSubCommands()) addSubcommand(new HelpSubCommand(this, command));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.forge.usage.help";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws Exception {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentTranslation(parent.getUsage(sender)));
            for (ICommand subCommand : getSubCommands()) {
                if (subCommand instanceof HelpSubCommand h && h.command.checkPermission(server, sender)) subCommand.execute(server, sender, args);
            }
            return;
        }
        super.execute(server, sender, args);
    }

    private static final class HelpSubCommand extends CommandBase {
        private final CommandTreeHelp parent;
        final ICommand command;

        HelpSubCommand(CommandTreeHelp parent, ICommand command) {
            this.parent = parent;
            this.command = command;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public String getName() {
            return command.getName();
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return parent.getUsage(sender);
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return command.checkPermission(server, sender);
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            sender.sendMessage(new TextComponentString("/" + getName() + ": ").appendSibling(new TextComponentTranslation(command.getUsage(sender))));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, new ArrayList<>(Collections.singletonList(command.getName()))) : Collections.emptyList();
        }
    }
}
