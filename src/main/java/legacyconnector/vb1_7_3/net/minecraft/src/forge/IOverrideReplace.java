package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Forge 1.0: a block that decides whether a write may replace it ({@code Chunk.setBlockID}); a refused write reports {@link #getReplacedSuccess}. */
public interface IOverrideReplace {
    boolean canReplaceBlock(World world, int x, int y, int z, int newId);
    boolean getReplacedSuccess();
}
