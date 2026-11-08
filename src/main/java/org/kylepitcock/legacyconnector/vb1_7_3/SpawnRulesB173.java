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
            for(Biome biome:biomes==null?Biome.mcc$standardBiomes():Arrays.asList(biomes))rules.remove(new Key(type,group,biome));
        }
        synchronized List<Entry> entries() {
            record Group(Class<?> type,int weight,SpawnGroup group) {}
            Map<Group,List<Biome>> grouped=new LinkedHashMap<>();
            rules.forEach((key,weight)->grouped.computeIfAbsent(new Group(key.type,weight,key.group),ignored->new ArrayList<>()).add(key.biome));
            return grouped.entrySet().stream().map(e->new Entry(e.getKey().type,e.getKey().weight,e.getKey().group,List.copyOf(e.getValue()))).toList();
        }
    }
    private SpawnRulesB173() {}
    public static void add(Class<?> type,int weight,SpawnGroup group,Biome[] biomes){RULES.add(type,weight,group,biomes);}
    public static void remove(Class<?> type,SpawnGroup group,Biome[] biomes){RULES.remove(type,group,biomes);}
    public static List<Entry> all(){return RULES.entries();}
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
    private static EntityType<?> hostedType(Class<?> original) {
        for(var info:AnimalsB173.all())if(info.registration().type()==original)return info.type();
        for(var info:MobsB173.all())if(info.registration().type()==original)return info.type();
        return null;
    }
    public static void contribute(Map<String,Supplier<byte[]>> out) {
        contribute(out,all(),original->{
            EntityType<?> type=hostedType(original);
            if(type==null)return null;
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        });
    }
    static void contribute(Map<String,Supplier<byte[]>> out,List<Entry> entries,Function<Class<?>,ResourceLocation> resolve) {
        int index=0;
        for(Entry entry:entries) {
            if(entry.weight<=0)continue;
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
            JsonObject modifier=new JsonObject();modifier.addProperty("type","neoforge:add_spawns");modifier.addProperty("biomes",biomes);
            JsonObject spawner=new JsonObject();spawner.addProperty("type",id.toString());spawner.addProperty("weight",entry.weight);
            // Beta had no group-size entry fields: its spawn loop tried up to four, capped by the entity callback.
            spawner.addProperty("minCount",1);spawner.addProperty("maxCount",4);modifier.add("spawners",spawner);
            byte[] json=modifier.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/legacyconnector/neoforge/biome_modifier/spawns/beta_"+suffix+".json",()->json);
        }
    }
    private static synchronized void warn(String message){if(WARNED.add(message))LOG.warn(message);}
}
