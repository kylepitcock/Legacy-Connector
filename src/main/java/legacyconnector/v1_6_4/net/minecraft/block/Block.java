package legacyconnector.v1_6_4.net.minecraft.block;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;

/**
 * Facade of the 1.7.10 shim's {@code Block} for 1.6.4 mods (see {@code Hierarchy.facadeOf}): the numeric-id members 1.6.4 had.
 * The loader redirects {@code block.blockID}, {@code Block.blocksList} and the vanilla statics ({@code Block.dirt}) to the
 * {@code mcc$get$} accessors here, and the id arguments of Block constructors to {@link #mcc$claimId}. Never instantiated.
 */
public final class Block {
    private Block() {}

    /** Block.blockID */
    public static int mcc$get$blockID(legacyconnector.v1_7_10.net.minecraft.block.Block self) {
        return Ids.blockId(self);
    }

    /** Block.blocksList (live mirror of the id table) */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block[] mcc$get$blocksList() {
        return Ids.blocksList();
    }

    /** The id a Block constructor was given ({@code super(id, material)}). */
    public static void mcc$claimId(legacyconnector.v1_7_10.net.minecraft.block.Block self, int id) {
        Ids.claimBlock(self, id);
    }

    /** {@code new ItemStack(blockId, ...)} style conversion: the block of an id (null for air or an unused id). */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$fromId(int id) {
        return Ids.block(id);
    }

    /** {@code new ItemBlock(id)}: an ItemBlock constructor gets its block's id minus 256. */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$fromItemCtorId(int constructorId) {
        return Ids.block(constructorId + 256);
    }

