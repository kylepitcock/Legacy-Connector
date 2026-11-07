package legacyconnector.v1_12_2.vazkii.patchouli.api;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A multiblock structure (Patchouli 1.12 API); the instances are views over the modern Patchouli's multiblocks. */
public interface IMultiblock {
    IMultiblock offset(int x, int y, int z);

    IMultiblock offsetView(int x, int y, int z);

    IMultiblock setSymmetrical(boolean symmetrical);

    IMultiblock setResourceLocation(ResourceLocation res);

    boolean isSymmetrical();

    void place(World world, BlockPos pos, Rotation rotation);

    void forEach(World world, BlockPos pos, Rotation rotation, char c, java.util.function.Consumer<BlockPos> action);

    boolean forEachMatcher(World world, BlockPos pos, Rotation rotation, char c, MatcherAcceptor action);

    boolean validate(World world, BlockPos pos);

    boolean test(World world, BlockPos start, int x, int y, int z, Rotation rotation);

    interface MatcherAcceptor {
        boolean accepts(BlockPos pos, BlockPos relPos, int x, int y, int z, char c, IStateMatcher matcher);
    }
}
