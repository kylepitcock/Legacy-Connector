#!/usr/bin/env python3
"""Generates the 1.12.2 vanilla registries (init/Blocks.java, init/Items.java) from the legacy field list: every static field typed
exactly Block / Item becomes a wrapper over the modern vanilla object. A field whose vanilla class the shim has (TYPED below) is an
instance of that class bound to the modern object (Blocks.SAND is a BlockSand), so its properties and metadata are real; the other
fields typed with subclasses are declared by the loader's synthesizer. usage: tools/gen_vanilla_1122.py"""
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
FIELDS = os.path.join(ROOT, "loader", "src", "main", "resources", "legacyconnector", "legacy", "1.12.2", "fields.txt")
OUT = os.path.join(ROOT, "src", "main", "java", "legacyconnector", "v1_12_2", "net", "minecraft", "init")

B = "legacyconnector.v1_12_2.net.minecraft.block."
M = "legacyconnector.v1_12_2.net.minecraft.block.material.Material."
I = "legacyconnector.v1_12_2.net.minecraft.item."
S = "legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot."

# field -> Java expression making the instance (anonymous subclasses where 1.12's constructor is protected)
TYPED_BLOCKS = {
    "STONE": "new %sBlockStone()" % B,
    "DIRT": "new %sBlockDirt() {}" % B,
    "PLANKS": "new %sBlockPlanks()" % B,
    "SAND": "new %sBlockSand()" % B,
    "SANDSTONE": "new %sBlockSandStone()" % B,
    "RED_SANDSTONE": "new %sBlockRedSandstone()" % B,
    "WATER": "new %sBlockStaticLiquid(%sWATER) {}" % (B, M),
    "LAVA": "new %sBlockStaticLiquid(%sLAVA) {}" % (B, M),
    "FLOWING_WATER": "new %sBlockDynamicLiquid(%sWATER) {}" % (B, M),
    "FLOWING_LAVA": "new %sBlockDynamicLiquid(%sLAVA) {}" % (B, M),
    "GRASS": "new %sBlockGrass() {}" % B,
    "TALLGRASS": "new %sBlockTallGrass() {}" % B,
    "DEADBUSH": "new %sBlockDeadBush() {}" % B,
    "MYCELIUM": "new %sBlockMycelium() {}" % B,
    "FIRE": "new %sBlockFire() {}" % B,
    "CACTUS": "new %sBlockCactus() {}" % B,
    "YELLOW_FLOWER": "%sBlockFlower.EnumFlowerColor.YELLOW.getBlock()" % B,
    "RED_FLOWER": "%sBlockFlower.EnumFlowerColor.RED.getBlock()" % B,
    "DISPENSER": "new %sBlockDispenser() {}" % B,
    "DROPPER": "new %sBlockDropper()" % B,
    "OAK_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "SPRUCE_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "BIRCH_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "JUNGLE_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "ACACIA_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "DARK_OAK_DOOR": "new %sBlockDoor(%sWOOD) {}" % (B, M),
    "IRON_DOOR": "new %sBlockDoor(%sIRON) {}" % (B, M),
    "LOG": "new %sBlockOldLog()" % B,
    "LOG2": "new %sBlockNewLog()" % B,
    "LEAVES": "new %sBlockOldLeaf()" % B,
    "LEAVES2": "new %sBlockNewLeaf()" % B,
    "SAPLING": "new %sBlockSapling() {}" % B,
    "WOOL": "new %sBlockColored(%sCLOTH)" % (B, M),
    "CONCRETE": "new %sBlockColored(%sROCK)" % (B, M),
    "STAINED_HARDENED_CLAY": "new %sBlockStainedHardenedClay()" % B,
    "STAINED_GLASS": "new %sBlockStainedGlass(%sGLASS)" % (B, M),
    "STAINED_GLASS_PANE": "new %sBlockStainedGlassPane()" % B,
    "CONCRETE_POWDER": "new %sBlockConcretePowder()" % B,
    "STONE_SLAB": "new %sBlockHalfStoneSlab()" % B,
    "DOUBLE_STONE_SLAB": "new %sBlockDoubleStoneSlab()" % B,
    "WOODEN_SLAB": "new %sBlockHalfWoodSlab()" % B,
    "DOUBLE_WOODEN_SLAB": "new %sBlockDoubleWoodSlab()" % B,
    "STONE_SLAB2": "new %sBlockHalfStoneSlabNew()" % B,
    "DOUBLE_STONE_SLAB2": "new %sBlockDoubleStoneSlabNew()" % B,
    "PURPUR_SLAB": "new %sBlockPurpurSlab.Half()" % B,
    "PURPUR_DOUBLE_SLAB": "new %sBlockPurpurSlab.Double()" % B,
    "STONEBRICK": "new %sBlockStoneBrick()" % B,
    "QUARTZ_BLOCK": "new %sBlockQuartz()" % B,
    "PRISMARINE": "new %sBlockPrismarine()" % B,
    "RED_MUSHROOM": "new %sBlockMushroom() {}" % B,
    "BROWN_MUSHROOM": "new %sBlockMushroom() {}" % B,
    "RED_MUSHROOM_BLOCK": "new %sBlockHugeMushroom(%sWOOD, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor.RED, Blocks.RED_MUSHROOM)" % (B, M),
    "BROWN_MUSHROOM_BLOCK": "new %sBlockHugeMushroom(%sWOOD, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor.DIRT, Blocks.BROWN_MUSHROOM)" % (B, M),
    "DOUBLE_PLANT": "new %sBlockDoublePlant()" % B,
    "REEDS": "new %sBlockReed() {}" % B,
    "SNOW_LAYER": "new %sBlockSnow() {}" % B,
    "FARMLAND": "new %sBlockFarmland() {}" % B,
    "ICE": "new %sBlockIce()" % B,
    "PACKED_ICE": "new %sBlockPackedIce()" % B,
    "GLOWSTONE": "new %sBlockGlowstone(%sGLASS)" % (B, M),
    "GRASS_PATH": "new %sBlockGrassPath() {}" % B,
    "GRAVEL": "new %sBlockGravel()" % B,
    "BOOKSHELF": "new %sBlockBookshelf()" % B,
    "MAGMA": "new %sBlockMagma()" % B,
    "BONE_BLOCK": "new %sBlockBone()" % B,
    "SPONGE": "new %sBlockSponge() {}" % B,
    "NETHER_WART": "new %sBlockNetherWart() {}" % B,
    "PUMPKIN": "new %sBlockPumpkin() {}" % B,
    "LIT_PUMPKIN": "new %sBlockPumpkin() {}" % B,
    "PUMPKIN_STEM": "new %sBlockStem(Blocks.PUMPKIN) {}" % B,
    "MELON_STEM": "new %sBlockStem(Blocks.MELON_BLOCK) {}" % B,
    "COCOA": "new %sBlockCocoa()" % B,
    "CAKE": "new %sBlockCake() {}" % B,
    "END_ROD": "new %sBlockEndRod() {}" % B,
    "OBSERVER": "new %sBlockObserver()" % B,
    "CHEST": "new %sBlockChest(%sBlockChest.Type.BASIC) {}" % (B, B),
    "TRAPPED_CHEST": "new %sBlockChest(%sBlockChest.Type.TRAP) {}" % (B, B),
    "FLOWER_POT": "new %sBlockFlowerPot()" % B,
    "RAIL": "new %sBlockRail() {}" % B,
    "GOLDEN_RAIL": "new %sBlockRailPowered() {}" % B,
    "ACTIVATOR_RAIL": "new %sBlockRailPowered(true) {}" % B,
    "DETECTOR_RAIL": "new %sBlockRailDetector()" % B,
    "CAULDRON": "new %sBlockCauldron()" % B,
    "HOPPER": "new %sBlockHopper()" % B,
    "FURNACE": "new %sBlockFurnace(false)" % B,
    "LIT_FURNACE": "new %sBlockFurnace(true)" % B,
    "BED": "new %sBlockBed()" % B,
    "PISTON": "new %sBlockPistonBase(false)" % B,
    "STICKY_PISTON": "new %sBlockPistonBase(true)" % B,
    "PISTON_HEAD": "new %sBlockPistonExtension()" % B,
    "PISTON_EXTENSION": "new %sBlockPistonMoving()" % B,
    "PORTAL": "new %sBlockPortal()" % B,
    "REDSTONE_WIRE": "new %sBlockRedstoneWire()" % B,
    "UNPOWERED_REPEATER": "new %sBlockRedstoneRepeater(false) {}" % B,
    "POWERED_REPEATER": "new %sBlockRedstoneRepeater(true) {}" % B,
    "UNPOWERED_COMPARATOR": "new %sBlockRedstoneComparator(false)" % B,
    "POWERED_COMPARATOR": "new %sBlockRedstoneComparator(true)" % B,
    "REDSTONE_BLOCK": "new %sBlockCompressedPowered(%sIRON, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor.TNT)" % (B, M),
}
for color in ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "SILVER", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]:
    TYPED_BLOCKS[color + "_SHULKER_BOX"] = "new %sBlockShulkerBox(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.%s)" % (B, color)

