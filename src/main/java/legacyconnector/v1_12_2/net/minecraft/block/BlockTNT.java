package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.Explosion;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 TNT: primed by redstone, flint and steel or fire charges, or another explosion; EXPLODE (metadata 1) primes it when broken. */
public class BlockTNT extends Block {
    public static final PropertyBool EXPLODE = PropertyBool.create("explode");

    public BlockTNT() {
        super(Material.TNT);
        setDefaultState(blockState.getBaseState().withProperty(EXPLODE, false));
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        if (worldIn.isBlockPowered(pos)) {
            onPlayerDestroy(worldIn, pos, state.withProperty(EXPLODE, true));
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isBlockPowered(pos)) {
            onPlayerDestroy(worldIn, pos, state.withProperty(EXPLODE, true));
            worldIn.setBlockToAir(pos);
        }
    }

    /** Blown up: a primed TNT with a short random fuse. */
    @Override
    public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
        if (worldIn.isRemote) return;
        prime(worldIn, pos, true, null);
        super.onExplosionDestroy(worldIn, pos, explosionIn);
    }

    @Override
    public void onPlayerDestroy(World worldIn, BlockPos pos, IBlockState state) {
        explode(worldIn, pos, state, null);
        super.onPlayerDestroy(worldIn, pos, state);
    }

    public void explode(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase igniter) {
        if (!worldIn.isRemote && state.getValue(EXPLODE)) prime(worldIn, pos, false, igniter);
    }

    /** Spawns the modern primed TNT there (its igniter as the owner). */
    private static void prime(World worldIn, BlockPos pos, boolean shortFuse, EntityLivingBase igniter) {
        var level = worldIn.mcc$level();
        if (level == null) return;
        net.minecraft.world.entity.LivingEntity owner = igniter == null ? null
                : igniter.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
        var tnt = new net.minecraft.world.entity.item.PrimedTnt(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, owner);
        if (shortFuse) tnt.setFuse((short) (level.random.nextInt(tnt.getFuse() / 4) + tnt.getFuse() / 8));
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), net.minecraft.sounds.SoundEvents.TNT_PRIMED, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (!held.isEmpty() && (held.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.FLINT_AND_STEEL
                || held.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.FIRE_CHARGE)) {
            explode(worldIn, pos, state.withProperty(EXPLODE, true), playerIn);
            worldIn.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState(), 11);
            if (held.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.FLINT_AND_STEEL) held.damageItem(1, playerIn);
            else if (!playerIn.isCreative()) held.shrink(1);
            return true;
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosionIn) {
        return false;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(EXPLODE, (meta & 1) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(EXPLODE) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, EXPLODE);
    }
}
