package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.io.File;
import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool;
import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiIngame;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Call-site redirects of Minecraft 1.2.5 (the {@code R} rows of {@code forward/1.4.7/member-manual.tsv} with a {@code mcc125$} name, and the
 * Forge 3.x patches of game classes): members of 1.2.5 whose 1.3 successor has another shape or whose Forge patch the shim has no counterpart of. The
 * 1.2.5 to 1.4.7 name join leaves such members as {@code mcc125$<Feather name>} (side independent: client and server use different letters); each is answered here with what the 1.2.5 method did, written against the
 * shim.
 */
public final class Redirects125 {
    private Redirects125() {}

    private static Block block(int id) {
        Block b = id <= 0 ? null : Ids.block(id);
        return b != null ? b : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
    }

    /** {@code FontRenderer.b(text, x, y, color)}: draws without a shadow (the one with a shadow was {@code a}). */
    public static void drawString(FontRenderer font, String text, int x, int y, int color) {
        font.drawString(text, x, y, color);
    }

    /** {@code Container.a(slot)}: adds a slot to the container's slot list. */
    public static void addSlot(Container container, Slot slot) {
        container.mcc$addSlot(slot);
    }

    /** {@code Container.a(slot, button, shift, player)}: a click on a slot; {@code shift} became mode 1 of 1.3's click modes. */
    public static ItemStack slotClick(Container container, int slot, int button, boolean shift, EntityPlayer player) {
        return container.slotClick(slot, button, shift ? 1 : 0, player);
    }

    /** {@code Minecraft.l()}: true while connected to a server that is not the integrated one (the layer's integrated server counts as single player). */
    public static boolean isMultiplayer(Minecraft mc) {
        return !mc.isSingleplayer();
    }

    /** {@code EntityPlayer.e(x, y, z)}: whether the player may change the block (adventure mode came with 1.3). */
    public static boolean canPlayerEdit(EntityPlayer player, int x, int y, int z) {
        return true;
    }

    public static void swingItem(EntityPlayer player) {
        player.swingItem();
    }

    /** {@code GuiIngame.a(text)}: adds a line to the chat. */
    public static void addChatMessage(GuiIngame gui, String text) {
        gui.getChatGUI().printChatMessage(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(text));
    }

    /** {@code World.a(blockId, x, y, z, ignoreEntities, side)}: whether a block can be placed there (no placing entity). */
    public static boolean canPlace(World world, int blockId, int x, int y, int z, boolean ignoreEntities, int side) {
        return world.canPlaceEntityOnSide(block(blockId), x, y, z, ignoreEntities, side, null, null);
    }

    /** {@code World.a(entity, x, y, z, power)}: an explosion without fire. */
    public static legacyconnector.v1_7_10.net.minecraft.world.Explosion explode(World world, Entity entity, double x, double y, double z, float power) {
        return world.createExplosion(entity, x, y, z, power, true);
    }

    /** {@code World.isBlockSolidOnSide(x, y, z, side)} (Forge 3.x's int side). */
    public static boolean isBlockSolidOnSide(World world, int x, int y, int z, int side) {
        return world.isSideSolid(x, y, z, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.getOrientation(side));
    }

    /** {@code ItemStack.q()}: the tooltip lines of the stack. */
    public static java.util.List<String> tooltip(ItemStack stack) {
        EntityPlayer player = null;
        try {
            if (legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler.instance().getSide() == legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.CLIENT) {
                player = Minecraft.mcc$get$thePlayer(Minecraft.getMinecraft());
            }
        } catch (RuntimeException | LinkageError e) {
            // no client: the tooltip is built without a player
        }
        return stack.getTooltip(player, false);
    }

    /** {@code ItemStack.c(other)}: same item and damage. */
    public static boolean isItemEqual(ItemStack stack, ItemStack other) {
        return stack.isItemEqual(other);
    }

    /** Forge 3.x {@code FurnaceRecipes.addSmelting(id, metadata, result)}: recipes for one damage value of an item (no experience). */
    public static void addSmelting(FurnaceRecipes recipes, int id, int metadata, ItemStack result) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = Ids.item(id);
        if (item != null) recipes.addSmeltingRecipe(new ItemStack(item, 1, metadata), result, 0.0F);
    }

    // The profiler was a static singleton in 1.2.5; the layer has none worth feeding.

    public static void startSection(String name) {}

    public static void endSection() {}

    public static void endStartSection(String name) {}

    /** {@code Minecraft.installResource(path, file)}: a sound file a mod brings into the game ({@code sound/}, {@code newsound/}, {@code streaming/}, {@code music/}, {@code newmusic/}). */
    public static void installResource(Minecraft mc, String path, File file) {
        int slash = path.indexOf('/');
        if (slash < 0) return;
        String category = path.substring(0, slash);
        String name = path.substring(slash + 1);
        switch (category) {
            case "sound", "newsound" -> SoundPool.SOUNDS.addSound(name, file);
            case "streaming" -> SoundPool.STREAMING.addSound(name, file);
            case "music", "newmusic" -> SoundPool.MUSIC.addSound(name, file);
            default -> {
                // not a sound category
            }
        }
    }
}
