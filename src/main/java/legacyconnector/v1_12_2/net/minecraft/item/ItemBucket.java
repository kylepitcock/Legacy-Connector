package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 bucket: an empty one picks up still water or lava, a full one places its block (water evaporates in the nether). Mods subclass
 * it with their own liquid block; Forge's FillBucketEvent lets mods handle the use first.
 */
public class ItemBucket extends Item {
    private final Block containedBlock;

    public ItemBucket(Block containedBlockIn) {
        this.maxStackSize = 1;
        this.containedBlock = containedBlockIn;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        boolean empty = containedBlock == Blocks.AIR;
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        RayTraceResult hit = rayTrace(worldIn, playerIn, empty);
        ActionResult<ItemStack> ret = legacyconnector.v1_12_2.net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, hit);
        if (ret != null) return ret;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return new ActionResult<>(EnumActionResult.PASS, itemstack);
        BlockPos blockpos = hit.getBlockPos();
        if (!worldIn.isBlockModifiable(playerIn, blockpos)) return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        if (empty) {
            if (!playerIn.canPlayerEdit(blockpos.offset(hit.sideHit), hit.sideHit, itemstack)) return new ActionResult<>(EnumActionResult.FAIL, itemstack);
            IBlockState state = worldIn.getBlockState(blockpos);
            Material material = state.getMaterial();
            boolean source = state.getPropertyKeys().contains(BlockLiquid.LEVEL) && state.getValue(BlockLiquid.LEVEL) == 0;
            if (material == Material.WATER && source) {
                worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 11);
                playerIn.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.getObjectUseStats(this));
                playerIn.playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
                return new ActionResult<>(EnumActionResult.SUCCESS, fillBucket(itemstack, playerIn, Items.WATER_BUCKET));
            }
            if (material == Material.LAVA && source) {
                playerIn.playSound(SoundEvents.ITEM_BUCKET_FILL_LAVA, 1.0F, 1.0F);
                worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 11);
                playerIn.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.getObjectUseStats(this));
                return new ActionResult<>(EnumActionResult.SUCCESS, fillBucket(itemstack, playerIn, Items.LAVA_BUCKET));
            }
            return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        }
        boolean replaceable = worldIn.getBlockState(blockpos).getBlock().isReplaceable(worldIn, blockpos);
        BlockPos target = replaceable && hit.sideHit == EnumFacing.UP ? blockpos : blockpos.offset(hit.sideHit);
        if (!playerIn.canPlayerEdit(target, hit.sideHit, itemstack)) return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        if (tryPlaceContainedLiquid(playerIn, worldIn, target)) {
            playerIn.addStat(legacyconnector.v1_12_2.net.minecraft.stats.StatList.getObjectUseStats(this));
            return playerIn.isCreative() ? new ActionResult<>(EnumActionResult.SUCCESS, itemstack)
                    : new ActionResult<>(EnumActionResult.SUCCESS, new ItemStack(Items.BUCKET));
        }
        return new ActionResult<>(EnumActionResult.FAIL, itemstack);
    }

    private ItemStack fillBucket(ItemStack emptyBuckets, EntityPlayer player, Item fullBucket) {
        if (player.isCreative()) return emptyBuckets;
        emptyBuckets.shrink(1);
        if (emptyBuckets.isEmpty()) return new ItemStack(fullBucket);
        var p = player.mcc$player();
        if (p != null) {
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(new ItemStack(fullBucket));
            if (!p.getInventory().add(modern)) p.drop(modern, false);
        }
        return emptyBuckets;
    }

    /** Places the bucket's block at a position (replacing a non-solid block, evaporating water where water vaporizes). */
    public boolean tryPlaceContainedLiquid(EntityPlayer player, World worldIn, BlockPos posIn) {
        if (containedBlock == Blocks.AIR) return false;
        IBlockState state = worldIn.getBlockState(posIn);
        Material material = state.getMaterial();
        boolean notSolid = !material.isSolid();
        boolean replaceable = state.getBlock().isReplaceable(worldIn, posIn);
        if (!worldIn.isAirBlock(posIn) && !notSolid && !replaceable) return false;
        var level = worldIn.mcc$level();
        if (level != null && level.dimensionType().ultraWarm() && containedBlock == Blocks.FLOWING_WATER) {
            int x = posIn.getX(), y = posIn.getY(), z = posIn.getZ();
            worldIn.playSound(player, posIn, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                    2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);
            for (int k = 0; k < 8; ++k) {
                worldIn.spawnParticle(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes.SMOKE_LARGE, (double) x + Math.random(),
                        (double) y + Math.random(), (double) z + Math.random(), 0.0D, 0.0D, 0.0D);
            }
            return true;
        }
        if (!worldIn.isRemote && (notSolid || replaceable) && !material.isLiquid()) worldIn.destroyBlock(posIn, true);
        var sound = containedBlock == Blocks.FLOWING_LAVA ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        worldIn.playSound(player, posIn, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        worldIn.setBlockState(posIn, containedBlock.getDefaultState(), 11);
        return true;
    }
}
