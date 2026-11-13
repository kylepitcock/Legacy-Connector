package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.stat.ItemOrBlockStat;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import net.minecraft.server.level.ServerPlayer;

/** Original numeric statistic families resolve to the existing native block/item statistics. */
public final class StatisticsB173 {
    private StatisticsB173(){}
    private static boolean installed;
    private static final java.util.Set<Class<?>> REPORTED_RECIPES=new java.util.HashSet<>();
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event)->crafted(event.getServer()));
        bus.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event)->{if(event.getPlayer()==null)crafted(event.getPlayerList().getServer());});
    }
    private static void crafted(net.minecraft.server.MinecraftServer server){
        // The shared recipe view uses the native manager; include its representable vanilla/data-pack outputs.
        for(var recipe:server.getRecipeManager().getRecipes()){
            try{
                var stack=StacksB173.toLegacy(recipe.value().getResultItem(server.registryAccess()));
                if(stack!=null)legacyconnector.vb1_7_3.net.minecraft.stat.Stats.mcc$crafted(stack.itemId);
            }catch(RuntimeException|LinkageError error){
                if(REPORTED_RECIPES.size()<32&&REPORTED_RECIPES.add(recipe.value().getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Cannot describe native recipe output for original crafting statistics",error);
            }
        }
    }
    private static final class Owned {final Object player,stat;int credit=1;Owned(Object player,Object stat){this.player=player;this.stat=stat;}}
    private static final ThreadLocal<Owned> OWNED=new ThreadLocal<>();
    public static final class Scope implements AutoCloseable {
        private final Owned previous;private boolean closed;
        private Scope(Owned previous){this.previous=previous;}
        @Override public void close(){if(closed)return;closed=true;if(previous==null)OWNED.remove();else OWNED.set(previous);}
    }
    public static Scope nativeAward(Object player,Object stat){var previous=OWNED.get();OWNED.set(new Owned(player,stat));return new Scope(previous);}
    static boolean nativeOwns(Object player,Object stat){var owned=OWNED.get();return owned!=null&&owned.player==player&&owned.stat==stat;}
    public static net.minecraft.stats.Stat<?> nativeStat(Stat stat){
        if(stat==null)return null;
        if(stat instanceof ItemOrBlockStat item){
            int id=item.getItemOrBlockId(),family=stat.id-id;
            if(family==0x1000000){
                var blocks=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS;
                var block=id>=0&&id<blocks.length&&blocks[id]!=null?blocks[id].mcc$modern:null;
                return block==null||!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsValue(block)?null:net.minecraft.stats.Stats.BLOCK_MINED.get(block);
            }
            var items=legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS;
            var nativeItem=id>=0&&id<items.length&&items[id]!=null?items[id].mcc$modern:null;
            if(nativeItem==null||!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsValue(nativeItem))return null; // a stat of a legacy item that is no registered native item has no native counterpart
            return switch(family){case 0x1010000->net.minecraft.stats.Stats.ITEM_CRAFTED.get(nativeItem);case 0x1020000->net.minecraft.stats.Stats.ITEM_USED.get(nativeItem);case 0x1030000->net.minecraft.stats.Stats.ITEM_BROKEN.get(nativeItem);default->null;};
        }
        if(!"minecraft".equals(stat.mcc$namespace)||legacyconnector.vb1_7_3.net.minecraft.stat.Stats.getStatById(stat.id)!=stat)return null;
        String path=switch(stat.id){case 1004->"leave_game";case 1100->"play_time";case 2000->"walk_one_cm";case 2001->"swim_one_cm";case 2002->"fall_one_cm";case 2003->"climb_one_cm";case 2004->"fly_one_cm";case 2005->"walk_under_water_one_cm";case 2006->"minecart_one_cm";case 2007->"boat_one_cm";case 2008->"pig_one_cm";case 2010->"jump";case 2011->"drop";case 2020->"damage_dealt";case 2021->"damage_taken";case 2022->"deaths";case 2023->"mob_kills";case 2024->"player_kills";case 2025->"fish_caught";default->null;};
        if(path==null)return null;
        var id=net.minecraft.resources.ResourceLocation.withDefaultNamespace(path);
        return net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT.containsKey(id)?net.minecraft.stats.Stats.CUSTOM.get(id):null;
    }
    public static boolean increment(ServerPlayer player,Stat stat,int amount){
        net.minecraft.stats.Stat<?> nativeStat;
        try{nativeStat=nativeStat(stat);}catch(RuntimeException error){return false;} // a legacy stat with no native counterpart is not counted
        if(nativeStat==null)return false;
        if(amount<=0)return true;
        if(nativeOwns(player,nativeStat)){var owned=OWNED.get();int credit=Math.min(owned.credit,amount);amount-=credit;owned.credit-=credit;}
        if(amount>0)player.awardStat(nativeStat,amount);return true;
    }
}
