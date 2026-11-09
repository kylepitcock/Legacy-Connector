package org.kylepitcock.legacyconnector.vb1_7_3;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement;
import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievements;
import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import legacyconnector.vb1_7_3.net.minecraft.src.ACPage;
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
    static ResourceLocation rootOf(String namespace){return ResourceLocation.fromNamespaceAndPath(namespace,"beta/root");}
    static ResourceLocation pageRoot(ACPage page){
        String name=page.title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]","_");
        return ResourceLocation.fromNamespaceAndPath(page.mcc$namespace,"beta/pages/"+(name.isEmpty()?"unnamed":name)+"_"+Integer.toUnsignedString(page.title.hashCode(),16)+"_"+page.mcc$titleOrdinal);
    }
    static ResourceLocation categoryOf(Achievement achievement){var page=ACPage.mcc$pageOf(achievement);return page==null?rootOf(achievement.mcc$namespace):pageRoot(page);}
    static ResourceLocation parentOf(Achievement achievement){
        if(achievement.mcc$vanilla&&achievement.parent!=null)return achievement.parent==Achievements.OPEN_INVENTORY?rootOf(achievement.mcc$namespace):idOf(achievement.parent);
        return achievement.parent==null||achievement.parent.mcc$vanilla||ACPage.mcc$pageOf(achievement)!=ACPage.mcc$pageOf(achievement.parent)?categoryOf(achievement):idOf(achievement.parent);
    }
    public static void describe(Achievement achievement,String title,String description){
        Objects.requireNonNull(achievement,"achievement");achievement.mcc$title=title;achievement.mcc$description=description;
        ContentB173.addLocalization(achievement.stringId,title);ContentB173.addLocalization(achievement.translationKey,description);
    }
    public static void contribute(Map<String,Supplier<byte[]>> out){
        if(org.kylepitcock.legacyconnector.core.LegacyMods.ordered().stream().noneMatch(mod->PlatformB173.VERSION.equals(mod.version)
                &&mod.instance!=null&&!org.kylepitcock.legacyconnector.core.LegacyLifecycle.isFailed(mod.modernId)))return;
        // Register bridge-owned Beta identities before taking the registry snapshot.
        Achievement vanillaRoot=Achievements.OPEN_INVENTORY;
        Map<ResourceLocation,String> roots=new LinkedHashMap<>();
        Map<ResourceLocation,String> titles=new LinkedHashMap<>();
        for(Stat stat:all())if(stat instanceof Achievement achievement&&achievement!=vanillaRoot&&!org.kylepitcock.legacyconnector.core.LegacyLifecycle.isFailed(achievement.mcc$namespace)){
            ResourceLocation id=idOf(achievement),parent=parentOf(achievement);
            var stack=achievement.icon==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(achievement.icon);
            String icon=stack.isEmpty()?"minecraft:barrier":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            byte[] bytes=definition(achievement,parent,icon).toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/"+id.getNamespace()+"/advancement/"+id.getPath()+".json",()->bytes);
            if(parent.equals(categoryOf(achievement))){
                roots.putIfAbsent(parent,icon);var page=ACPage.mcc$pageOf(achievement);
                if(page!=null)titles.put(parent,page.title);
            }
        }
        for(var entry:roots.entrySet()){
            ResourceLocation id=entry.getKey();String namespace=id.getNamespace();var mod=org.kylepitcock.legacyconnector.core.LegacyMods.find(namespace);
            String title=mod==null?namespace:mod.name();
            byte[] bytes=rootDefinition(titles.getOrDefault(id,title+" (Beta 1.7.3)"),entry.getValue()).toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/"+id.getNamespace()+"/advancement/"+id.getPath()+".json",()->bytes);
        }
    }
    /** Only the synthetic category unlocks automatically; original achievements retain their award callbacks. */
    static JsonObject rootDefinition(String title,String icon){
        JsonObject json=new JsonObject(),display=new JsonObject(),item=new JsonObject();item.addProperty("id",icon);display.add("icon",item);
        display.addProperty("title",title);display.addProperty("description","Achievements of Beta 1.7.3 mods");display.addProperty("frame","task");
        display.addProperty("background","minecraft:textures/gui/advancements/backgrounds/stone.png");
        display.addProperty("show_toast",false);display.addProperty("announce_to_chat",false);json.add("display",display);
        JsonObject criteria=new JsonObject(),automatic=new JsonObject();automatic.addProperty("trigger","minecraft:tick");criteria.add("visible",automatic);json.add("criteria",criteria);
        return json;
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
                if(achievement==Achievements.OPEN_INVENTORY)for(String criterion:advancement.value().criteria().keySet())player.getAdvancements().award(advancement,criterion);
                else player.getAdvancements().award(advancement,CRITERION);
            }
            return;
        }
        var persistent=player.getPersistentData();var counters=persistent.getCompound("legacyconnector_beta_stats");
        String key=stat.mcc$namespace+":"+stat.id;
        counters.putInt(key,(int)Math.min(Integer.MAX_VALUE,(long)counters.getInt(key)+amount));persistent.put("legacyconnector_beta_stats",counters);
    }
}
