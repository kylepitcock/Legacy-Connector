package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 slime blocks: bouncy (no fall damage, rebound), slippery, sticky to pistons. */
public class BlockSlime extends BlockBreakable {
    public BlockSlime() {
        super(Material.CLAY, false, MapColor.GRASS);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        this.slipperiness = 0.8F;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance) {
        if (entityIn.isSneaking()) super.onFallenUpon(worldIn, pos, entityIn, fallDistance);
        else entityIn.fall(fallDistance, 0.0F);
    }

    @Override
    public void onLanded(World worldIn, Entity entityIn) {
        if (entityIn.isSneaking()) {
            super.onLanded(worldIn, entityIn);
        } else if (Entity.mcc$get$motionY(entityIn) < 0.0D) {
            double y = -Entity.mcc$get$motionY(entityIn);
            if (!(entityIn instanceof EntityLivingBase)) y *= 0.8D;
            Entity.mcc$set$motionY(entityIn, y);
        }
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        double my = Entity.mcc$get$motionY(entityIn);
        if (Math.abs(my) < 0.1D && !entityIn.isSneaking()) {
            double d0 = 0.4D + Math.abs(my) * 0.2D;
            Entity.mcc$set$motionX(entityIn, Entity.mcc$get$motionX(entityIn) * d0);
            Entity.mcc$set$motionZ(entityIn, Entity.mcc$get$motionZ(entityIn) * d0);
        }
        super.onEntityWalk(worldIn, pos, entityIn);
    }

    @Override
    public boolean isStickyBlock(IBlockState state) {
        return true;
    }
}
