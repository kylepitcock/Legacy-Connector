package mccompat.v1_7_10.net.minecraft.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.util.ChatComponentTranslation;

/** Vanilla statistic lists; mods insert into them and read the general stats by field. */
public class StatList {
    public static final Map<String, StatBase> oneShotStats = new HashMap<>();
    public static final List<StatBase> allStats = new ArrayList<>();
    public static final List<StatBase> generalStats = new ArrayList<>();
    public static final List<StatBase> itemStats = new ArrayList<>();
    public static final List<StatBase> objectMineStats = new ArrayList<>();

    public static final StatBase[] mineBlockStatArray = new StatBase[4096];
    public static final StatBase[] objectCraftStats = new StatBase[32000];
    public static final StatBase[] objectUseStats = new StatBase[32000];
    public static final StatBase[] objectBreakStats = new StatBase[32000];

    public static final StatBase leaveGameStat = basic("stat.leaveGame", StatBase.simpleStatType);
    public static final StatBase minutesPlayedStat = basic("stat.playOneMinute", StatBase.timeStatType);
    public static final StatBase distanceWalkedStat = basic("stat.walkOneCm", StatBase.distanceStatType);
    public static final StatBase distanceSwumStat = basic("stat.swimOneCm", StatBase.distanceStatType);
    public static final StatBase distanceFallenStat = basic("stat.fallOneCm", StatBase.distanceStatType);
    public static final StatBase distanceClimbedStat = basic("stat.climbOneCm", StatBase.distanceStatType);
    public static final StatBase distanceFlownStat = basic("stat.flyOneCm", StatBase.distanceStatType);
    public static final StatBase distanceDoveStat = basic("stat.diveOneCm", StatBase.distanceStatType);
    public static final StatBase distanceByMinecartStat = basic("stat.minecartOneCm", StatBase.distanceStatType);
    public static final StatBase distanceByBoatStat = basic("stat.boatOneCm", StatBase.distanceStatType);
    public static final StatBase distanceByPigStat = basic("stat.pigOneCm", StatBase.distanceStatType);
    public static final StatBase jumpStat = basic("stat.jump", StatBase.simpleStatType);
    public static final StatBase dropStat = basic("stat.drop", StatBase.simpleStatType);
    public static final StatBase damageDealtStat = basic("stat.damageDealt", StatBase.simpleStatType);
    public static final StatBase damageTakenStat = basic("stat.damageTaken", StatBase.simpleStatType);
    public static final StatBase deathsStat = basic("stat.deaths", StatBase.simpleStatType);
    public static final StatBase mobKillsStat = basic("stat.mobKills", StatBase.simpleStatType);
    public static final StatBase animalsBredStat = basic("stat.animalsBred", StatBase.simpleStatType);
    public static final StatBase playerKillsStat = basic("stat.playerKills", StatBase.simpleStatType);
    public static final StatBase fishCaughtStat = basic("stat.fishCaught", StatBase.simpleStatType);

    private static StatBase basic(String key, IStatType type) {
        return new StatBasic(key, new ChatComponentTranslation(key), type).initIndependentStat().registerStat();
    }

    public static void func_151178_a() {
    }

    public static StatBase getOneShotStat(String id) {
        return oneShotStats.get(id);
    }
}
