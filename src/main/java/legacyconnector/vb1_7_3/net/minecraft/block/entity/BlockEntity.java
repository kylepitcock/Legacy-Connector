package legacyconnector.vb1_7_3.net.minecraft.block.entity;

import java.util.HashMap;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code TileEntity}: position and world. Not hosted in the modern world yet ({@code World.getBlockEntity} answers null);
 * NBT reading and writing are synthesized.
 */
public class BlockEntity {
    public static Map<Object, Object> idToClass = new HashMap<>();
    public static Map<Object, Object> classToId = new HashMap<>();

    public World world;
    public int x;
    public int y;
    public int z;
    protected boolean removed;

    public BlockEntity() {}

    public void tick() {}

    public void markDirty() {}

    public Block getBlock() {
        return world == null ? null : Block.BLOCKS[world.getBlockId(x, y, z)];
    }

    public boolean isRemoved() {
        return removed;
    }
}
