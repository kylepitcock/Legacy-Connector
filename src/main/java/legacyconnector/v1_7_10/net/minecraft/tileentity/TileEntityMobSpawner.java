package legacyconnector.v1_7_10.net.minecraft.tileentity;

import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Legacy view of a vanilla spawner block (see TileEntityBridge): legacy code picks the mob through its spawner logic. */
public class TileEntityMobSpawner extends TileEntity {
    private final MobSpawnerBaseLogic logic;

    /** A spawner a mod's block hosts itself: its logic takes the world and position from this tile entity and it ticks here. */
    public TileEntityMobSpawner() {
        this.logic = new MobSpawnerBaseLogic(null) {
            @Override
            public legacyconnector.v1_7_10.net.minecraft.world.World getSpawnerWorld() {
                return TileEntityMobSpawner.this.worldObj;
            }

            @Override
            public int getSpawnerX() {
                return TileEntityMobSpawner.this.xCoord;
            }

            @Override
            public int getSpawnerY() {
                return TileEntityMobSpawner.this.yCoord;
            }

            @Override
            public int getSpawnerZ() {
                return TileEntityMobSpawner.this.zCoord;
            }

            @Override
            public void func_98267_a(int id) {
                legacyconnector.v1_7_10.net.minecraft.world.World world = TileEntityMobSpawner.this.worldObj;
                if (world != null) world.addBlockEvent(xCoord(), yCoord(), zCoord(), legacyconnector.v1_7_10.net.minecraft.init.Blocks.mob_spawner, id, 0);
            }

            private int xCoord() {
                return TileEntityMobSpawner.this.xCoord;
            }

            private int yCoord() {
                return TileEntityMobSpawner.this.yCoord;
            }

            private int zCoord() {
                return TileEntityMobSpawner.this.zCoord;
            }
        };
    }

    @Override
    public void readFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.readFromNBT(tag);
        logic.readFromNBT(tag);
    }

    @Override
    public void writeToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeToNBT(tag);
        logic.writeToNBT(tag);
    }

    @Override
    public void updateEntity() {
        logic.updateSpawner();
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        return logic.setDelayToMin(id) || super.receiveClientEvent(id, type);
    }

    public TileEntityMobSpawner(SpawnerBlockEntity host) {
        this.logic = new MobSpawnerBaseLogic(host);
    }

    public MobSpawnerBaseLogic getSpawnerBaseLogic() {
        return logic;
    }

    /** 1.7.10 MCP has no readable name for this one: mods call it by its SRG name. */
    public MobSpawnerBaseLogic func_145881_a() {
        return logic;
    }
}
