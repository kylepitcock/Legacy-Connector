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

    /** 1.4.7 {@code FurnaceRecipes.smelting().getSmeltingResult(itemId)}: recipes by item id (1.5 passes the stack); the id lookup matches any damage. */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemStack getSmeltingResult(
            legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes r, int id) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = Ids.item(id);
        if (item == null) return null;
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack out = r.getSmeltingResult(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 0));
        return out == null ? null : out.copy();
    }

    /** 1.4.7 {@code FurnaceRecipes.getExperience(itemId)}. */
    public static float getExperience(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes r, int id) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = Ids.item(id);
        return item == null ? 0.0F : r.getSmeltingExperience(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 0));
    }

    /** 1.4.7 {@code item.getIconFromDamage(damage)}: the sheet index (1.5 returns an Icon). */
    public static int itemIconIndex(legacyconnector.v1_7_10.net.minecraft.item.Item item, int damage) {
        return SpriteSheets147.indexOf(item.getIconFromDamage(damage), false);
    }

    /** 1.4.7 {@code stack.getIconIndex()}. */
    public static int stackIconIndex(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        return stack == null || stack.getItem() == null ? 0 : SpriteSheets147.indexOf(stack.getIconIndex(), false);
    }

    /** 1.4.7 {@code block.getBlockTextureFromSide(side)}. */
    public static int blockTextureFromSide(legacyconnector.v1_7_10.net.minecraft.block.Block block, int side) {
        return SpriteSheets147.indexOf(block.getBlockTextureFromSide(side), true);
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

    /** {@code world.setBlockAndMetadataWithUpdate(x, y, z, id, meta, updateLights)}: neighbor updates and client sync (the light always updates in 1.21). */
    public static boolean setBlockAndMetadataWithUpdate(World w, int x, int y, int z, int id, int meta, boolean updateLights) {
        return w.setBlock(x, y, z, block(id), meta, 3);
    }

    /**
     * {@code IBlockAccess.getLightBrightness}-style value of a light level 0 to 15 (the curve of the 1.4 and 1.5 world: 0.05 in the dark, 1.0 in full
     * light): what {@code Block.getBlockBrightness} returned.
     */
    public static float lightBrightness(int level) {
        float f = 1.0F - Math.max(0, Math.min(15, level)) / 15.0F;
        return (1.0F - f) / (f * 3.0F + 1.0F) * 0.95F + 0.05F;
    }

    /** The light level 0 to 15 behind a 1.7.10 packed brightness ({@code sky << 20 | block << 4}): the brighter of the two lights. */
    public static int lightLevel(int packedBrightness) {
        return Math.max(packedBrightness >> 20 & 15, packedBrightness >> 4 & 15);
    }

    /** 1.4.7 {@code block.isBlockNormalCube(world, x, y, z)}: an opaque material and a full cube. */
    public static boolean isBlockNormalCube(legacyconnector.v1_7_10.net.minecraft.block.Block block, World w, int x, int y, int z) {
        return block.getMaterial().isOpaque() && block.renderAsNormalBlock();
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
