package legacyconnector.v1_6_4.net.minecraft.item;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.init.Items;

/**
 * Facade of the 1.7.10 shim's {@code Item} for 1.6.4 mods (see {@code Hierarchy.facadeOf}): {@code item.itemID},
 * {@code Item.itemsList}, the vanilla statics ({@code Item.ingotIron}) and the id claim of Item constructors. Never instantiated.
 */
public final class Item {
    private Item() {}

    /** Item.itemID */
    public static int mcc$get$itemID(legacyconnector.v1_7_10.net.minecraft.item.Item self) {
        return Ids.itemId(self);
    }

    /** Item.itemsList (live mirror of the id table) */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item[] mcc$get$itemsList() {
        return Ids.itemsList();
    }

    /** The id an Item constructor was given: {@code new Item(id)} puts the item at 256 + id. */
    public static void mcc$claimId(legacyconnector.v1_7_10.net.minecraft.item.Item self, int constructorId) {
        Ids.claimItem(self, constructorId);
    }

    /** {@code new ItemStack(itemId, ...)}: the item of an id (null for 0 or an unused id). */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$fromId(int id) {
        return Ids.item(id);
    }

    // BEGIN GENERATED (tools/gen_statics_164.py): vanilla statics of Item in 1.6.4
    /** Item.appleGold = Items.golden_apple */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$appleGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_apple);
    }

    /** Item.appleRed = Items.apple */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$appleRed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.apple);
    }

    /** Item.arrow = Items.arrow */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$arrow() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.arrow);
    }

    /** Item.axeDiamond = Items.diamond_axe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$axeDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_axe);
    }

    /** Item.axeGold = Items.golden_axe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$axeGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_axe);
    }

    /** Item.axeIron = Items.iron_axe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$axeIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_axe);
    }

    /** Item.axeStone = Items.stone_axe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$axeStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stone_axe);
    }

    /** Item.axeWood = Items.wooden_axe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$axeWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_axe);
    }

    /** Item.bakedPotato = Items.baked_potato */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bakedPotato() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.baked_potato);
    }

    /** Item.bed = Items.bed */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bed);
    }

    /** Item.beefCooked = Items.cooked_beef */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$beefCooked() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cooked_beef);
    }

    /** Item.beefRaw = Items.beef */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$beefRaw() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.beef);
    }

    /** Item.blazePowder = Items.blaze_powder */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$blazePowder() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.blaze_powder);
    }

    /** Item.blazeRod = Items.blaze_rod */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$blazeRod() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.blaze_rod);
    }

    /** Item.boat = Items.boat */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$boat() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.boat);
    }

    /** Item.bone = Items.bone */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bone);
    }

    /** Item.book = Items.book */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$book() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.book);
    }

    /** Item.bootsChain = Items.chainmail_boots */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$bootsChain() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chainmail_boots);
    }

    /** Item.bootsDiamond = Items.diamond_boots */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$bootsDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_boots);
    }

    /** Item.bootsGold = Items.golden_boots */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$bootsGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_boots);
    }

    /** Item.bootsIron = Items.iron_boots */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$bootsIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_boots);
    }

    /** Item.bootsLeather = Items.leather_boots */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$bootsLeather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.leather_boots);
    }

    /** Item.bow = Items.bow */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemBow mcc$get$bow() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemBow.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bow);
    }

    /** Item.bowlEmpty = Items.bowl */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bowlEmpty() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bowl);
    }

    /** Item.bowlSoup = Items.mushroom_stew */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bowlSoup() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.mushroom_stew);
    }

    /** Item.bread = Items.bread */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bread() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bread);
    }

    /** Item.brewingStand = Items.brewing_stand */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$brewingStand() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.brewing_stand);
    }

    /** Item.brick = Items.brick */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$brick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.brick);
    }

    /** Item.bucketEmpty = Items.bucket */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bucketEmpty() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.bucket);
    }

    /** Item.bucketLava = Items.lava_bucket */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bucketLava() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.lava_bucket);
    }

    /** Item.bucketMilk = Items.milk_bucket */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bucketMilk() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.milk_bucket);
    }

    /** Item.bucketWater = Items.water_bucket */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$bucketWater() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.water_bucket);
    }

    /** Item.cake = Items.cake */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$cake() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cake);
    }

    /** Item.carrot = Items.carrot */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$carrot() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.carrot);
    }

    /** Item.carrotOnAStick = Items.carrot_on_a_stick */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$carrotOnAStick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.carrot_on_a_stick);
    }

    /** Item.cauldron = Items.cauldron */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$cauldron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cauldron);
    }

    /** Item.chickenCooked = Items.cooked_chicken */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$chickenCooked() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cooked_chicken);
    }

    /** Item.chickenRaw = Items.chicken */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$chickenRaw() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chicken);
    }

    /** Item.clay = Items.clay_ball */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$clay() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.clay_ball);
    }

    /** Item.coal = Items.coal */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$coal() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.coal);
    }

    /** Item.comparator = Items.comparator */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$comparator() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.comparator);
    }

    /** Item.compass = Items.compass */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$compass() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.compass);
    }

    /** Item.cookie = Items.cookie */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$cookie() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cookie);
    }

    /** Item.diamond = Items.diamond */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$diamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond);
    }

    /** Item.doorIron = Items.iron_door */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$doorIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_door);
    }

    /** Item.doorWood = Items.wooden_door */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$doorWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_door);
    }

    /** Item.dyePowder = Items.dye */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$dyePowder() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.dye);
    }

    /** Item.egg = Items.egg */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$egg() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.egg);
    }

    /** Item.emerald = Items.emerald */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$emerald() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.emerald);
    }

    /** Item.emptyMap = Items.map */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemEmptyMap mcc$get$emptyMap() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemEmptyMap.class, legacyconnector.v1_7_10.net.minecraft.init.Items.map);
    }

    /** Item.enchantedBook = Items.enchanted_book */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemEnchantedBook mcc$get$enchantedBook() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemEnchantedBook.class, legacyconnector.v1_7_10.net.minecraft.init.Items.enchanted_book);
    }

    /** Item.enderPearl = Items.ender_pearl */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$enderPearl() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.ender_pearl);
    }

    /** Item.expBottle = Items.experience_bottle */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$expBottle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.experience_bottle);
    }

    /** Item.eyeOfEnder = Items.ender_eye */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$eyeOfEnder() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.ender_eye);
    }

    /** Item.feather = Items.feather */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$feather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.feather);
    }

    /** Item.fermentedSpiderEye = Items.fermented_spider_eye */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$fermentedSpiderEye() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.fermented_spider_eye);
    }

    /** Item.fireballCharge = Items.fire_charge */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$fireballCharge() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.fire_charge);
    }

    /** Item.firework = Items.fireworks */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$firework() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.fireworks);
    }

    /** Item.fireworkCharge = Items.firework_charge */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$fireworkCharge() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.firework_charge);
    }

    /** Item.fishCooked = Items.cooked_fished */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$fishCooked() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cooked_fished);
    }

    /** Item.fishRaw = Items.fish */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$fishRaw() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.fish);
    }

    /** Item.fishingRod = Items.fishing_rod */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemFishingRod mcc$get$fishingRod() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemFishingRod.class, legacyconnector.v1_7_10.net.minecraft.init.Items.fishing_rod);
    }

    /** Item.flint = Items.flint */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$flint() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.flint);
    }

    /** Item.flintAndSteel = Items.flint_and_steel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$flintAndSteel() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.flint_and_steel);
    }

    /** Item.flowerPot = Items.flower_pot */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$flowerPot() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.flower_pot);
    }

    /** Item.ghastTear = Items.ghast_tear */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$ghastTear() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.ghast_tear);
    }

    /** Item.glassBottle = Items.glass_bottle */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$glassBottle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.glass_bottle);
    }

    /** Item.glowstone = Items.glowstone_dust */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$glowstone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.glowstone_dust);
    }

    /** Item.goldNugget = Items.gold_nugget */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$goldNugget() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.gold_nugget);
    }

    /** Item.goldenCarrot = Items.golden_carrot */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$goldenCarrot() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_carrot);
    }

    /** Item.gunpowder = Items.gunpowder */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$gunpowder() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.gunpowder);
    }

    /** Item.helmetChain = Items.chainmail_helmet */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$helmetChain() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chainmail_helmet);
    }

    /** Item.helmetDiamond = Items.diamond_helmet */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$helmetDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_helmet);
    }

    /** Item.helmetGold = Items.golden_helmet */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$helmetGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_helmet);
    }

    /** Item.helmetIron = Items.iron_helmet */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$helmetIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_helmet);
    }

    /** Item.helmetLeather = Items.leather_helmet */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$helmetLeather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.leather_helmet);
    }

    /** Item.hoeDiamond = Items.diamond_hoe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$hoeDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_hoe);
    }

    /** Item.hoeGold = Items.golden_hoe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$hoeGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_hoe);
    }

    /** Item.hoeIron = Items.iron_hoe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$hoeIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_hoe);
    }

    /** Item.hoeStone = Items.stone_hoe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$hoeStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stone_hoe);
    }

    /** Item.hoeWood = Items.wooden_hoe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$hoeWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_hoe);
    }

    /** Item.horseArmorDiamond = Items.diamond_horse_armor */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$horseArmorDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_horse_armor);
    }

    /** Item.horseArmorGold = Items.golden_horse_armor */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$horseArmorGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_horse_armor);
    }

    /** Item.horseArmorIron = Items.iron_horse_armor */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$horseArmorIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_horse_armor);
    }

    /** Item.ingotGold = Items.gold_ingot */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$ingotGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.gold_ingot);
    }

    /** Item.ingotIron = Items.iron_ingot */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$ingotIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_ingot);
    }

    /** Item.itemFrame = Items.item_frame */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$itemFrame() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.item_frame);
    }

    /** Item.leash = Items.lead */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$leash() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.lead);
    }

    /** Item.leather = Items.leather */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$leather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.leather);
    }

    /** Item.legsChain = Items.chainmail_leggings */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$legsChain() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chainmail_leggings);
    }

    /** Item.legsDiamond = Items.diamond_leggings */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$legsDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_leggings);
    }

    /** Item.legsGold = Items.golden_leggings */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$legsGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_leggings);
    }

    /** Item.legsIron = Items.iron_leggings */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$legsIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_leggings);
    }

    /** Item.legsLeather = Items.leather_leggings */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$legsLeather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.leather_leggings);
    }

    /** Item.magmaCream = Items.magma_cream */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$magmaCream() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.magma_cream);
    }

    /** Item.map = Items.filled_map */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemMap mcc$get$map() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemMap.class, legacyconnector.v1_7_10.net.minecraft.init.Items.filled_map);
    }

    /** Item.melon = Items.melon */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$melon() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.melon);
    }

    /** Item.melonSeeds = Items.melon_seeds */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$melonSeeds() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.melon_seeds);
    }

    /** Item.minecartCrate = Items.chest_minecart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$minecartCrate() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chest_minecart);
    }

    /** Item.minecartEmpty = Items.minecart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$minecartEmpty() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.minecart);
    }

    /** Item.minecartHopper = Items.hopper_minecart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$minecartHopper() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.hopper_minecart);
    }

    /** Item.minecartPowered = Items.furnace_minecart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$minecartPowered() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.furnace_minecart);
    }

    /** Item.minecartTnt = Items.tnt_minecart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$minecartTnt() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.tnt_minecart);
    }

    /** Item.monsterPlacer = Items.spawn_egg */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$monsterPlacer() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.spawn_egg);
    }

    /** Item.nameTag = Items.name_tag */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$nameTag() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.name_tag);
    }

    /** Item.netherQuartz = Items.quartz */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$netherQuartz() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.quartz);
    }

    /** Item.netherStalkSeeds = Items.nether_wart */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$netherStalkSeeds() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.nether_wart);
    }

    /** Item.netherStar = Items.nether_star */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$netherStar() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.nether_star);
    }

    /** Item.netherrackBrick = Items.netherbrick */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$netherrackBrick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.netherbrick);
    }

    /** Item.painting = Items.painting */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$painting() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.painting);
    }

    /** Item.paper = Items.paper */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$paper() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.paper);
    }

    /** Item.pickaxeDiamond = Items.diamond_pickaxe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pickaxeDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_pickaxe);
    }

    /** Item.pickaxeGold = Items.golden_pickaxe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pickaxeGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_pickaxe);
    }

    /** Item.pickaxeIron = Items.iron_pickaxe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pickaxeIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_pickaxe);
    }

    /** Item.pickaxeStone = Items.stone_pickaxe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pickaxeStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stone_pickaxe);
    }

    /** Item.pickaxeWood = Items.wooden_pickaxe */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pickaxeWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_pickaxe);
    }

    /** Item.plateChain = Items.chainmail_chestplate */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$plateChain() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.chainmail_chestplate);
    }

    /** Item.plateDiamond = Items.diamond_chestplate */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$plateDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_chestplate);
    }

    /** Item.plateGold = Items.golden_chestplate */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$plateGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_chestplate);
    }

    /** Item.plateIron = Items.iron_chestplate */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$plateIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_chestplate);
    }

    /** Item.plateLeather = Items.leather_chestplate */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemArmor mcc$get$plateLeather() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemArmor.class, legacyconnector.v1_7_10.net.minecraft.init.Items.leather_chestplate);
    }

    /** Item.pocketSundial = Items.clock */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pocketSundial() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.clock);
    }

    /** Item.poisonousPotato = Items.poisonous_potato */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$poisonousPotato() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.poisonous_potato);
    }

    /** Item.porkCooked = Items.cooked_porkchop */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$porkCooked() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.cooked_porkchop);
    }

    /** Item.porkRaw = Items.porkchop */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$porkRaw() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.porkchop);
    }

    /** Item.potato = Items.potato */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$potato() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.potato);
    }

    /** Item.potion = Items.potion */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemPotion mcc$get$potion() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemPotion.class, Ids.vanillaItem("potion"));
    }

    /** Item.pumpkinPie = Items.pumpkin_pie */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pumpkinPie() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.pumpkin_pie);
    }

    /** Item.pumpkinSeeds = Items.pumpkin_seeds */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$pumpkinSeeds() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.pumpkin_seeds);
    }

    /** Item.record11 = Items.record_11 */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$record11() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_11);
    }

    /** Item.record13 = Items.record_13 */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$record13() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_13);
    }

    /** Item.recordBlocks = Items.record_blocks */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordBlocks() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_blocks);
    }

    /** Item.recordCat = Items.record_cat */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordCat() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_cat);
    }

    /** Item.recordChirp = Items.record_chirp */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordChirp() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_chirp);
    }

    /** Item.recordFar = Items.record_far */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordFar() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_far);
    }

    /** Item.recordMall = Items.record_mall */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordMall() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_mall);
    }

    /** Item.recordMellohi = Items.record_mellohi */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordMellohi() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_mellohi);
    }

    /** Item.recordStal = Items.record_stal */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordStal() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_stal);
    }

    /** Item.recordStrad = Items.record_strad */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordStrad() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_strad);
    }

    /** Item.recordWait = Items.record_wait */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordWait() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_wait);
    }

    /** Item.recordWard = Items.record_ward */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$recordWard() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.record_ward);
    }

    /** Item.redstone = Items.redstone */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$redstone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.redstone);
    }

    /** Item.redstoneRepeater = Items.repeater */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$redstoneRepeater() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.repeater);
    }

    /** Item.reed = Items.reeds */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$reed() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.reeds);
    }

    /** Item.rottenFlesh = Items.rotten_flesh */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$rottenFlesh() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.rotten_flesh);
    }

    /** Item.saddle = Items.saddle */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$saddle() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.saddle);
    }

    /** Item.seeds = Items.wheat_seeds */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$seeds() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wheat_seeds);
    }

    /** Item.shears = Items.shears */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemShears mcc$get$shears() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.ItemShears.class, legacyconnector.v1_7_10.net.minecraft.init.Items.shears);
    }

    /** Item.shovelDiamond = Items.diamond_shovel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$shovelDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_shovel);
    }

    /** Item.shovelGold = Items.golden_shovel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$shovelGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_shovel);
    }

    /** Item.shovelIron = Items.iron_shovel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$shovelIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_shovel);
    }

    /** Item.shovelStone = Items.stone_shovel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$shovelStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stone_shovel);
    }

    /** Item.shovelWood = Items.wooden_shovel */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$shovelWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_shovel);
    }

    /** Item.sign = Items.sign */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$sign() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.sign);
    }

    /** Item.silk = Items.string */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$silk() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.string);
    }

    /** Item.skull = Items.skull */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$skull() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.skull);
    }

    /** Item.slimeBall = Items.slime_ball */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$slimeBall() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.slime_ball);
    }

    /** Item.snowball = Items.snowball */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$snowball() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.snowball);
    }

    /** Item.speckledMelon = Items.speckled_melon */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$speckledMelon() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.speckled_melon);
    }

    /** Item.spiderEye = Items.spider_eye */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$spiderEye() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.spider_eye);
    }

    /** Item.stick = Items.stick */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$stick() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stick);
    }

    /** Item.sugar = Items.sugar */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$sugar() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.sugar);
    }

    /** Item.swordDiamond = Items.diamond_sword */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$swordDiamond() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.diamond_sword);
    }

    /** Item.swordGold = Items.golden_sword */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$swordGold() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.golden_sword);
    }

    /** Item.swordIron = Items.iron_sword */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$swordIron() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.iron_sword);
    }

    /** Item.swordStone = Items.stone_sword */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$swordStone() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.stone_sword);
    }

    /** Item.swordWood = Items.wooden_sword */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$swordWood() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wooden_sword);
    }

    /** Item.wheat = Items.wheat */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$wheat() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.wheat);
    }

    /** Item.writableBook = Items.writable_book */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$writableBook() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.writable_book);
    }

    /** Item.writtenBook = Items.written_book */
    public static legacyconnector.v1_7_10.net.minecraft.item.Item mcc$get$writtenBook() {
        return Ids.as(legacyconnector.v1_7_10.net.minecraft.item.Item.class, legacyconnector.v1_7_10.net.minecraft.init.Items.written_book);
    }

    // END GENERATED
}
