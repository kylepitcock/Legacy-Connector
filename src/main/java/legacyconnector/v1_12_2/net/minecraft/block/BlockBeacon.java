package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBeacon;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 beacon: lit, opens the beacon screen; its pyramid and effects are the tile entity's (the modern beacon's for vanilla). */
public class BlockBeacon extends BlockContainer {
    public BlockBeacon() {
        super(Material.GLASS, MapColor.DIAMOND);
        setHardness(3.0F);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBeacon();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        // the vanilla beacon opens its screen as the modern block; a mod's beacon block opens its own GUI in its override
        var level = worldIn.mcc$liveLevel();
        var player = playerIn.mcc$player();
        if (level != null && player != null) {
            var be = level.getBlockEntity(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos));
            if (be instanceof net.minecraft.world.MenuProvider provider) player.openMenu(provider);
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /** Re-checks the beacon's pyramid (a block of it changed); the modern beacon re-checks on its own tick. */
    public static void updateColorAsync(World worldIn, BlockPos glassPos) {
        var level = worldIn.mcc$liveLevel();
        if (level == null) return;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(glassPos);
        for (int i = 1; i <= 4 && mp.getY() - i >= level.getMinBuildHeight(); ++i) {
            var below = mp.below(i);
            if (!level.isLoaded(below)) return;
            var state = level.getBlockState(below);
            if (state.is(net.minecraft.world.level.block.Blocks.BEACON)) {
                level.blockEvent(below, state.getBlock(), 1, 0);
                return;
            }
        }
    }
}
