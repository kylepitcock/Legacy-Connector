package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Call-site redirects of Minecraft 1.4.7 (the {@code R ... static} rules of {@code forward/1.4.7/member-manual.tsv}): methods 1.4.7 had under other
 * shapes than 1.5.2, which the forward tables chain onto. The block-setting family had a variant per notification mode; 1.5 folded them into
 * {@code setBlock(x, y, z, id, meta, flags)}: the {@code WithNotify} variants are flags 3 (neighbor updates and client sync), the others flag 2
 * (client sync only), and the setters that took no metadata reset it to 0.
 */
public final class Redirects147 {
    private Redirects147() {}

    private static legacyconnector.v1_7_10.net.minecraft.block.Block block(int id) {
        return id <= 0 ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : orAir(Ids.block(id));
    }

    private static legacyconnector.v1_7_10.net.minecraft.block.Block orAir(legacyconnector.v1_7_10.net.minecraft.block.Block b) {
        return b != null ? b : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
    }

    /** {@code world.setBlockAndMetadataWithNotify(x, y, z, id, meta)}. */
    public static boolean setBlockAndMetadataWithNotify(World w, int x, int y, int z, int id, int meta) {
        return w.setBlock(x, y, z, block(id), meta, 3);
    }

    /** {@code world.setBlockAndMetadata(x, y, z, id, meta)}: no neighbor updates. */
    public static boolean setBlockAndMetadata(World w, int x, int y, int z, int id, int meta) {
        return w.setBlock(x, y, z, block(id), meta, 2);
    }

    /** {@code world.setBlockWithNotify(x, y, z, id)}. */
    public static boolean setBlockWithNotify(World w, int x, int y, int z, int id) {
        return w.setBlock(x, y, z, block(id), 0, 3);
    }

    /** {@code world.setBlockMetadataWithNotify(x, y, z, meta)}. */
    public static void setBlockMetadataWithNotify(World w, int x, int y, int z, int meta) {
        w.setBlockMetadataWithNotify(x, y, z, meta, 3);
    }

    /** {@code world.setBlockMetadata(x, y, z, meta)}: no neighbor updates. */
    public static boolean setBlockMetadata(World w, int x, int y, int z, int meta) {
        w.setBlockMetadataWithNotify(x, y, z, meta, 2);
        return true;
    }

    /** {@code world.canPlaceEntityOnSide(blockId, x, y, z, ignoreEntities, side, entity)}. */
    public static boolean canPlaceEntityOnSide(World w, int id, int x, int y, int z, boolean ignoreEntities, int side,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        return w.canPlaceEntityOnSide(block(id), x, y, z, ignoreEntities, side, entity, null);
    }
}
