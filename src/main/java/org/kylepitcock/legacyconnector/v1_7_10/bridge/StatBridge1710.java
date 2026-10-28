package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;
import legacyconnector.v1_7_10.net.minecraft.stats.StatBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10 statistics counted in the modern statistics: the vanilla general stats are the modern custom stats of the same
 * meaning, and stats a mod registers ({@code StatBase.registerStat}) become custom stats of their own, kept in the
 * player's stats file and listed on the statistics screen. Their id is taken from the legacy translation key
 * ({@code stat.<mod>.<name>} becomes {@code <mod>:<name>}, whose modern key is the same text) where it has that shape.
 */
public final class StatBridge1710 {
    private static final Map<String, ResourceLocation> VANILLA = Map.ofEntries(
            Map.entry("stat.leaveGame", Stats.LEAVE_GAME), Map.entry("stat.playOneMinute", Stats.PLAY_TIME),
            Map.entry("stat.walkOneCm", Stats.WALK_ONE_CM), Map.entry("stat.swimOneCm", Stats.SWIM_ONE_CM),
            Map.entry("stat.fallOneCm", Stats.FALL_ONE_CM), Map.entry("stat.climbOneCm", Stats.CLIMB_ONE_CM),
            Map.entry("stat.flyOneCm", Stats.FLY_ONE_CM), Map.entry("stat.diveOneCm", Stats.WALK_UNDER_WATER_ONE_CM),
            Map.entry("stat.minecartOneCm", Stats.MINECART_ONE_CM), Map.entry("stat.boatOneCm", Stats.BOAT_ONE_CM),
            Map.entry("stat.pigOneCm", Stats.PIG_ONE_CM), Map.entry("stat.jump", Stats.JUMP), Map.entry("stat.drop", Stats.DROP),
            Map.entry("stat.damageDealt", Stats.DAMAGE_DEALT), Map.entry("stat.damageTaken", Stats.DAMAGE_TAKEN),
            Map.entry("stat.deaths", Stats.DEATHS), Map.entry("stat.mobKills", Stats.MOB_KILLS),
            Map.entry("stat.animalsBred", Stats.ANIMALS_BRED), Map.entry("stat.playerKills", Stats.PLAYER_KILLS),
            Map.entry("stat.fishCaught", Stats.FISH_CAUGHT));
    private static final Map<String, Stat<ResourceLocation>> STATS = new ConcurrentHashMap<>();

    private StatBridge1710() {}

    /** Called by {@code StatBase.registerStat}: a mod stat registered while registries are open becomes a modern custom stat. */
    public static void registered(StatBase stat) {
        if (stat == null || stat.statId == null || stat instanceof Achievement || VANILLA.containsKey(stat.statId) || STATS.containsKey(stat.statId)) return;
        if (!LegacyLifecycle.isRegistrationOpen()) return;
        ResourceLocation id = idOf(stat);
        if (id == null) return;
        try {
            ResourceLocation registered = BuiltInRegistries.CUSTOM_STAT.containsKey(id) ? id : LegacyRegistrar.register(BuiltInRegistries.CUSTOM_STAT, id, id);
            STATS.put(stat.statId, Stats.CUSTOM.get(registered, StatFormatter.DEFAULT));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Stats").warn("Could not register legacy statistic {}: {}", stat.statId, e.toString());
        }
    }

    private static ResourceLocation idOf(StatBase stat) {
        String key = stat.getStatName() instanceof legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation t ? t.getKey() : null;
        if (key == null) key = stat.statId;
        if (key.startsWith("stat.")) {
            String rest = key.substring(5);
            int dot = rest.indexOf('.');
            if (dot > 0) {
                ResourceLocation rl = ResourceLocation.tryParse(rest.substring(0, dot).toLowerCase(Locale.ROOT) + ":" + clean(rest.substring(dot + 1)));
                if (rl != null && !rl.getNamespace().equals("minecraft")) return rl;
            }
        }
        LegacyModContainer1710 owner = LegacyModContainer1710.active();
        return ResourceLocation.tryParse((owner == null ? "legacyconnector" : owner.record().modernId) + ":" + clean(stat.statId));
    }

    private static String clean(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    /** The modern stat a legacy stat counts in, or null. */
    public static Stat<ResourceLocation> modern(StatBase stat) {
        if (stat == null || stat.statId == null) return null;
        ResourceLocation vanilla = VANILLA.get(stat.statId);
        if (vanilla != null) return Stats.CUSTOM.get(vanilla);
        return STATS.get(stat.statId);
    }

    public static void award(ServerPlayer player, StatBase stat, int amount) {
        Stat<ResourceLocation> modern = modern(stat);
        if (modern != null && amount != 0) player.awardStat(modern, amount);
    }

    /** The player's value of the stat, or -1 when it has no modern stat. */
    public static int value(ServerPlayer player, StatBase stat) {
        Stat<ResourceLocation> modern = modern(stat);
        return modern == null ? -1 : player.getStats().getValue(modern);
    }
}
