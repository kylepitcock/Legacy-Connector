package org.kylepitcock.legacyconnector.vb1_7_3;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns;

/** ModLoader's per-biome spawn weights become native biome modifiers for admitted entity hosts. */
public final class SpawnRulesB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Spawns");
    private static final Registry RULES=new Registry();
    private static final java.util.concurrent.atomic.AtomicLong REVISION=new java.util.concurrent.atomic.AtomicLong();
    private static final Set<String> WARNED=new HashSet<>();
    public record Entry(Class<?> type,int weight,SpawnGroup group,List<Biome> biomes) {}
    private record Key(Class<?> type,SpawnGroup group,Biome biome) {}
    /** Separate registry makes replacement/removal semantics independently testable. */
    static final class Registry {
        private final Map<Key,Integer> rules=new LinkedHashMap<>();
        synchronized void add(Class<?> type,int weight,SpawnGroup group,Biome[] biomes) {
            Objects.requireNonNull(type,"entityClass cannot be null");Objects.requireNonNull(group,"spawnList cannot be null");
            for(Biome biome:biomes==null?Biome.mcc$standardBiomes():Arrays.asList(biomes)) {
                Objects.requireNonNull(biome,"biome cannot be null");
                rules.put(new Key(type,group,biome),weight);
            }
        }
        synchronized void remove(Class<?> type,SpawnGroup group,Biome[] biomes) {
            Objects.requireNonNull(type,"entityClass cannot be null");Objects.requireNonNull(group,"spawnList cannot be null");
            for(Biome biome:biomes==null?Biome.mcc$standardBiomes():Arrays.asList(biomes)){Objects.requireNonNull(biome,"biome cannot be null");rules.put(new Key(type,group,biome),-1);}
        }
        synchronized List<Entry> entries(){return grouped(false);}
        synchronized List<Entry> changes(){return grouped(true);}
        private List<Entry> grouped(boolean removals) {
            record Group(Class<?> type,int weight,SpawnGroup group) {}
            Map<Group,List<Biome>> grouped=new LinkedHashMap<>();
            rules.forEach((key,weight)->{if(removals||weight>=0)grouped.computeIfAbsent(new Group(key.type,weight,key.group),ignored->new ArrayList<>()).add(key.biome);});
            return grouped.entrySet().stream().map(e->new Entry(e.getKey().type,e.getKey().weight,e.getKey().group,List.copyOf(e.getValue()))).toList();
        }
    }
    private SpawnRulesB173() {}
    public static void add(Class<?> type,int weight,SpawnGroup group,Biome[] biomes){RULES.add(type,weight,group,biomes);REVISION.incrementAndGet();}
    public static void remove(Class<?> type,SpawnGroup group,Biome[] biomes){RULES.remove(type,group,biomes);REVISION.incrementAndGet();}
    static long revision(){return REVISION.get();}
    public static List<Entry> all(){return RULES.entries();}
    public static List<Entry> changes(){return RULES.changes();}
    /** Only exact game API classes, never a mod subclass with a similar name. */
    static EntityType<?> vanillaTypeNamed(String name){
        String relative=null;
        for(String prefix:List.of("legacyconnector.vb1_7_3.net.minecraft.entity.","legacyconnector_synth.vb1_7_3.net.minecraft.entity."))
            if(name.startsWith(prefix)){relative=name.substring(prefix.length());break;}
        if(relative==null||!relative.endsWith("Entity"))return null;
        int dot=relative.lastIndexOf('.');String family=dot<0?"":relative.substring(0,dot+1);
        if(family.equals("mob."))family="monster.";
        String simple=relative.substring(dot+1,relative.length()-6);
        // Beta mappings name this mob GiantEntity; the shared MCP table names it EntityGiantZombie.
        if(family.equals("monster.")&&simple.equals("Giant"))simple="GiantZombie";
        return org.kylepitcock.legacyconnector.v1_7_10.entity.VanillaEntities.modernTypeNamed(family+"Entity"+simple);
    }
    static EntityType<?> vanillaType(Class<?> type){return vanillaTypeNamed(type.getName());}
    /** A unique original ModLoader category is authoritative even for custom direct-LivingEntity families. */
    public static net.minecraft.world.entity.MobCategory categoryFor(Class<?> type,net.minecraft.world.entity.MobCategory fallback){return categoryFor(type,fallback,all());}
    static net.minecraft.world.entity.MobCategory categoryFor(Class<?> type,net.minecraft.world.entity.MobCategory fallback,List<Entry> entries){
        var categories=new LinkedHashSet<net.minecraft.world.entity.MobCategory>();
        for(Entry entry:entries)if(entry.type()==type&&entry.weight()>0)categories.add(entry.group().mcc$modern());
        if(categories.size()==1)return categories.iterator().next();
        if(categories.size()>1)warn("Conflicting Beta spawn groups for "+type.getName()+": "+categories+"; using family category "+fallback);
        return fallback;
    }
    public static Class<?> namedEntity(String name) {
        for(var registration:ContentB173.entities())if(registration.name().equals(name))return registration.type();
        return null;
    }
    static EntityType<?> hostedType(Class<?> original) {
        for(var info:AnimalsB173.all())if(info.registration().type()==original)return info.type();
        for(var info:MobsB173.all())if(info.registration().type()==original)return info.type();
        return null;
    }
    static EntityType<?> resolvedType(Class<?> original){EntityType<?> type=hostedType(original);return type!=null?type:vanillaType(original);}
    public static void contribute(Map<String,Supplier<byte[]>> out) {
        contribute(out,changes(),original->{
            EntityType<?> type=hostedType(original);if(type==null)type=vanillaType(original);
            if(type==null)return null;
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        });
    }
    static void contribute(Map<String,Supplier<byte[]>> out,List<Entry> entries,Function<Class<?>,ResourceLocation> resolve) {
        int index=0;
        for(Entry entry:entries) {
            boolean vanilla=vanillaType(entry.type)!=null;
            if(entry.weight==0&&!vanilla)continue;
            ResourceLocation id=resolve.apply(entry.type);if(id==null)continue;
            var ids=new LinkedHashSet<String>();boolean unsupported=false;
            for(Biome biome:entry.biomes) {
                String modern=biome.mcc$modernBiome();
                if(modern==null){warn("Unbridged Beta biome "+biome.getClass().getName()+" ("+biome.name+") for "+entry.type.getName());unsupported=true;break;}
                ids.add(modern);
            }
            if(unsupported||ids.isEmpty())continue;
            EntityType<?> host=hostedType(entry.type);
            if(host!=null&&host.getCategory()!=entry.group.mcc$modern()) {
                warn("Beta spawn category "+entry.group+" does not match admitted host "+id+"; entry omitted");continue;
            }
            String suffix=id.getNamespace()+"_"+id.getPath().replace('/','_')+"_"+(index++);
            String biomes=entry.biomes.size()==Biome.mcc$standardBiomes().size()&&entry.biomes.containsAll(Biome.mcc$standardBiomes())
                    ?"#minecraft:is_overworld":LegacyEntitySpawns.biomeTag(out,"beta_spawn_"+suffix,ids);
            JsonObject modifier=new JsonObject();modifier.addProperty("biomes",biomes);
            if(vanilla||entry.weight<0){
                modifier.addProperty("type","legacyconnector:beta_spawn_override");modifier.addProperty("entity",id.toString());
                modifier.addProperty("category",entry.group.mcc$modern().getName());modifier.addProperty("weight",Math.max(0,entry.weight));
                byte[] json=modifier.toString().getBytes(StandardCharsets.UTF_8);
                out.put("data/legacyconnector/neoforge/biome_modifier/spawns/beta_"+suffix+".json",()->json);continue;
            }
            modifier.addProperty("type","neoforge:add_spawns");
            JsonObject spawner=new JsonObject();spawner.addProperty("type",id.toString());spawner.addProperty("weight",entry.weight);
            // Beta had no group-size entry fields: its spawn loop tried up to four, capped by the entity callback.
            spawner.addProperty("minCount",1);spawner.addProperty("maxCount",4);modifier.add("spawners",spawner);
            byte[] json=modifier.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/legacyconnector/neoforge/biome_modifier/spawns/beta_"+suffix+".json",()->json);
        }
    }
    private static synchronized void warn(String message){if(WARNED.add(message))LOG.warn(message);}
}
