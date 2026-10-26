package legacyconnector.v1_7_10.net.minecraft.command.server;

import io.netty.buffer.ByteBuf;
import java.text.SimpleDateFormat;
import java.util.Date;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Vanilla's command block state (command, custom name, success count, last output) as mods subclass it for their own
 * command-running blocks. Commands run through the legacy command manager.
 */
public abstract class CommandBlockLogic implements ICommandSender {
    private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss");
    private int successCount;
    private boolean trackOutput = true;
    private IChatComponent lastOutput;
    private String commandStored = "";
    private String customName = "@";

    public int getSuccessCount() {
        return successCount;
    }

    public IChatComponent getLastOutput() {
        return lastOutput;
    }

    public void writeDataToNBT(NBTTagCompound tag) {
        tag.setString("Command", commandStored);
        tag.setInteger("SuccessCount", successCount);
        tag.setString("CustomName", customName);
        if (lastOutput != null) tag.setString("LastOutput", IChatComponent.Serializer.componentToJson(lastOutput));
        tag.setBoolean("TrackOutput", trackOutput);
    }

    public void readDataFromNBT(NBTTagCompound tag) {
        commandStored = tag.getString("Command");
        successCount = tag.getInteger("SuccessCount");
        if (tag.hasKey("CustomName", 8)) customName = tag.getString("CustomName");
        if (tag.hasKey("LastOutput", 8)) lastOutput = IChatComponent.Serializer.jsonToComponent(tag.getString("LastOutput"));
        if (tag.hasKey("TrackOutput", 1)) trackOutput = tag.getBoolean("TrackOutput");
    }

    @Override
    public boolean canCommandSenderUseCommand(int permissionLevel, String command) {
        return permissionLevel <= 2;
    }

    public void setCommand(String command) {
        this.commandStored = command;
    }

    /** The stored command (1.7.10's MCP names this getter "getCustomName"; the sender name is {@link #getCommandSenderName}). */
    public String getCustomName() {
        return commandStored;
    }

    /** Runs the stored command; the result count becomes the success count (comparators read it). */
    public void func_145755_a(World world) {
        if (world != null && world.isRemote) successCount = 0;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.isCommandBlockEnabled()) {
            successCount = server.getCommandManager().executeCommand(this, commandStored);
        } else {
            successCount = 0;
        }
    }

    @Override
    public String getCommandSenderName() {
        return customName;
    }

    @Override
    public IChatComponent getFormattedCommandSenderName() {
        return new ChatComponentText(getCommandSenderName());
    }

    /** Sets the sender name shown in chat. */
    public void func_145754_b(String name) {
        this.customName = name;
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        World world = getEntityWorld();
        if (trackOutput && world != null && !world.isRemote) {
            lastOutput = new ChatComponentText("[" + timestampFormat.format(new Date()) + "] ").appendSibling(message);
            func_145756_e();
        }
    }

    /** The owner saves and resyncs its state (the output changed). */
    public abstract void func_145756_e();

    /** 0 for a block, 1 for a minecart (the client's edit packet says which). */
    public abstract int func_145751_f();

    /** Writes the owner's position or entity id for the edit packet. */
    public abstract void func_145757_a(ByteBuf buf);

    public void func_145750_b(IChatComponent output) {
        this.lastOutput = output;
    }
}
