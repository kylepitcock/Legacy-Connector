package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 magma block: lit (light 0.2), burns what walks on it unless it sneaks or resists fire. */
public class BlockMagma extends Block {
    public BlockMagma() {
        super(Material.ROCK, MapColor.NETHERRACK);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setLightLevel(0.2F);
        setTickRandomly(true);
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        if (!entityIn.isImmuneToFire() && entityIn instanceof EntityLivingBase living
                && !legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.hasFrostWalkerEnchantment(living)) {
            entityIn.attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource.HOT_FLOOR, 1.0F);
        }
        super.onEntityWalk(worldIn, pos, entityIn);
    }

    @Override
    public boolean canEntitySpawn(IBlockState state, Entity entityIn) {
        return entityIn.isImmuneToFire();
    }
}
