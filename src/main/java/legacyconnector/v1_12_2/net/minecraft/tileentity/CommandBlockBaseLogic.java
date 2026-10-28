package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's command block logic (command blocks and command block minecarts): the command, its last output and success count, and
 * running it with the game's command dispatcher at the block. A command block of the game is seen through logic over the modern one.
 */
public abstract class CommandBlockBaseLogic {
    private int successCount;
    private boolean trackOutput = true;
    private ITextComponent lastOutput;
    private String commandStored = "";
    private String customName = "@";
    private final net.minecraft.world.level.BaseCommandBlock mcc$modern;

    public CommandBlockBaseLogic() {
        this.mcc$modern = null;
    }

    protected CommandBlockBaseLogic(net.minecraft.world.level.BaseCommandBlock modern) {
        this.mcc$modern = modern;
    }

    public int getSuccessCount() {
        return mcc$modern != null ? mcc$modern.getSuccessCount() : successCount;
    }

    public void setSuccessCount(int successCountIn) {
        successCount = successCountIn;
        if (mcc$modern != null) mcc$modern.setSuccessCount(successCountIn);
    }

    public ITextComponent getLastOutput() {
        if (mcc$modern != null) return new TextComponentString(mcc$modern.getLastOutput().getString());
        return lastOutput == null ? new TextComponentString("") : lastOutput;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound p_189510_1_) {
        p_189510_1_.setString("Command", getCommand());
        p_189510_1_.setInteger("SuccessCount", getSuccessCount());
        p_189510_1_.setString("CustomName", getName());
        p_189510_1_.setBoolean("TrackOutput", trackOutput);
        if (lastOutput != null && trackOutput) p_189510_1_.setString("LastOutput", ITextComponent.Serializer.componentToJson(lastOutput));
        return p_189510_1_;
    }

    public void readDataFromNBT(NBTTagCompound nbt) {
        setCommand(nbt.getString("Command"));
        setSuccessCount(nbt.getInteger("SuccessCount"));
        if (nbt.hasKey("CustomName", 8)) customName = nbt.getString("CustomName");
        if (nbt.hasKey("TrackOutput", 1)) trackOutput = nbt.getBoolean("TrackOutput");
        if (nbt.hasKey("LastOutput", 8) && trackOutput) {
            try {
                lastOutput = ITextComponent.Serializer.jsonToComponent(nbt.getString("LastOutput"));
            } catch (RuntimeException e) {
                lastOutput = new TextComponentString(nbt.getString("LastOutput"));
            }
        } else {
            lastOutput = null;
        }
    }

    public boolean canUseCommand(int permLevel, String commandName) {
        return permLevel <= 2;
    }

    public void setCommand(String command) {
        commandStored = command == null ? "" : command;
        successCount = 0;
        if (mcc$modern != null) mcc$modern.setCommand(commandStored);
    }

    public String getCommand() {
        return mcc$modern != null ? mcc$modern.getCommand() : commandStored;
    }

    /** Runs the command at the block (server side): true when it ran. */
    public boolean trigger(World worldIn) {
        if (worldIn == null || worldIn.isRemote) return false;
        var level = worldIn.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return false;
        if (mcc$modern != null) return mcc$modern.performCommand(server);
        String command = getCommand();
        if (command.isEmpty()) {
            successCount = 0;
            return true;
        }
        var pos = getPositionVector();
        var source = new net.minecraft.commands.CommandSourceStack(net.minecraft.commands.CommandSource.NULL,
                pos, net.minecraft.world.phys.Vec2.ZERO, server, 2, getName(), net.minecraft.network.chat.Component.literal(getName()), server.getServer(), null)
                .withCallback((success, result) -> {
                    if (success) ++successCount;
                });
        successCount = 0;
        try {
            server.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException e) {
            lastOutput = new TextComponentString("Command failed: " + e.getMessage());
        }
        return true;
    }

    private net.minecraft.world.phys.Vec3 getPositionVector() {
        var p = getPosition();
        return p == null ? net.minecraft.world.phys.Vec3.ZERO : new net.minecraft.world.phys.Vec3(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D);
    }

    public String getName() {
        return mcc$modern != null ? mcc$modern.getName().getString() : customName;
    }

    public void setName(String name) {
        customName = name;
    }

    public void setLastOutput(ITextComponent lastOutputMessage) {
        lastOutput = lastOutputMessage;
    }

    public boolean shouldTrackOutput() {
        return trackOutput;
    }

    public void setTrackOutput(boolean shouldTrackOutput) {
        trackOutput = shouldTrackOutput;
    }

    public abstract void updateCommand();

    public abstract World getEntityWorld();

    public abstract legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getPosition();
}
