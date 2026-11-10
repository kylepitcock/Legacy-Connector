package legacyconnector.vb1_7_3.net.minecraft.stat;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;

/** Numeric Beta statistic tables, populated again after original mod content and recipes register. */
public class Stats {
    protected static final Map<Integer,Stat> ID_TO_STAT=new LinkedHashMap<>();
    public static final List<Stat> ALL_STATS=new ArrayList<>(),GENERAL_STATS=new ArrayList<>(),ITEM_STATS=new ArrayList<>(),BLOCK_MINED_STATS=new ArrayList<>();
    public static Stat[] MINE_BLOCK=new Stat[256],CRAFTED=new Stat[32000],USED=new Stat[32000],BROKEN=new Stat[32000];
    public static final Stat START_GAME=general(1000,"stat.startGame"),CREATE_WORLD=general(1001,"stat.createWorld"),LOAD_WORLD=general(1002,"stat.loadWorld"),JOIN_MULTIPLAYER=general(1003,"stat.joinMultiplayer"),LEAVE_GAME=general(1004,"stat.leaveGame"),PLAY_ONE_MINUTE=general(1100,"stat.playOneMinute");
    public static final Stat WALK_ONE_CM=general(2000,"stat.walkOneCm"),SWIM_ONE_CM=general(2001,"stat.swimOneCm"),FALL_ONE_CM=general(2002,"stat.fallOneCm"),CLIMB_ONE_CM=general(2003,"stat.climbOneCm"),FLY_ONE_CM=general(2004,"stat.flyOneCm"),DIVE_ONE_CM=general(2005,"stat.diveOneCm"),MINECART_ONE_CM=general(2006,"stat.minecartOneCm"),BOAT_ONE_CM=general(2007,"stat.boatOneCm"),PIG_ONE_CM=general(2008,"stat.pigOneCm"),JUMP=general(2010,"stat.jump"),DROP=general(2011,"stat.drop"),DAMAGE_DEALT=general(2020,"stat.damageDealt"),DAMAGE_TAKEN=general(2021,"stat.damageTaken"),DEATHS=general(2022,"stat.deaths"),MOB_KILLS=general(2023,"stat.mobKills"),PLAYER_KILLS=general(2024,"stat.playerKills"),FISH_CAUGHT=general(2025,"stat.fishCaught");
    private static Stat general(int id,String key){var stat=new Stat(id,key,"minecraft");if(id<2020)stat.localOnly();mcc$register(stat);GENERAL_STATS.add(stat);return stat;}
    public static synchronized void mcc$register(Stat stat){if(ID_TO_STAT.putIfAbsent(stat.id,stat)==null)ALL_STATS.add(stat);}
    public static synchronized Stat getStatById(int id){return ID_TO_STAT.get(id);}
    public static synchronized void initialize(){
        for(int id=0;id<Math.min(256,Block.BLOCKS.length);id++){
            var block=Block.BLOCKS[id];if(block!=null&&block.isTrackingStatistics()&&MINE_BLOCK[id]==null){
                MINE_BLOCK[id]=entry(0x1000000,id,"stat.mineBlock",owner(block.mcc$owner));BLOCK_MINED_STATS.add(MINE_BLOCK[id]);
            }
        }
        initializeItemStats();initializeExtendedItemStats();
        link(MINE_BLOCK);link(USED);link(BROKEN);
    }
    private static String owner(Object owner){return owner instanceof org.kylepitcock.legacyconnector.core.LegacyModRecord mod?mod.modernId:"minecraft";}
    private static Stat entry(int base,int id,String name,String owner){var stat=new ItemOrBlockStat(base+id,name+"."+id,id,owner);mcc$register(stat);return stat;}
    public static synchronized void initializeItemStats(){items(0,Math.min(256,Item.ITEMS.length));initializeCraftedItemStats();}
    public static synchronized void initializeExtendedItemStats(){items(256,Math.min(32000,Item.ITEMS.length));initializeCraftedItemStats();}
    private static void items(int start,int end){
        for(int id=start;id<end;id++){
            var item=Item.ITEMS[id];if(item==null)continue;
            if(USED[id]==null){USED[id]=entry(0x1020000,id,"stat.useItem",owner(item.mcc$owner));if(id>=256)ITEM_STATS.add(USED[id]);}
            if(item.isDamageable()&&BROKEN[id]==null)BROKEN[id]=entry(0x1030000,id,"stat.breakItem",owner(item.mcc$owner));
        }
    }
    public static synchronized void initializeCraftedItemStats(){
        for(int id:org.kylepitcock.legacyconnector.vb1_7_3.RecipesB173.craftedItemIds())mcc$crafted(id);
    }
    public static synchronized void mcc$crafted(int id){if(id>=0&&id<CRAFTED.length&&Item.ITEMS[id]!=null&&CRAFTED[id]==null)CRAFTED[id]=entry(0x1010000,id,"stat.craftItem",owner(Item.ITEMS[id].mcc$owner));}
    private static void link(Stat[] stats){
        // The original lava entry self-links still lava; it does not alias flowing lava to it.
        int[][] aliases={{9,8},{11,11},{91,86},{62,61},{74,73},{94,93},{76,75},{40,39},{43,44},{2,3},{60,3}};
        for(var pair:aliases){int from=pair[0],to=pair[1];if(stats[from]!=null&&stats[to]==null)stats[to]=stats[from];else if(stats[to]!=null){
            Stat removed=stats[from];if(removed!=stats[to]){ALL_STATS.remove(removed);BLOCK_MINED_STATS.remove(removed);GENERAL_STATS.remove(removed);}stats[from]=stats[to];
        }}
    }
}
