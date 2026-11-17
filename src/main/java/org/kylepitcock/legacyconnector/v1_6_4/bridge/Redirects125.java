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

    /** {@code GuiIngame.b()} (Feather {@code clearChat}): empties the chat history; checked in the 1.2.5 client, it cleared its two line lists. */
    public static void clearChat(GuiIngame gui) {
        gui.getChatGUI().clearChatMessages();
    }

    /** {@code GuiIngame.c(key)} (Feather {@code translate}): translates the key with the language table and adds the result as a chat line (the 1.2.5 body). */
    public static void translate(GuiIngame gui, String key) {
        addChatMessage(gui, legacyconnector.v1_7_10.net.minecraft.util.StatCollector.translateToLocal(key));
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

    // ---- FML 2.x (the FML that Forge 3.x for 1.2.5 bundles): members FML 3 moved to TickRegistry, GameRegistry and NetworkRegistry

    /**
     * FML 2.x {@code FMLCommonHandler.registerTickHandler(handler)}: one registration for both sides (FML 3 split it into {@code TickRegistry.registerTickHandler(handler, side)});
     * each side's listener only fires the tick kinds of its own side, so the handler sees what it asked for from {@code ticks()}.
     */
    public static void registerTickHandler(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler fml, legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler handler) {
        if (handler == null) return;
        Ticks164.register(handler, legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.CLIENT);
        Ticks164.register(handler, legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.SERVER);
    }

    /** FML 2.x {@code FMLCommonHandler.registerWorldGenerator(generator)}: the registry call of FML 3, weight 0. */
    public static void registerWorldGenerator(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler fml, legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator generator) {
        if (generator != null) legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.registerWorldGenerator(generator, 0);
    }

    /**
     * FML 2.x {@code FMLCommonHandler.handleWorldGeneration(chunkX, chunkZ, seed, data...)}: the hook the patched chunk provider calls after a chunk is populated;
     * {@code data} is (world, chunk generator, chunk provider). Runs the registered generators on that chunk.
     */
    public static void handleWorldGeneration(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler fml, int chunkX, int chunkZ, long seed, Object[] data) {
        if (data == null || data.length < 3 || !(data[0] instanceof World world)
                || !(data[1] instanceof legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider generator)
                || !(data[2] instanceof legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider provider)) return;
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry.generateWorld(chunkX, chunkZ, world, generator, provider);
    }

    /**
     * FML 2.x {@code FMLCommonHandler.registerChannel(container, channel)}: the mod's custom payloads on {@code channel}. A ModLoader mod gets them in
     * {@code receiveCustomPacket} (the packet-250 model of {@code ModLoader.registerPacketChannel}); other mods register their {@code IPacketHandler} the FML 3 way.
     */
    public static void registerChannel(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler fml, legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container, String channel) {
        Object mod = container == null ? null : container.getMod();
        if (mod instanceof legacyconnector.v1_6_4.net.minecraft.src.BaseMod base) legacyconnector.v1_6_4.net.minecraft.src.ModLoader.registerPacketChannel(base, channel);
    }

    /**
     * {@code RenderEngine.getTextureContents(path)}: the pixels (ARGB, row by row) of a texture file, read from the mods' jars or the game's resources like ModLoader's
     * {@code loadImage}; an unreadable file gives an empty array (1.2.5 returned its missing-texture pixels).
     */
    public static int[] textureContents(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures, String path) {
        try {
            java.awt.image.BufferedImage image = legacyconnector.v1_6_4.net.minecraft.src.ModLoader.loadImage(textures, path);
            if (image == null) return new int[0];
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        } catch (Exception e) {
            return new int[0];
        }
    }

    /** Forge 3.x {@code Item.setTextureFile(file)} returns void ({@code Item} itself in later versions; Java cannot overload on the result). */
    public static void setItemTextureFile(legacyconnector.v1_7_10.net.minecraft.item.Item item, String file) {
        item.setTextureFile(file);
    }

    /** Forge 3.x {@code Block.setTextureFile(file)}, void like the item's. */
    public static void setBlockTextureFile(Block block, String file) {
        block.setTextureFile(file);
    }
}
