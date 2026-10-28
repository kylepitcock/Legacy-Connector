package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's command block tile entity: its {@link CommandBlockBaseLogic}. A command block of the game is seen through a view of it. */
public class TileEntityCommandBlock extends TileEntity {
    private final CommandBlockBaseLogic commandBlockLogic;

    public TileEntityCommandBlock() {
        this.commandBlockLogic = new Logic(null);
    }

    /** A view of a game command block. */
    public TileEntityCommandBlock(net.minecraft.world.level.block.entity.CommandBlockEntity command) {
        this.commandBlockLogic = new Logic(command.getCommandBlock());
    }

    private final class Logic extends CommandBlockBaseLogic {
        Logic(net.minecraft.world.level.BaseCommandBlock modern) {
            super(modern);
        }

        @Override
        public void updateCommand() {
            markDirty();
        }

        @Override
        public World getEntityWorld() {
            return world;
        }

        @Override
        public BlockPos getPosition() {
            return pos;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        commandBlockLogic.writeToNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        commandBlockLogic.readDataFromNBT(compound);
    }

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }

    public CommandBlockBaseLogic getCommandBlockLogic() {
        return commandBlockLogic;
    }
}