ARMOR = {"LEATHER": ("LEATHER", 0), "CHAINMAIL": ("CHAIN", 1), "IRON": ("IRON", 2), "DIAMOND": ("DIAMOND", 3), "GOLDEN": ("GOLD", 4)}
SLOTS = {"HELMET": "HEAD", "CHESTPLATE": "CHEST", "LEGGINGS": "LEGS", "BOOTS": "FEET"}
TYPED_ITEMS = {
    "BOW": "new %sItemBow()" % I,
    "SHEARS": "new %sItemShears()" % I,
    "POTIONITEM": "new %sItemPotion()" % I,
    "SPLASH_POTION": "new %sItemSplashPotion()" % I,
    "LINGERING_POTION": "new %sItemLingeringPotion()" % I,
}
for tier, (material, index) in ARMOR.items():
    for piece, slot in SLOTS.items():
        TYPED_ITEMS["%s_%s" % (tier, piece)] = "new %sItemArmor(%sItemArmor.ArmorMaterial.%s, %d, %s%s)" % (I, I, material, index, S, slot)


def collect(owner, desc=None):
    out = {}
    for line in open(FIELDS, encoding="utf-8"):
        p = line.split()
        if len(p) >= 4 and p[1] == owner and (desc is None or p[3] == desc):
            out[p[2]] = p[3]
    return out


