package legacyconnector.vb1_7_3.net.minecraft.world;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code IBlockAccess}: block ids and metadata by position. The world-generation and biome members are synthesized. */
public interface BlockView {
    int getBlockId(int x, int y, int z);

    BlockEntity getBlockEntity(int x, int y, int z);

    /** MCP {@code getLightBrightness}. */
    float method_1782(int x, int y, int z);

    int getBlockMeta(int x, int y, int z);

    Material getMaterial(int x, int y, int z);

    /** MCP {@code isBlockOpaqueCube}. */
    boolean method_1783(int x, int y, int z);

    /** MCP {@code isBlockNormalCube}. */
    boolean shouldSuffocate(int x, int y, int z);
}
