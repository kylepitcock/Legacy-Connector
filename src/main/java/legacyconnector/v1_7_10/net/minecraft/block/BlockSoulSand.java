package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Soul sand (1.7.10 {@code BlockSoulSand}): sand material, hardness 0.5; the box is an eighth lower and whoever walks in it is slowed. */
public class BlockSoulSand extends Block {
    public BlockSoulSand(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockSoulSand() {
        super(Material.sand);
        setHardness(0.5F);
        setStepSound(soundTypeSand);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if (mcc$vanilla != null) return super.getCollisionBoundingBoxFromPool(world, x, y, z);
        float inset = 0.125F;
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1 - inset, z + 1);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (mcc$vanilla != null) {
            super.onEntityCollidedWithBlock(world, x, y, z, entity);
            return;
        }
        Entity.mcc$set$motionX(entity, Entity.mcc$get$motionX(entity) * 0.4D);
        Entity.mcc$set$motionZ(entity, Entity.mcc$get$motionZ(entity) * 0.4D);
    }
}
