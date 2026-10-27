package legacyconnector.v1_12_2.net.minecraft.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/**
 * 1.12's statistics. The general ones count as the modern custom statistics; the per item and per block ones as the modern
 * used/broken/crafted/mined/picked up/dropped statistics of the modern item or block.
 */
public class StatList {
    protected static final Map<String, StatBase> ID_TO_STAT_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    public static final List<StatBase> ALL_STATS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<StatBasic> BASIC_STATS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<StatCrafting> USE_ITEM_STATS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<StatCrafting> MINE_BLOCK_STATS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static final StatBase ANIMALS_BRED = custom(new StatBasic("stat.animalsBred", new TextComponentTranslation("stat.animalsBred")), net.minecraft.stats.Stats.ANIMALS_BRED);
    public static final StatBase FURNACE_INTERACTION = custom(new StatBasic("stat.furnaceInteraction", new TextComponentTranslation("stat.furnaceInteraction")), net.minecraft.stats.Stats.INTERACT_WITH_FURNACE);
    public static final StatBase CRAFTING_TABLE_INTERACTION = custom(new StatBasic("stat.craftingTableInteraction", new TextComponentTranslation("stat.craftingTableInteraction")), net.minecraft.stats.Stats.INTERACT_WITH_CRAFTING_TABLE);
    public static final StatBase CHEST_OPENED = custom(new StatBasic("stat.chestOpened", new TextComponentTranslation("stat.chestOpened")), net.minecraft.stats.Stats.OPEN_CHEST);
    public static final StatBase SLEEP_IN_BED = custom(new StatBasic("stat.sleepInBed", new TextComponentTranslation("stat.sleepInBed")), net.minecraft.stats.Stats.SLEEP_IN_BED);
    public static final StatBase DEATHS = custom(new StatBasic("stat.deaths", new TextComponentTranslation("stat.deaths")), net.minecraft.stats.Stats.DEATHS);
    public static final StatBase MOB_KILLS = custom(new StatBasic("stat.mobKills", new TextComponentTranslation("stat.mobKills")), net.minecraft.stats.Stats.MOB_KILLS);
    public static final StatBase FISH_CAUGHT = custom(new StatBasic("stat.fishCaught", new TextComponentTranslation("stat.fishCaught")), net.minecraft.stats.Stats.FISH_CAUGHT);
    public static final StatBase TALKED_TO_VILLAGER = custom(new StatBasic("stat.talkedToVillager", new TextComponentTranslation("stat.talkedToVillager")), net.minecraft.stats.Stats.TALKED_TO_VILLAGER);
    public static final StatBase TRADED_WITH_VILLAGER = custom(new StatBasic("stat.tradedWithVillager", new TextComponentTranslation("stat.tradedWithVillager")), net.minecraft.stats.Stats.TRADED_WITH_VILLAGER);
    public static final StatBase CAKE_SLICES_EATEN = custom(new StatBasic("stat.cakeSlicesEaten", new TextComponentTranslation("stat.cakeSlicesEaten")), net.minecraft.stats.Stats.EAT_CAKE_SLICE);
    public static final StatBase CAULDRON_FILLED = custom(new StatBasic("stat.cauldronFilled", new TextComponentTranslation("stat.cauldronFilled")), net.minecraft.stats.Stats.FILL_CAULDRON);
    public static final StatBase CAULDRON_USED = custom(new StatBasic("stat.cauldronUsed", new TextComponentTranslation("stat.cauldronUsed")), net.minecraft.stats.Stats.USE_CAULDRON);
    public static final StatBase ARMOR_CLEANED = custom(new StatBasic("stat.armorCleaned", new TextComponentTranslation("stat.armorCleaned")), net.minecraft.stats.Stats.CLEAN_ARMOR);
    public static final StatBase BANNER_CLEANED = custom(new StatBasic("stat.bannerCleaned", new TextComponentTranslation("stat.bannerCleaned")), net.minecraft.stats.Stats.CLEAN_BANNER);
    public static final StatBase BREWINGSTAND_INTERACTION = custom(new StatBasic("stat.brewingstandInteraction", new TextComponentTranslation("stat.brewingstandInteraction")), net.minecraft.stats.Stats.INTERACT_WITH_BREWINGSTAND);
    public static final StatBase BEACON_INTERACTION = custom(new StatBasic("stat.beaconInteraction", new TextComponentTranslation("stat.beaconInteraction")), net.minecraft.stats.Stats.INTERACT_WITH_BEACON);
    public static final StatBase DROPPER_INSPECTED = custom(new StatBasic("stat.dropperInspected", new TextComponentTranslation("stat.dropperInspected")), net.minecraft.stats.Stats.INSPECT_DROPPER);
    public static final StatBase HOPPER_INSPECTED = custom(new StatBasic("stat.hopperInspected", new TextComponentTranslation("stat.hopperInspected")), net.minecraft.stats.Stats.INSPECT_HOPPER);
    public static final StatBase DISPENSER_INSPECTED = custom(new StatBasic("stat.dispenserInspected", new TextComponentTranslation("stat.dispenserInspected")), net.minecraft.stats.Stats.INSPECT_DISPENSER);
    public static final StatBase NOTEBLOCK_PLAYED = custom(new StatBasic("stat.noteblockPlayed", new TextComponentTranslation("stat.noteblockPlayed")), net.minecraft.stats.Stats.PLAY_NOTEBLOCK);
    public static final StatBase NOTEBLOCK_TUNED = custom(new StatBasic("stat.noteblockTuned", new TextComponentTranslation("stat.noteblockTuned")), net.minecraft.stats.Stats.TUNE_NOTEBLOCK);
    public static final StatBase FLOWER_POTTED = custom(new StatBasic("stat.flowerPotted", new TextComponentTranslation("stat.flowerPotted")), net.minecraft.stats.Stats.POT_FLOWER);
    public static final StatBase TRAPPED_CHEST_TRIGGERED = custom(new StatBasic("stat.trappedChestTriggered", new TextComponentTranslation("stat.trappedChestTriggered")), net.minecraft.stats.Stats.TRIGGER_TRAPPED_CHEST);
    public static final StatBase ENDERCHEST_OPENED = custom(new StatBasic("stat.enderchestOpened", new TextComponentTranslation("stat.enderchestOpened")), net.minecraft.stats.Stats.OPEN_ENDERCHEST);
    public static final StatBase ITEM_ENCHANTED = custom(new StatBasic("stat.itemEnchanted", new TextComponentTranslation("stat.itemEnchanted")), net.minecraft.stats.Stats.ENCHANT_ITEM);
    public static final StatBase RECORD_PLAYED = custom(new StatBasic("stat.recordPlayed", new TextComponentTranslation("stat.recordPlayed")), net.minecraft.stats.Stats.PLAY_RECORD);
    public static final StatBase PLAY_ONE_MINUTE = custom(new StatBasic("stat.playOneMinute", new TextComponentTranslation("stat.playOneMinute"), StatBase.timeStatType), net.minecraft.stats.Stats.PLAY_TIME);
    public static final StatBase TIME_SINCE_DEATH = custom(new StatBasic("stat.timeSinceDeath", new TextComponentTranslation("stat.timeSinceDeath"), StatBase.timeStatType), net.minecraft.stats.Stats.TIME_SINCE_DEATH);
    public static final StatBase SNEAK_TIME = custom(new StatBasic("stat.sneakTime", new TextComponentTranslation("stat.sneakTime"), StatBase.timeStatType), net.minecraft.stats.Stats.CROUCH_TIME);
    public static final StatBase WALK_ONE_CM = custom(new StatBasic("stat.walkOneCm", new TextComponentTranslation("stat.walkOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.WALK_ONE_CM);
    public static final StatBase CROUCH_ONE_CM = custom(new StatBasic("stat.crouchOneCm", new TextComponentTranslation("stat.crouchOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.CROUCH_ONE_CM);
    public static final StatBase SPRINT_ONE_CM = custom(new StatBasic("stat.sprintOneCm", new TextComponentTranslation("stat.sprintOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.SPRINT_ONE_CM);
    public static final StatBase CLIMB_ONE_CM = custom(new StatBasic("stat.climbOneCm", new TextComponentTranslation("stat.climbOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.CLIMB_ONE_CM);
    public static final StatBase FLY_ONE_CM = custom(new StatBasic("stat.flyOneCm", new TextComponentTranslation("stat.flyOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.FLY_ONE_CM);
    public static final StatBase DIVE_ONE_CM = custom(new StatBasic("stat.diveOneCm", new TextComponentTranslation("stat.diveOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.WALK_UNDER_WATER_ONE_CM);
    public static final StatBase MINECART_ONE_CM = custom(new StatBasic("stat.minecartOneCm", new TextComponentTranslation("stat.minecartOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.MINECART_ONE_CM);
    public static final StatBase BOAT_ONE_CM = custom(new StatBasic("stat.boatOneCm", new TextComponentTranslation("stat.boatOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.BOAT_ONE_CM);
    public static final StatBase PIG_ONE_CM = custom(new StatBasic("stat.pigOneCm", new TextComponentTranslation("stat.pigOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.PIG_ONE_CM);
    public static final StatBase HORSE_ONE_CM = custom(new StatBasic("stat.horseOneCm", new TextComponentTranslation("stat.horseOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.HORSE_ONE_CM);
    public static final StatBase AVIATE_ONE_CM = custom(new StatBasic("stat.aviateOneCm", new TextComponentTranslation("stat.aviateOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.AVIATE_ONE_CM);
    public static final StatBase FALL_ONE_CM = custom(new StatBasic("stat.fallOneCm", new TextComponentTranslation("stat.fallOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.FALL_ONE_CM);
    public static final StatBase SWIM_ONE_CM = custom(new StatBasic("stat.swimOneCm", new TextComponentTranslation("stat.swimOneCm"), StatBase.distanceStatType), net.minecraft.stats.Stats.SWIM_ONE_CM);
    public static final StatBase DAMAGE_DEALT = custom(new StatBasic("stat.damageDealt", new TextComponentTranslation("stat.damageDealt"), StatBase.divideByTen), net.minecraft.stats.Stats.DAMAGE_DEALT);
    public static final StatBase DAMAGE_TAKEN = custom(new StatBasic("stat.damageTaken", new TextComponentTranslation("stat.damageTaken"), StatBase.divideByTen), net.minecraft.stats.Stats.DAMAGE_TAKEN);
    public static final StatBase OPEN_SHULKER_BOX = custom(new StatBasic("stat.shulkerBoxOpened", new TextComponentTranslation("stat.shulkerBoxOpened")), net.minecraft.stats.Stats.OPEN_SHULKER_BOX);
    public static final StatBase PLAYER_KILLS = custom(new StatBasic("stat.playerKills", new TextComponentTranslation("stat.playerKills")), net.minecraft.stats.Stats.PLAYER_KILLS);
    public static final StatBase LEAVE_GAME = custom(new StatBasic("stat.leaveGame", new TextComponentTranslation("stat.leaveGame")), net.minecraft.stats.Stats.LEAVE_GAME);
    public static final StatBase DROP = custom(new StatBasic("stat.drop", new TextComponentTranslation("stat.drop")), net.minecraft.stats.Stats.DROP);
    public static final StatBase JUMP = custom(new StatBasic("stat.jump", new TextComponentTranslation("stat.jump")), net.minecraft.stats.Stats.JUMP);

    private static final Map<Object, StatBase> PER_OBJECT = new java.util.concurrent.ConcurrentHashMap<>();

    private StatList() {}

    private static StatBase custom(StatBasic stat, net.minecraft.resources.ResourceLocation modern) {
        stat.mcc$bind(() -> net.minecraft.stats.Stats.CUSTOM.get(modern));
        ID_TO_STAT_MAP.put(stat.statId, stat);
        ALL_STATS.add(stat);
        BASIC_STATS.add(stat);
        return stat;
    }

    static void mcc$register(StatBase stat) {
        ID_TO_STAT_MAP.putIfAbsent(stat.statId, stat);
        if (!ALL_STATS.contains(stat)) ALL_STATS.add(stat);
    }

    public static void init() {
    }

    private static net.minecraft.world.item.Item modernItem(Item item) {
        return item == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122.modern(item, 0);
    }

    private static <T> StatBase itemStat(String kind, String prefix, Item item, net.minecraft.stats.StatType<net.minecraft.world.item.Item> type) {
        if (item == null) return null;
        return PER_OBJECT.computeIfAbsent(java.util.List.of(kind, item), k -> {
            var name = Item.REGISTRY.getNameForObject(item);
            String id = prefix + (name == null ? "unknown" : name.getNamespace() + "." + name.getPath());
            StatCrafting stat = new StatCrafting(prefix, name == null ? "unknown" : name.getNamespace() + "." + name.getPath(),
                    new TextComponentTranslation(prefix.substring(0, prefix.length() - 1) + "Item", String.valueOf(name)),
                    item);
            stat.mcc$bind(() -> {
                var m = modernItem(item);
                return m == null ? null : type.get(m);
            });
            ID_TO_STAT_MAP.putIfAbsent(id, stat);
            return stat;
        });
    }

    public static StatBase getCraftStats(Item itemIn) {
        return itemStat("craft", "stat.craftItem.", itemIn, net.minecraft.stats.Stats.ITEM_CRAFTED);
    }

    public static StatBase getObjectUseStats(Item itemIn) {
        return itemStat("use", "stat.useItem.", itemIn, net.minecraft.stats.Stats.ITEM_USED);
    }

    public static StatBase getObjectBreakStats(Item itemIn) {
        return itemStat("break", "stat.breakItem.", itemIn, net.minecraft.stats.Stats.ITEM_BROKEN);
    }

    public static StatBase getObjectsPickedUpStats(Item itemIn) {
        return itemStat("pickup", "stat.pickup.", itemIn, net.minecraft.stats.Stats.ITEM_PICKED_UP);
    }

    public static StatBase getDroppedObjectStats(Item itemIn) {
        return itemStat("drop", "stat.drop.", itemIn, net.minecraft.stats.Stats.ITEM_DROPPED);
    }

    /** The "mined" statistic of a block (null for blocks without one, as in 1.12). */
    public static StatBase getBlockStats(Block blockIn) {
        if (blockIn == null) return null;
        return PER_OBJECT.computeIfAbsent(java.util.List.of("mine", blockIn), k -> {
            var name = Block.REGISTRY.getNameForObject(blockIn);
            String suffix = name == null ? "unknown" : name.getNamespace() + "." + name.getPath();
            Item item = Item.getItemFromBlock(blockIn);
            StatCrafting stat = new StatCrafting("stat.mineBlock.", suffix, new TextComponentTranslation("stat.mineBlock",
                    String.valueOf(name)), item);
            stat.mcc$bind(() -> blockIn.mcc$modern instanceof net.minecraft.world.level.block.Block mb ? net.minecraft.stats.Stats.BLOCK_MINED.get(mb) : null);
            ID_TO_STAT_MAP.putIfAbsent(stat.statId, stat);
            return stat;
        });
    }

    public static StatBase getOneShotStat(String statName) {
        return ID_TO_STAT_MAP.get(statName);
    }

    public static void reinit() {
    }
}