def shim_type(desc):
    return "legacyconnector.v1_12_2." + desc[1:-1].replace("/", ".").replace("$", ".")


def emit(owner_class, owner, kind, desc, type_name, typed, helper):
    plain = collect(owner, desc)
    every = collect(owner)
    names = sorted(set(plain) | {n for n in typed if n in every})
    lines = ["package legacyconnector.v1_12_2.net.minecraft.init;", "",
             "import org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122;",
             "import org.kylepitcock.legacyconnector.v1_12_2.bridge.VanillaBlocks1122;", "",
             "/** 1.12.2 vanilla %ss (generated by tools/gen_vanilla_1122.py): wrappers over the modern vanilla objects. */" % kind.lower(),
             "public class %s {" % owner_class]
    for n in names:
        if n in typed:
            ftype = type_name if every[n] == desc else shim_type(every[n])
            lines.append("    public static final %s %s = %s;" % (ftype, n, helper(n, typed[n])))
        else:
            lines.append("    public static final %s %s = Vanilla1122.%s(\"%s\");" % (type_name, n, kind.lower(), n))
    lines.append("}")
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, owner_class + ".java"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print(owner_class, len(names))


emit("Blocks", "net/minecraft/init/Blocks", "Block", "Lnet/minecraft/block/Block;", "legacyconnector.v1_12_2.net.minecraft.block.Block",
     TYPED_BLOCKS, lambda n, e: "VanillaBlocks1122.block(\"%s\", %s)" % (n, e))
emit("Items", "net/minecraft/init/Items", "Item", "Lnet/minecraft/item/Item;", "legacyconnector.v1_12_2.net.minecraft.item.Item",
     TYPED_ITEMS, lambda n, e: "VanillaBlocks1122.item(\"%s\", null, %s)" % (n, e))
