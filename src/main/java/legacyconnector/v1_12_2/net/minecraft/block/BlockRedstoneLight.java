package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 redstone lamp: the lit and unlit blocks swap when powered (turning off waits four ticks). */
public class BlockRedstoneLight extends Block {
    private final boolean isOn;

    public BlockRedstoneLight(boolean isOn) {
        super(Material.REDSTONE_LIGHT);
        this.isOn = isOn;
        if (isOn) setLightLevel(1.0F);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) update(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!worldIn.isRemote) update(worldIn, pos);
    }

    private void update(World worldIn, BlockPos pos) {
        boolean powered = worldIn.isBlockPowered(pos);
        if (isOn && !powered) worldIn.scheduleUpdate(pos, this, 4);
        else if (!isOn && powered) worldIn.setBlockState(pos, Blocks.LIT_REDSTONE_LAMP.getDefaultState(), 2);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isRemote && isOn && !worldIn.isBlockPowered(pos)) worldIn.setBlockState(pos, Blocks.REDSTONE_LAMP.getDefaultState(), 2);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(Blocks.REDSTONE_LAMP);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Blocks.REDSTONE_LAMP);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(Blocks.REDSTONE_LAMP);
    }
}
