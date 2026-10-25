package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 enderman: the carried block id and metadata are in watcher slots 16 and 17, the screaming flag in slot 18. */
public class EntityEnderman extends EntityMob {
    private static final boolean[] carriable = new boolean[256];

    public EntityEnderman(World world) {
        super(world);
        setSize(0.6F, 2.9F);
    }

    /** Which blocks endermen may pick up; mods add their own. */
    public static void setCarriable(Block block, boolean value) {
        int id = Block.getIdFromBlock(block);
        if (id >= 0 && id < carriable.length) carriable[id] = value;
    }

    public static boolean getCarriable(Block block) {
        int id = Block.getIdFromBlock(block);
        return id >= 0 && id < carriable.length && carriable[id];
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
        dataWatcher.addObject(17, (byte) 0);
        dataWatcher.addObject(18, (byte) 0);
    }

    public void func_146081_a(Block block) {
        dataWatcher.updateObject(16, (byte) (Block.getIdFromBlock(block) & 255));
    }

    public Block func_146080_bZ() {
        return Block.getBlockById(dataWatcher.getWatchableObjectByte(16) & 255);
    }

    public void setCarryingData(int meta) {
        dataWatcher.updateObject(17, (byte) (meta & 255));
    }

    public int getCarryingData() {
        return dataWatcher.getWatchableObjectByte(17) & 255;
    }
}
