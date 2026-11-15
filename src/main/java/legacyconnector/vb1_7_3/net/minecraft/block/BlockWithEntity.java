package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta 1.7.3 {@code BlockContainer}: a block with a tile entity, made by {@link #createBlockEntity} when the block is placed. */
public abstract class BlockWithEntity extends Block {
    protected BlockWithEntity(int id, Material material) {
        super(id, material);
        BLOCKS_WITH_ENTITY[id] = true;
    }

    protected BlockWithEntity(int id, int textureId, Material material) {
        super(id, textureId, material);
        BLOCKS_WITH_ENTITY[id] = true;
    }

    @Override
    public void onPlaced(World world, int x, int y, int z) {
        super.onPlaced(world, x, y, z);
        world.setBlockEntity(x, y, z, getBlockEntity(world.getBlockMeta(x, y, z)));
    }

    @Override
    public void onBreak(World world, int x, int y, int z) {
        super.onBreak(world, x, y, z);
        world.removeBlockEntity(x, y, z);
    }

    /** MCP {@code getBlockEntity}. */
    public abstract BlockEntity createBlockEntity();

    /** Forge 1.0: the tile entity for a metadata (one block, a tile entity class per metadata); the plain one by default. */
    protected BlockEntity getBlockEntity(int meta) {
        return createBlockEntity();
    }

    /** The tile entity this block makes with that metadata ({@link #getBlockEntity(int)}, which mods override as protected). */
    public final BlockEntity mcc$createBlockEntity(int meta) {
        return getBlockEntity(meta);
    }
}
