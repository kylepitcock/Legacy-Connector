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

    /**
     * Index redirects in progress, by receiver and argument. The shim answers them through {@code getIcon}/{@code getIconFromDamage}, which a 1.4.7
     * mod's own override (bridged from its int-returning method) may answer by calling the very method being redirected (Mystcraft's BlockLinkModifier:
     * {@code getIcon} -> getBlockTextureFromSide -> {@code getIcon} ...): the nested call gets the plain sheet index of the block or item instead of recursing.
     */
    private static final ReentryGuard GUARD = new ReentryGuard();

    private static boolean enter(Object owner, int kind, int arg) {
        return GUARD.enter(owner, kind, arg);
    }

    private static void leave() {
        GUARD.leave();
    }

    /** 1.4.7 {@code item.getIconFromDamage(damage)}: the sheet index (1.5 returns an Icon). */
    public static int itemIconIndex(legacyconnector.v1_7_10.net.minecraft.item.Item item, int damage) {
        if (!enter(item, 0, damage)) return Math.max(item.iconIndex, 0);
        try {
            return SpriteSheets147.indexOf(item.getIconFromDamage(damage), false);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code stack.getIconIndex()}. */
    public static int stackIconIndex(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        if (!enter(stack, 1, 0)) return Math.max(stack.getItem().iconIndex, 0);
        try {
            return SpriteSheets147.indexOf(stack.getIconIndex(), false);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code block.getBlockTextureFromSide(side)}. */
    public static int blockTextureFromSide(legacyconnector.v1_7_10.net.minecraft.block.Block block, int side) {
        if (!enter(block, 2, side)) return Math.max(block.blockIndexInTexture, 0);
        try {
            return SpriteSheets147.indexOf(block.getBlockTextureFromSide(side), true);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code block.getBlockTexture(access, x, y, z, side)}: the sheet index at a position (guarded like {@link #blockTextureFromSide}). */
    public static int blockTexture(legacyconnector.v1_7_10.net.minecraft.block.Block block, legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess access,
            int x, int y, int z, int side) {
        if (!enter(block, 3, ((x * 31 + y) * 31 + z) * 8 + side)) return Math.max(block.blockIndexInTexture, 0);
        try {
            return SpriteSheets147.indexOf(block.getIcon(access, x, y, z, side), true);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code item.getIconIndex(stack)}: the sheet index of the icon the stack shows (the item's own damage icon when it has no override). */
    public static int itemIconIndexOfStack(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        if (stack == null) return Math.max(item.iconIndex, 0);
        if (!enter(item, 4, stack.getItemDamage())) return Math.max(item.iconIndex, 0);
        try {
            return SpriteSheets147.indexOf(item.getIconIndex(stack), false);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code item.getIconIndex(stack, pass)}: the sheet index of one render pass of the stack. */
    public static int itemIconIndexOfPass(legacyconnector.v1_7_10.net.minecraft.item.Item item, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int pass) {
        if (stack == null) return Math.max(item.iconIndex, 0);
        if (!enter(item, 5, stack.getItemDamage() * 4 + pass)) return Math.max(item.iconIndex, 0);
        try {
            return SpriteSheets147.indexOf(item.getIcon(stack, pass), false);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code item.getIconFromDamageForRenderPass(damage, pass)} (ItemPotion and other multi-layer items). */
    public static int itemIconIndexFromDamagePass(legacyconnector.v1_7_10.net.minecraft.item.Item item, int damage, int pass) {
        if (!enter(item, 6, damage * 4 + pass)) return Math.max(item.iconIndex, 0);
        try {
            return SpriteSheets147.indexOf(item.getIconFromDamageForRenderPass(damage, pass), false);
        } finally {
            leave();
        }
    }

    /** 1.4.7 {@code slot.getBackgroundIconIndex()}: the index in /gui/items.png of the icon drawn in an empty slot (-1 for none). */
    public static int slotBackgroundIconIndex(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot) {
        legacyconnector.v1_7_10.net.minecraft.util.IIcon icon = slot.getBackgroundIconIndex();
        return icon == null ? -1 : SpriteSheets147.indexOf(icon, false);
    }

    /** 1.4.7 {@code slot.setBackgroundIconIndex(index)}: the icon at that index of /gui/items.png (a negative index clears it). */
    public static void setSlotBackgroundIconIndex(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, int index) {
        slot.setBackgroundIcon(index < 0 ? null : SpriteSheets147.icon(SpriteSheets147.ITEMS, index, false));
    }

    /** 1.4.7 {@code slot.setBackgroundIconTexture(path)}: the sheet the background index addresses; the block atlas for terrain.png, else the item atlas. */
    public static void setSlotBackgroundIconTexture(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, String path) {
        boolean blocks = path != null && SpriteSheets147.normalize(path).equals(SpriteSheets147.TERRAIN);
        slot.setBackgroundIconTexture(new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation(blocks ? "textures/atlas/blocks.png" : "textures/atlas/items.png"));
    }

    /** 1.4.7 {@code world.isBlockProvidingPowerTo(x, y, z, side)}: whether the block powers its neighbor on that side (1.7.10 answers the strength). */
    public static boolean isBlockProvidingPowerTo(World w, int x, int y, int z, int side) {
        return w.isBlockProvidingPowerTo(x, y, z, side) > 0;
    }

    /** 1.4.7 {@code entity.dropItemWithOffset(itemId, count, offsetY)}. */
    public static legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem dropItemWithOffset(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity,
            int id, int count, float offset) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = Ids.item(id);
        return item == null ? null : entity.dropItemWithOffset(item, count, offset);
    }

    /** 1.4.7 {@code chunk.setBlockID(x, y, z, id)}: the block with metadata 0, true when something changed. */
    public static boolean chunkSetBlockID(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk chunk, int x, int y, int z, int id) {
        return chunk.func_150807_a(x, y, z, block(id), 0);
    }

    /** 1.4.7 {@code stack.onBlockDestroyed(world, blockId, x, y, z, player)}. */
    public static void stackOnBlockDestroyed(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, World w, int id, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        stack.onBlockDestroyed(w, block(id), x, y, z, player);
    }

    /** 1.4.7 {@code settings.renderDistance}: 0 far (16 chunks) to 3 tiny (2 chunks); 1.7.10 stores the chunk count. */
    public static int renderDistance(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        int chunks = settings.renderDistanceChunks;
        return chunks >= 16 ? 0 : chunks >= 8 ? 1 : chunks >= 4 ? 2 : 3;
    }

    public static void setRenderDistance(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings, int level) {
        settings.renderDistanceChunks = new int[] {16, 8, 4, 2}[Math.max(0, Math.min(3, level))];
    }

    /** 1.4.7 {@code renderBlocks.overrideBlockTexture = index}: draw every face with that tile of terrain.png (-1 for the block's own). */
    public static void setOverrideBlockTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks rb, int index) {
        rb.overrideBlockTexture = index < 0 ? null : SpriteSheets147.icon(SpriteSheets147.TERRAIN, index, true);
    }

    /** 1.4.7 {@code spawner.yaw} / {@code yaw2}: the cage's spin angle and its previous value. */
    public static void setSpawnerYaw(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityMobSpawner spawner, double yaw) {
        spawner.getSpawnerBaseLogic().field_98287_c = yaw;
    }

    public static void setSpawnerYaw2(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityMobSpawner spawner, double yaw) {
        spawner.getSpawnerBaseLogic().field_98284_d = yaw;
    }

    /** 1.4.7 {@code spawner.delay} (the countdown to the next spawn; it lives in the spawner logic since 1.6). */
    public static void setSpawnerDelay(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityMobSpawner spawner, int delay) {
        spawner.getSpawnerBaseLogic().spawnDelay = delay;
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
