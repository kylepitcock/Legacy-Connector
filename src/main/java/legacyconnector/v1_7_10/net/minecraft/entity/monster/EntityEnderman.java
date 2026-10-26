package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 enderman: the carried block id and metadata are in watcher slots 16 and 17, the screaming flag in slot 18. */
public class EntityEnderman extends EntityMob {
    private static final boolean[] carriable = new boolean[256];

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityEnderman(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

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

    /** The modern enderman this is a view of (null for a mod's enderman, which keeps the legacy watcher). */
    private net.minecraft.world.entity.monster.EnderMan mcc$enderman() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.EnderMan e && !mcc$hosted() ? e : null;
    }

    /** The block it carries (air when none). */
    public void setCarriedBlock(Block block) {
        var e = mcc$enderman();
        if (e != null) {
            e.setCarriedBlock(block == null || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.air ? null
                    : org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.toModern(block, 0));
            return;
        }
        dataWatcher.updateObject(16, (byte) (Block.getIdFromBlock(block) & 255));
    }

    public Block getCarriedBlock() {
        var e = mcc$enderman();
        if (e != null) {
            var state = e.getCarriedBlock();
            return state == null ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyBlock(state);
        }
        return Block.getBlockById(dataWatcher.getWatchableObjectByte(16) & 255);
    }

    public void func_146081_a(Block block) {
        setCarriedBlock(block);
    }

    public Block func_146080_bZ() {
        return getCarriedBlock();
    }

    /** The carried block's metadata. */
    public void setCarryingData(int meta) {
        var e = mcc$enderman();
        if (e != null) {
            var state = e.getCarriedBlock();
            if (state != null) {
                e.setCarriedBlock(org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.toModern(
                        org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyBlock(state), meta));
            }
            return;
        }
        dataWatcher.updateObject(17, (byte) (meta & 255));
    }

    public int getCarryingData() {
        var e = mcc$enderman();
        if (e != null) {
            var state = e.getCarriedBlock();
            return state == null ? 0 : org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyMeta(state);
        }
        return dataWatcher.getWatchableObjectByte(17) & 255;
    }

    /** Whether it is screaming (angry at a player who looked at it). */
    public boolean isScreaming() {
        var e = mcc$enderman();
        if (e != null) return e.isCreepy();
        return dataWatcher.getWatchableObjectByte(18) > 0;
    }

    /** A mod's enderman screams; a modern one screams by its own target logic. */
    public void setScreaming(boolean screaming) {
        if (mcc$enderman() != null) return;
        dataWatcher.updateObject(18, (byte) (screaming ? 1 : 0));
    }
}
