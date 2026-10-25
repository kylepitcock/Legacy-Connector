package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The entries Forge registers for vanilla blocks and items before any mod does (recipes of mods name them, e.g. EnderIO's 'gravel'). */
final class VanillaOres {
    private static final class V {
        static Block block(String field) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.block(field);
        }
    }

    private static final int ANY = OreDictionary.WILDCARD_VALUE;

    private VanillaOres() {}

    static void register() {
        b("logWood", Blocks.LOG, ANY);
        b("logWood", Blocks.LOG2, ANY);
        b("plankWood", Blocks.PLANKS, ANY);
        b("slabWood", V.block("WOODEN_SLAB"), ANY);
        b("stairWood", Blocks.OAK_STAIRS, ANY);
        b("stairWood", Blocks.SPRUCE_STAIRS, ANY);
        b("stairWood", Blocks.BIRCH_STAIRS, ANY);
        b("stairWood", Blocks.JUNGLE_STAIRS, ANY);
        b("stairWood", Blocks.ACACIA_STAIRS, ANY);
        b("stairWood", Blocks.DARK_OAK_STAIRS, ANY);
        i("stickWood", Items.STICK, 0);
        b("treeSapling", Blocks.SAPLING, ANY);
        b("treeLeaves", V.block("LEAVES"), ANY);
        b("treeLeaves", V.block("LEAVES2"), ANY);
        b("vine", Blocks.VINE, 0);
        b("oreGold", Blocks.GOLD_ORE, 0);
        b("oreIron", Blocks.IRON_ORE, 0);
        b("oreLapis", Blocks.LAPIS_ORE, 0);
        b("oreDiamond", Blocks.DIAMOND_ORE, 0);
        b("oreRedstone", Blocks.REDSTONE_ORE, 0);
        b("oreEmerald", Blocks.EMERALD_ORE, 0);
        b("oreQuartz", Blocks.QUARTZ_ORE, 0);
        b("oreCoal", Blocks.COAL_ORE, 0);
        i("gemDiamond", Items.DIAMOND, 0);
        i("gemEmerald", Items.EMERALD, 0);
        i("gemQuartz", Items.QUARTZ, 0);
        i("gemLapis", Items.DYE, 4);
        i("dustRedstone", Items.REDSTONE, 0);
        i("dustGlowstone", Items.GLOWSTONE_DUST, 0);
        i("glowstone", Items.GLOWSTONE_DUST, 0);
        b("glowstone", Blocks.GLOWSTONE, 0);
        b("blockGold", Blocks.GOLD_BLOCK, 0);
        b("blockIron", Blocks.IRON_BLOCK, 0);
        b("blockLapis", Blocks.LAPIS_BLOCK, 0);
        b("blockDiamond", Blocks.DIAMOND_BLOCK, 0);
        b("blockRedstone", Blocks.REDSTONE_BLOCK, 0);
        b("blockEmerald", Blocks.EMERALD_BLOCK, 0);
        b("blockQuartz", Blocks.QUARTZ_BLOCK, ANY);
        b("blockCoal", Blocks.COAL_BLOCK, 0);
        i("cropWheat", Items.WHEAT, 0);
        i("cropPotato", Items.POTATO, 0);
        i("cropCarrot", Items.CARROT, 0);
        i("cropNetherWart", Items.NETHER_WART, 0);
        i("sugarcane", Items.REEDS, 0);
        b("blockCactus", V.block("CACTUS"), 0);
        i("paper", Items.PAPER, 0);
        i("slimeball", Items.SLIME_BALL, 0);
        i("enderpearl", Items.ENDER_PEARL, 0);
        i("bone", Items.BONE, 0);
        i("gunpowder", Items.GUNPOWDER, 0);
        i("string", Items.STRING, 0);
        i("netherStar", Items.NETHER_STAR, 0);
        i("leather", Items.LEATHER, 0);
        i("feather", Items.FEATHER, 0);
        i("egg", Items.EGG, 0);
        b("stone", Blocks.STONE, 0);
        b("cobblestone", Blocks.COBBLESTONE, ANY);
        b("gravel", Blocks.GRAVEL, ANY);
        b("sand", V.block("SAND"), ANY);
        b("sandstone", Blocks.SANDSTONE, ANY);
        b("netherrack", Blocks.NETHERRACK, 0);
        b("obsidian", Blocks.OBSIDIAN, 0);
        b("glass", Blocks.GLASS, ANY);
        b("paneGlass", Blocks.GLASS_PANE, ANY);
        b("blockGlass", Blocks.GLASS, ANY);
        b("blockGlassColorless", Blocks.GLASS, ANY);
        b("blockGlass", V.block("STAINED_GLASS"), ANY);
        b("paneGlass", V.block("STAINED_GLASS_PANE"), ANY);
        b("clay", Blocks.CLAY, 0);
        i("clay", Items.CLAY_BALL, 0);
        b("blockSlime", Blocks.SLIME_BLOCK, 0);
        i("ingotIron", Items.IRON_INGOT, 0);
        i("ingotGold", Items.GOLD_INGOT, 0);
        i("ingotBrick", Items.BRICK, 0);
        i("ingotBrickNether", Items.NETHERBRICK, 0);
        i("nuggetGold", Items.GOLD_NUGGET, 0);
        i("nuggetIron", Items.IRON_NUGGET, 0);
        i("gemPrismarine", Items.PRISMARINE_SHARD, 0);
        i("dustPrismarine", Items.PRISMARINE_CRYSTALS, 0);
        b("blockPrismarine", Blocks.PRISMARINE, 0);
        b("blockPrismarineBrick", Blocks.PRISMARINE, 1);
        b("blockPrismarineDark", Blocks.PRISMARINE, 2);
        b("chest", V.block("CHEST"), ANY);
        b("chestWood", V.block("CHEST"), ANY);
        b("chestTrapped", Blocks.TRAPPED_CHEST, ANY);
        b("chestEnder", Blocks.ENDER_CHEST, ANY);
        b("workbench", Blocks.CRAFTING_TABLE, ANY);
        b("dirt", Blocks.DIRT, ANY);
        b("grass", V.block("GRASS"), ANY);
        b("endstone", Blocks.END_STONE, 0);
        b("bookshelf", Blocks.BOOKSHELF, 0);
        b("fenceWood", Blocks.OAK_FENCE, 0);
        b("fenceGateWood", Blocks.OAK_FENCE_GATE, 0);
        String[] dyes = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
        for (int meta = 0; meta < dyes.length; meta++) {
            i("dye" + dyes[meta], Items.DYE, meta);
            b("blockGlass" + dyes[meta], V.block("STAINED_GLASS"), 15 - meta);
        }
        i("dye", Items.DYE, ANY);
    }

    private static void b(String name, Block block, int meta) {
        try {
            if (block != null) OreDictionary.registerOre(name, new ItemStack(Item.getItemFromBlock(block), 1, meta));
        } catch (RuntimeException | LinkageError e) {
            // a vanilla entry the shim cannot resolve is left out
        }
    }

    private static void i(String name, Item item, int meta) {
        try {
            if (item != null) OreDictionary.registerOre(name, new ItemStack(item, 1, meta));
        } catch (RuntimeException | LinkageError e) {
            // a vanilla entry the shim cannot resolve is left out
        }
    }
}