    // BEGIN GENERATED (tools/gen_statics_164.py): vanilla statics of Block in 1.6.4
    /** Block.anvil = Blocks.anvil */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$anvil() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.anvil);
    }

    /** Block.beacon = Blocks.beacon */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockBeacon mcc$get$beacon() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockBeacon.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.beacon);
    }

    /** Block.bed = Blocks.bed */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$bed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.bed);
    }

    /** Block.bedrock = Blocks.bedrock */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$bedrock() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.bedrock);
    }

    /** Block.blockClay = Blocks.clay */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockClay() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.clay);
    }

    /** Block.blockDiamond = Blocks.diamond_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_block);
    }

    /** Block.blockEmerald = Blocks.emerald_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockEmerald() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.emerald_block);
    }

    /** Block.blockGold = Blocks.gold_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_block);
    }

    /** Block.blockIron = Blocks.iron_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_block);
    }

    /** Block.blockLapis = Blocks.lapis_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockLapis() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lapis_block);
    }

    /** Block.blockNetherQuartz = Blocks.quartz_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockNetherQuartz() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.quartz_block);
    }

    /** Block.blockRedstone = Blocks.redstone_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockRedstone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_block);
    }

    /** Block.blockSnow = Blocks.snow */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$blockSnow() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.snow);
    }

    /** Block.bookShelf = Blocks.bookshelf */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$bookShelf() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.bookshelf);
    }

    /** Block.brewingStand = Blocks.brewing_stand */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$brewingStand() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.brewing_stand);
    }

    /** Block.brick = Blocks.brick_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$brick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.brick_block);
    }

    /** Block.cactus = Blocks.cactus */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cactus() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cactus);
    }

    /** Block.cake = Blocks.cake */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cake() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cake);
    }

    /** Block.carpet = Blocks.carpet */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$carpet() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.carpet);
    }

    /** Block.carrot = Blocks.carrots */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$carrot() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.carrots);
    }

    /** Block.cauldron = Blocks.cauldron */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockCauldron mcc$get$cauldron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockCauldron.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cauldron);
    }

    /** Block.chest = Blocks.chest */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockChest mcc$get$chest() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockChest.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.chest);
    }

    /** Block.chestTrapped = Blocks.trapped_chest */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$chestTrapped() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.trapped_chest);
    }

    /** Block.cloth = Blocks.wool */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cloth() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wool);
    }

    /** Block.coalBlock = Blocks.coal_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$coalBlock() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.coal_block);
    }

    /** Block.cobblestone = Blocks.cobblestone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cobblestone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cobblestone);
    }

    /** Block.cobblestoneMossy = Blocks.mossy_cobblestone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cobblestoneMossy() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.mossy_cobblestone);
    }

    /** Block.cobblestoneWall = Blocks.cobblestone_wall */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cobblestoneWall() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cobblestone_wall);
    }

    /** Block.cocoaPlant = Blocks.cocoa */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$cocoaPlant() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.cocoa);
    }

    /** Block.commandBlock = Blocks.command_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$commandBlock() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.command_block);
    }

    /** Block.crops = Blocks.wheat */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$crops() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wheat);
    }

    /** Block.daylightSensor = Blocks.daylight_detector */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockDaylightDetector mcc$get$daylightSensor() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockDaylightDetector.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.daylight_detector);
    }

    /** Block.deadBush = Blocks.deadbush */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockDeadBush mcc$get$deadBush() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockDeadBush.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.deadbush);
    }

    /** Block.dirt = Blocks.dirt */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$dirt() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt);
    }

    /** Block.dispenser = Blocks.dispenser */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$dispenser() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dispenser);
    }

    /** Block.doorIron = Blocks.iron_door */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$doorIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_door);
    }

    /** Block.doorWood = Blocks.wooden_door */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$doorWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wooden_door);
    }

    /** Block.dragonEgg = Blocks.dragon_egg */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$dragonEgg() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dragon_egg);
    }

    /** Block.dropper = Blocks.dropper */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$dropper() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.dropper);
    }

    /** Block.enchantmentTable = Blocks.enchanting_table */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$enchantmentTable() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.enchanting_table);
    }

    /** Block.endPortal = Blocks.end_portal */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$endPortal() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.end_portal);
    }

    /** Block.endPortalFrame = Blocks.end_portal_frame */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$endPortalFrame() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.end_portal_frame);
    }

    /** Block.enderChest = Blocks.ender_chest */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$enderChest() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.ender_chest);
    }

    /** Block.fence = Blocks.fence */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$fence() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.fence);
    }

    /** Block.fenceGate = Blocks.fence_gate */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$fenceGate() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.fence_gate);
    }

    /** Block.fenceIron = Blocks.iron_bars */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$fenceIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_bars);
    }

    /** Block.fire = Blocks.fire */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockFire mcc$get$fire() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockFire.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire);
    }

    /** Block.flowerPot = Blocks.flower_pot */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$flowerPot() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.flower_pot);
    }

    /** Block.furnaceBurning = Blocks.lit_furnace */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$furnaceBurning() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_furnace);
    }

    /** Block.furnaceIdle = Blocks.furnace */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$furnaceIdle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.furnace);
    }

    /** Block.glass = Blocks.glass */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$glass() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.glass);
    }

    /** Block.glowStone = Blocks.glowstone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$glowStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.glowstone);
    }

    /** Block.grass = Blocks.grass */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockGrass mcc$get$grass() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockGrass.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass);
    }

    /** Block.gravel = Blocks.gravel */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$gravel() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gravel);
    }

    /** Block.hardenedClay = Blocks.hardened_clay */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$hardenedClay() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.hardened_clay);
    }

    /** Block.hay = Blocks.hay_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$hay() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.hay_block);
    }

    /** Block.hopperBlock = Blocks.hopper */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockHopper mcc$get$hopperBlock() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockHopper.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.hopper);
    }

    /** Block.ice = Blocks.ice */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$ice() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.ice);
    }

    /** Block.jukebox = Blocks.jukebox */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$jukebox() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.jukebox);
    }

    /** Block.ladder = Blocks.ladder */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$ladder() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.ladder);
    }

    /** Block.lavaMoving = Blocks.flowing_lava */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid mcc$get$lavaMoving() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_lava);
    }

    /** Block.lavaStill = Blocks.lava */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$lavaStill() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lava);
    }

    /** Block.leaves = Blocks.leaves */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockLeaves mcc$get$leaves() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockLeaves.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.leaves);
    }

    /** Block.lever = Blocks.lever */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$lever() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lever);
    }

    /** Block.melon = Blocks.melon_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$melon() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.melon_block);
    }

    /** Block.melonStem = Blocks.melon_stem */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$melonStem() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.melon_stem);
    }

    /** Block.mobSpawner = Blocks.mob_spawner */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$mobSpawner() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.mob_spawner);
    }

    /** Block.mushroomBrown = Blocks.brown_mushroom */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockFlower mcc$get$mushroomBrown() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockFlower.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.brown_mushroom);
    }

    /** Block.mushroomCapBrown = Blocks.brown_mushroom_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$mushroomCapBrown() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.brown_mushroom_block);
    }

    /** Block.mushroomCapRed = Blocks.red_mushroom_block */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$mushroomCapRed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_mushroom_block);
    }

    /** Block.mushroomRed = Blocks.red_mushroom */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockFlower mcc$get$mushroomRed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockFlower.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_mushroom);
    }

    /** Block.music = Blocks.noteblock */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$music() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.noteblock);
    }

    /** Block.mycelium = Blocks.mycelium */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockMycelium mcc$get$mycelium() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockMycelium.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.mycelium);
    }

    /** Block.netherBrick = Blocks.nether_brick */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$netherBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.nether_brick);
    }

    /** Block.netherFence = Blocks.nether_brick_fence */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$netherFence() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.nether_brick_fence);
    }

    /** Block.netherStalk = Blocks.nether_wart */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$netherStalk() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.nether_wart);
    }

    /** Block.netherrack = Blocks.netherrack */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$netherrack() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.netherrack);
    }

    /** Block.obsidian = Blocks.obsidian */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$obsidian() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.obsidian);
    }

    /** Block.oreCoal = Blocks.coal_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreCoal() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.coal_ore);
    }

    /** Block.oreDiamond = Blocks.diamond_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_ore);
    }

    /** Block.oreEmerald = Blocks.emerald_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreEmerald() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.emerald_ore);
    }

    /** Block.oreGold = Blocks.gold_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_ore);
    }

    /** Block.oreIron = Blocks.iron_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_ore);
    }

    /** Block.oreLapis = Blocks.lapis_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreLapis() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lapis_ore);
    }

    /** Block.oreNetherQuartz = Blocks.quartz_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreNetherQuartz() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.quartz_ore);
    }

    /** Block.oreRedstone = Blocks.redstone_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreRedstone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_ore);
    }

    /** Block.oreRedstoneGlowing = Blocks.lit_redstone_ore */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$oreRedstoneGlowing() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_redstone_ore);
    }

    /** Block.pistonBase = Blocks.piston */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase mcc$get$pistonBase() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston);
    }

    /** Block.pistonExtension = Blocks.piston_head */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockPistonExtension mcc$get$pistonExtension() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockPistonExtension.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston_head);
    }

    /** Block.pistonMoving = Blocks.piston_extension */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockPistonMoving mcc$get$pistonMoving() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockPistonMoving.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.piston_extension);
    }

    /** Block.pistonStickyBase = Blocks.sticky_piston */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase mcc$get$pistonStickyBase() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sticky_piston);
    }

    /** Block.planks = Blocks.planks */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$planks() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.planks);
    }

    /** Block.plantRed = Blocks.red_flower */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockFlower mcc$get$plantRed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockFlower.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_flower);
    }

    /** Block.plantYellow = Blocks.yellow_flower */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockFlower mcc$get$plantYellow() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockFlower.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.yellow_flower);
    }

    /** Block.portal = Blocks.portal */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockPortal mcc$get$portal() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockPortal.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.portal);
    }

    /** Block.potato = Blocks.potatoes */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$potato() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.potatoes);
    }

    /** Block.pressurePlateGold = Blocks.light_weighted_pressure_plate */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pressurePlateGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.light_weighted_pressure_plate);
    }

    /** Block.pressurePlateIron = Blocks.heavy_weighted_pressure_plate */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pressurePlateIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.heavy_weighted_pressure_plate);
    }

    /** Block.pressurePlatePlanks = Blocks.wooden_pressure_plate */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pressurePlatePlanks() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wooden_pressure_plate);
    }

    /** Block.pressurePlateStone = Blocks.stone_pressure_plate */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pressurePlateStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_pressure_plate);
    }

    /** Block.pumpkin = Blocks.pumpkin */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pumpkin() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.pumpkin);
    }

    /** Block.pumpkinLantern = Blocks.lit_pumpkin */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pumpkinLantern() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_pumpkin);
    }

    /** Block.pumpkinStem = Blocks.pumpkin_stem */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$pumpkinStem() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.pumpkin_stem);
    }

    /** Block.rail = Blocks.rail */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$rail() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.rail);
    }

    /** Block.railActivator = Blocks.activator_rail */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$railActivator() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.activator_rail);
    }

    /** Block.railDetector = Blocks.detector_rail */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$railDetector() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.detector_rail);
    }

    /** Block.railPowered = Blocks.golden_rail */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$railPowered() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.golden_rail);
    }

    /** Block.redstoneLampActive = Blocks.lit_redstone_lamp */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$redstoneLampActive() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.lit_redstone_lamp);
    }

    /** Block.redstoneLampIdle = Blocks.redstone_lamp */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$redstoneLampIdle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_lamp);
    }

    /** Block.redstoneRepeaterActive = Blocks.powered_repeater */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneRepeater mcc$get$redstoneRepeaterActive() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneRepeater.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.powered_repeater);
    }

    /** Block.redstoneRepeaterIdle = Blocks.unpowered_repeater */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneRepeater mcc$get$redstoneRepeaterIdle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneRepeater.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.unpowered_repeater);
    }

    /** Block.redstoneWire = Blocks.redstone_wire */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneWire mcc$get$redstoneWire() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockRedstoneWire.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_wire);
    }

    /** Block.reed = Blocks.reeds */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$reed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.reeds);
    }

    /** Block.sand = Blocks.sand */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$sand() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sand);
    }

    /** Block.sandStone = Blocks.sandstone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$sandStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sandstone);
    }

    /** Block.sapling = Blocks.sapling */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$sapling() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sapling);
    }

    /** Block.signPost = Blocks.standing_sign */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$signPost() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.standing_sign);
    }

    /** Block.signWall = Blocks.wall_sign */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$signWall() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wall_sign);
    }

    /** Block.silverfish = Blocks.monster_egg */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$silverfish() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.monster_egg);
    }

    /** Block.skull = Blocks.skull */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$skull() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.skull);
    }

    /** Block.slowSand = Blocks.soul_sand */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$slowSand() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.soul_sand);
    }

    /** Block.snow = Blocks.snow_layer */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$snow() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.snow_layer);
    }

    /** Block.sponge = Blocks.sponge */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$sponge() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sponge);
    }

    /** Block.stainedClay = Blocks.stained_hardened_clay */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stainedClay() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stained_hardened_clay);
    }

    /** Block.stairsBrick = Blocks.brick_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.brick_stairs);
    }

    /** Block.stairsCobblestone = Blocks.stone_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsCobblestone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_stairs);
    }

    /** Block.stairsNetherBrick = Blocks.nether_brick_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsNetherBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.nether_brick_stairs);
    }

    /** Block.stairsNetherQuartz = Blocks.quartz_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsNetherQuartz() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.quartz_stairs);
    }

    /** Block.stairsSandStone = Blocks.sandstone_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsSandStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.sandstone_stairs);
    }

    /** Block.stairsStoneBrick = Blocks.stone_brick_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsStoneBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_brick_stairs);
    }

    /** Block.stairsWoodBirch = Blocks.birch_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsWoodBirch() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.birch_stairs);
    }

    /** Block.stairsWoodJungle = Blocks.jungle_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsWoodJungle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.jungle_stairs);
    }

    /** Block.stairsWoodOak = Blocks.oak_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsWoodOak() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.oak_stairs);
    }

    /** Block.stairsWoodSpruce = Blocks.spruce_stairs */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stairsWoodSpruce() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.spruce_stairs);
    }

    /** Block.stone = Blocks.stone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone);
    }

    /** Block.stoneBrick = Blocks.stonebrick */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stoneBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stonebrick);
    }

    /** Block.stoneButton = Blocks.stone_button */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$stoneButton() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_button);
    }

    /** Block.stoneDoubleSlab = Blocks.double_stone_slab */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockSlab mcc$get$stoneDoubleSlab() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockSlab.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.double_stone_slab);
    }

    /** Block.stoneSingleSlab = Blocks.stone_slab */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockSlab mcc$get$stoneSingleSlab() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockSlab.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone_slab);
    }

    /** Block.tallGrass = Blocks.tallgrass */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockTallGrass mcc$get$tallGrass() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockTallGrass.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.tallgrass);
    }

    /** Block.thinGlass = Blocks.glass_pane */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$thinGlass() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.glass_pane);
    }

    /** Block.tilledField = Blocks.farmland */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$tilledField() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland);
    }

    /** Block.tnt = Blocks.tnt */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$tnt() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.tnt);
    }

    /** Block.torchRedstoneActive = Blocks.redstone_torch */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$torchRedstoneActive() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_torch);
    }

    /** Block.torchRedstoneIdle = Blocks.unlit_redstone_torch */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$torchRedstoneIdle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.unlit_redstone_torch);
    }

    /** Block.torchWood = Blocks.torch */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$torchWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.torch);
    }

    /** Block.trapdoor = Blocks.trapdoor */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$trapdoor() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.trapdoor);
    }

    /** Block.tripWire = Blocks.tripwire */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$tripWire() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire);
    }

    /** Block.tripWireSource = Blocks.tripwire_hook */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockTripWireHook mcc$get$tripWireSource() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockTripWireHook.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire_hook);
    }

    /** Block.vine = Blocks.vine */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$vine() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.vine);
    }

    /** Block.waterMoving = Blocks.flowing_water */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid mcc$get$waterMoving() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockLiquid.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.flowing_water);
    }

    /** Block.waterStill = Blocks.water */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$waterStill() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.water);
    }

    /** Block.waterlily = Blocks.waterlily */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$waterlily() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.waterlily);
    }

    /** Block.web = Blocks.web */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$web() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.web);
    }

    /** Block.whiteStone = Blocks.end_stone */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$whiteStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.end_stone);
    }

    /** Block.wood = Blocks.log */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$wood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.log);
    }

    /** Block.woodDoubleSlab = Blocks.double_wooden_slab */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockSlab mcc$get$woodDoubleSlab() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockSlab.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.double_wooden_slab);
    }

    /** Block.woodSingleSlab = Blocks.wooden_slab */
    public static legacyconnector.v1_7_10.net.minecraft.block.BlockSlab mcc$get$woodSingleSlab() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.BlockSlab.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wooden_slab);
    }

    /** Block.woodenButton = Blocks.wooden_button */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$woodenButton() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.wooden_button);
    }

    /** Block.workbench = Blocks.crafting_table */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$get$workbench() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.init.Blocks.crafting_table);
    }

    // END GENERATED
}
