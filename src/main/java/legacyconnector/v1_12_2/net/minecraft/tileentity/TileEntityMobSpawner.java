package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's monster spawner tile entity: its {@link MobSpawnerBaseLogic} at this block. A game spawner is seen through a view of it. */
public class TileEntityMobSpawner extends TileEntity implements ITickable, org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private final MobSpawnerBaseLogic spawnerLogic;

    public TileEntityMobSpawner() {
        this.spawnerLogic = new Logic();
    }

    /** A view of a game spawner. */
    public TileEntityMobSpawner(net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
        this.spawnerLogic = new Logic(spawner.getSpawner());
    }

    private final class Logic extends MobSpawnerBaseLogic {
        Logic() {
        }

        Logic(net.minecraft.world.level.BaseSpawner modern) {
            super(modern);
        }

        @Override
        public void broadcastEvent(int id) {
            if (world != null) world.addBlockEvent(pos, getBlockType(), id, 0);
        }

        @Override
        public World getSpawnerWorld() {
            return world;
        }

        @Override
        public BlockPos getSpawnerPosition() {
            return pos;
        }
    }

    @Override
    public void mcc$refresh() {
        spawnerLogic.mcc$pull();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        spawnerLogic.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        spawnerLogic.writeToNBT(compound);
        return compound;
    }

    @Override
    public void update() {
        spawnerLogic.updateSpawner();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = writeToNBT(new NBTTagCompound());
        tag.removeTag("SpawnPotentials");
        return tag;
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        return spawnerLogic.setDelayToMin(id) || super.receiveClientEvent(id, type);
    }

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }

    public MobSpawnerBaseLogic getSpawnerBaseLogic() {
        return spawnerLogic;
    }
}
