package org.kylepitcock.legacyconnector.vb1_7_3;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievements;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Original Beta achievement registrations and award callbacks become native data-pack advancements. */
public final class AdvancementsB173 {
    public static final String CRITERION="legacy_earned";
    private static final Map<String,Stat> REGISTERED=new LinkedHashMap<>();
    private AdvancementsB173(){}
    public static synchronized void register(Stat stat){
        String key=stat.mcc$namespace+":"+stat.id;
        Stat previous=REGISTERED.putIfAbsent(key,stat);
        if(previous!=null&&previous!=stat)throw new IllegalArgumentException("Duplicate Beta statistic "+key+": "+stat.stringId);
    }
    public static synchronized List<Stat> all(){return List.copyOf(REGISTERED.values());}
    public static ResourceLocation idOf(Achievement achievement){
        if(achievement.mcc$vanilla&&achievement==Achievements.OPEN_INVENTORY)return ResourceLocation.withDefaultNamespace("story/root");
        String name=achievement.stringId.replaceFirst("^achievement\\.","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]","_");
        if(name.isEmpty())name="unnamed";
        return ResourceLocation.fromNamespaceAndPath(achievement.mcc$namespace,"beta/achievements/"+name+"_"+achievement.id);
    }
    public static void describe(Achievement achievement,String title,String description){
        Objects.requireNonNull(achievement,"achievement");achievement.mcc$title=title;achievement.mcc$description=description;
        ContentB173.addLocalization(achievement.stringId,title);ContentB173.addLocalization(achievement.translationKey,description);
    }
    public static void contribute(Map<String,Supplier<byte[]>> out){
        for(Stat stat:all())if(stat instanceof Achievement achievement&&!achievement.mcc$vanilla&&!org.kylepitcock.legacyconnector.core.LegacyLifecycle.isFailed(achievement.mcc$namespace)){
            ResourceLocation id=idOf(achievement),parent=achievement.parent==null?null:idOf(achievement.parent);
            var stack=achievement.icon==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(achievement.icon);
            String icon=stack.isEmpty()?"minecraft:barrier":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            byte[] bytes=definition(achievement,parent,icon).toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/"+id.getNamespace()+"/advancement/"+id.getPath()+".json",()->bytes);
        }
    }
    static JsonObject definition(Achievement achievement,ResourceLocation parent,String icon){
        JsonObject json=new JsonObject();if(parent!=null)json.addProperty("parent",parent.toString());
        JsonObject display=new JsonObject(),item=new JsonObject();item.addProperty("id",icon);display.add("icon",item);
        JsonObject title=new JsonObject(),description=new JsonObject();
        title.addProperty(achievement.mcc$title==null?"translate":"text",achievement.mcc$title==null?achievement.stringId:achievement.mcc$title);
        description.addProperty(achievement.mcc$description==null?"translate":"text",achievement.mcc$description==null?achievement.translationKey:achievement.mcc$description);
        display.add("title",title);display.add("description",description);display.addProperty("frame",achievement.isChallenge()?"challenge":"task");
        display.addProperty("show_toast",true);display.addProperty("announce_to_chat",false);
        if(parent==null)display.addProperty("background","minecraft:textures/gui/advancements/backgrounds/stone.png");
        json.add("display",display);
        JsonObject criteria=new JsonObject(),criterion=new JsonObject();criterion.addProperty("trigger","minecraft:impossible");criteria.add(CRITERION,criterion);json.add("criteria",criteria);
        JsonArray requirements=new JsonArray(),group=new JsonArray();group.add(CRITERION);requirements.add(group);json.add("requirements",requirements);
        return json;
    }
    /** Called only by the original player-stat API; client renderers cannot award server progress. */
    public static void increment(ServerPlayer player,Stat stat,int amount){
        if(stat==null||amount<=0||player.getServer()==null)return;
        if(stat instanceof Achievement achievement){
            var advancement=player.getServer().getAdvancements().get(idOf(achievement));
            if(advancement!=null){
                if(achievement.mcc$vanilla)for(String criterion:advancement.value().criteria().keySet())player.getAdvancements().award(advancement,criterion);
                else player.getAdvancements().award(advancement,CRITERION);
            }
            return;
        }
        var persistent=player.getPersistentData();var counters=persistent.getCompound("legacyconnector_beta_stats");
        String key=stat.mcc$namespace+":"+stat.id;
        counters.putInt(key,(int)Math.min(Integer.MAX_VALUE,(long)counters.getInt(key)+amount));persistent.put("legacyconnector_beta_stats",counters);
    }
}
