package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's tall plants. Without a plant type the modern game's sunflower patch (its successor) is placed; with one
 * ({@code setPlantType(BlockDoublePlant.EnumPlantType)}, answered by {@code WorldSynth1122}) 64 tries scatter that plant as 1.12 did.
 */
public class WorldGenDoublePlant extends WorldGenerator {
    private net.minecraft.world.level.block.Block mcc$plant;

    public WorldGenDoublePlant() {
        super(false);
    }

    /** The legacy plant type by its constant name (SUNFLOWER, SYRINGA, GRASS, FERN, ROSE, PAEONIA). */
    public void mcc$setPlantType(String name) {
        this.mcc$plant = name == null ? null : switch (name) {
            case "SUNFLOWER" -> net.minecraft.world.level.block.Blocks.SUNFLOWER;
            case "SYRINGA" -> net.minecraft.world.level.block.Blocks.LILAC;
            case "GRASS" -> net.minecraft.world.level.block.Blocks.TALL_GRASS;
            case "FERN" -> net.minecraft.world.level.block.Blocks.LARGE_FERN;
            case "ROSE" -> net.minecraft.world.level.block.Blocks.ROSE_BUSH;
            case "PAEONIA" -> net.minecraft.world.level.block.Blocks.PEONY;
            default -> null;
        };
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (mcc$plant == null) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernFeatures1122.place(worldIn, net.minecraft.data.worldgen.features.VegetationFeatures.PATCH_SUNFLOWER, rand, position);
        }
        var access = worldIn == null ? null : worldIn.mcc$access();
        if (access == null || position == null) return false;
        boolean placed = false;
        var state = mcc$plant.defaultBlockState();
        for (int i = 0; i < 64; ++i) {
            var p = new net.minecraft.core.BlockPos(position.getX() + rand.nextInt(8) - rand.nextInt(8), position.getY() + rand.nextInt(4) - rand.nextInt(4),
                    position.getZ() + rand.nextInt(8) - rand.nextInt(8));
            try {
                if (access.isEmptyBlock(p) && access.isEmptyBlock(p.above()) && p.getY() < access.getMaxBuildHeight() - 2 && state.canSurvive(access, p)) {
                    net.minecraft.world.level.block.DoublePlantBlock.placeAt(access, state, p, 2);
                    placed = true;
                }
            } catch (RuntimeException e) {
                // a position outside the generation region
            }
        }
        return placed;
    }
}
