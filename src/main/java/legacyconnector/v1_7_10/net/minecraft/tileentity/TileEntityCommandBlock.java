package legacyconnector.v1_7_10.net.minecraft.tileentity;

import io.netty.buffer.ByteBuf;
import legacyconnector.v1_7_10.net.minecraft.command.server.CommandBlockLogic;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A command block: its {@link CommandBlockLogic} ({@link #func_145993_a}) holds the command, the success count and the last
 * output. For a vanilla command block it is a view whose logic reads and runs the modern block's command (ComputerCraft's
 * command block peripheral drives vanilla command blocks this way).
 */
public class TileEntityCommandBlock extends TileEntity {
    private final CommandBlockLogic field_145994_a;

    public TileEntityCommandBlock() {
        this.field_145994_a = new CommandBlockLogic() {
            @Override
            public ChunkCoordinates getPlayerCoordinates() {
                return new ChunkCoordinates(xCoord, yCoord, zCoord);
            }

            @Override
            public World getEntityWorld() {
                return getWorldObj();
            }

            @Override
            public void setCommand(String command) {
                super.setCommand(command);
                markDirty();
            }

            @Override
            public void func_145756_e() {
                if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }

            @Override
            public int func_145751_f() {
                return 0;
            }

            @Override
            public void func_145757_a(ByteBuf buf) {
                buf.writeInt(xCoord);
                buf.writeInt(yCoord);
                buf.writeInt(zCoord);
            }
        };
    }

    /** View of a vanilla command block. */
    public TileEntityCommandBlock(net.minecraft.world.level.block.entity.CommandBlockEntity block) {
        net.minecraft.world.level.BaseCommandBlock modern = block.getCommandBlock();
        this.field_145994_a = new CommandBlockLogic() {
            @Override
            public ChunkCoordinates getPlayerCoordinates() {
                return new ChunkCoordinates(xCoord, yCoord, zCoord);
            }

            @Override
            public World getEntityWorld() {
                return getWorldObj();
            }

            @Override
            public void setCommand(String command) {
                modern.setCommand(command == null ? "" : command);
                block.setChanged();
            }

            @Override
            public String getCustomName() {
                return modern.getCommand();
            }

            @Override
            public int getSuccessCount() {
                return modern.getSuccessCount();
            }

            @Override
            public void func_145755_a(World world) {
                var level = block.getLevel();
                if (level != null && !level.isClientSide()) modern.performCommand(level);
            }

            @Override
            public void func_145756_e() {
                block.setChanged();
            }

            @Override
            public int func_145751_f() {
                return 0;
            }

            @Override
            public void func_145757_a(ByteBuf buf) {
                buf.writeInt(xCoord);
                buf.writeInt(yCoord);
                buf.writeInt(zCoord);
            }
        };
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        field_145994_a.writeDataToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        field_145994_a.readDataFromNBT(tag);
    }

    /** The command block's logic (command, success count, output). */
    public CommandBlockLogic func_145993_a() {
        return field_145994_a;
    }
}
