package legacyconnector.v1_12_2.net.minecraft.world.biome;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 biome tints blended over the 3x3 columns around a position, as vanilla's block colours did. */
public class BiomeColorHelper {
    public static final ColorResolver GRASS_COLOR = (biome, pos) -> biome.getGrassColorAtPos(pos);
    public static final ColorResolver FOLIAGE_COLOR = (biome, pos) -> biome.getFoliageColorAtPos(pos);
    public static final ColorResolver WATER_COLOR = (biome, pos) -> biome.getWaterColor();

    public static int getColorAtPos(IBlockAccess world, BlockPos pos, ColorResolver resolver) {
        int r = 0, g = 0, b = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                Biome biome = world.getBiome(p);
                int c = biome == null ? 0xFFFFFF : resolver.getColorAtPos(biome, p);
                r += (c & 0xFF0000) >> 16;
                g += (c & 0xFF00) >> 8;
                b += c & 0xFF;
            }
        }
        return (r / 9 & 255) << 16 | (g / 9 & 255) << 8 | b / 9 & 255;
    }

    public static int getGrassColorAtPos(IBlockAccess world, BlockPos pos) {
        return getColorAtPos(world, pos, GRASS_COLOR);
    }

    public static int getFoliageColorAtPos(IBlockAccess world, BlockPos pos) {
        return getColorAtPos(world, pos, FOLIAGE_COLOR);
    }

    public static int getWaterColorAtPos(IBlockAccess world, BlockPos pos) {
        return getColorAtPos(world, pos, WATER_COLOR);
    }

    public interface ColorResolver {
        int getColorAtPos(Biome biome, BlockPos pos);
    }
}
