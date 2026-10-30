package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2's stained terracotta: sixteen colours by metadata, with the terracotta map colours. */
public class BlockStainedHardenedClay extends BlockColored {
    private static final MapColor[] MAP_COLORS = {MapColor.WHITE_STAINED_HARDENED_CLAY, MapColor.ORANGE_STAINED_HARDENED_CLAY,
            MapColor.MAGENTA_STAINED_HARDENED_CLAY, MapColor.LIGHT_BLUE_STAINED_HARDENED_CLAY, MapColor.YELLOW_STAINED_HARDENED_CLAY,
            MapColor.LIME_STAINED_HARDENED_CLAY, MapColor.PINK_STAINED_HARDENED_CLAY, MapColor.GRAY_STAINED_HARDENED_CLAY,
            MapColor.SILVER_STAINED_HARDENED_CLAY, MapColor.CYAN_STAINED_HARDENED_CLAY, MapColor.PURPLE_STAINED_HARDENED_CLAY,
            MapColor.BLUE_STAINED_HARDENED_CLAY, MapColor.BROWN_STAINED_HARDENED_CLAY, MapColor.GREEN_STAINED_HARDENED_CLAY,
            MapColor.RED_STAINED_HARDENED_CLAY, MapColor.BLACK_STAINED_HARDENED_CLAY};

    public BlockStainedHardenedClay() {
        super(Material.ROCK);
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return MAP_COLORS[state.getValue(COLOR).getMetadata() & 15];
    }
}
