package org.kylepitcock.legacyconnector.vb1_7_3.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.src.DimensionBase;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.EntitySpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.SpawnRulesB173;

/** Owner-scoped native resources derived from original dimension registrations and providers. */
public final class DimensionRegistrationsB173 {
    private static final Map<Integer,Registration> REGISTRATIONS=new LinkedHashMap<>();
    private static final class Registration {
        final DimensionBase original;final LegacyModRecord owner;final ResourceKey<Level> key;
        final Map<ResourceLocation,Biome> biomes=new LinkedHashMap<>();final Set<ResourceLocation> biomeKeys=new LinkedHashSet<>();Dimension prototype;
        Registration(DimensionBase original,LegacyModRecord owner){
            this.original=original;this.owner=owner;
            key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(owner==null?"legacyconnector":owner.modernId,"dimension_"+original.number));
        }
    }
    private static final class PrototypeWorld extends World {private PrototypeWorld(){super();}}
    private DimensionRegistrationsB173(){}
    public static synchronized void register(DimensionBase original){
        REGISTRATIONS.putIfAbsent(original.number,new Registration(original,ContentB173.current()));
    }
    public static synchronized ResourceKey<Level> levelKey(int id){
        if(id==0)return Level.OVERWORLD;if(id==-1)return Level.NETHER;
        var entry=REGISTRATIONS.get(id);return entry==null?null:entry.key;
    }
    public static synchronized Integer registeredId(Level world){
        if(world==null)return null;
        for(var entry:REGISTRATIONS.values())if(entry.original.number!=0&&entry.original.number!=-1&&entry.key.equals(world.dimension()))return entry.original.number;
        return null;
    }
    public static int worldId(Level world){
        if(world==null)return 0;Integer registered=registeredId(world);if(registered!=null)return registered;
        return world.dimension().equals(Level.NETHER)?-1:world.dimension().equals(Level.END)?1:0;
    }
    public static synchronized Dimension newProvider(World world){
        var level=world.mcc$level();Integer registered=level==null?null:registeredId(level);
        int id=registered!=null?registered:level!=null&&level.dimension().equals(Level.NETHER)?-1:0;
        if(registered==null&&level!=null&&!level.dimension().equals(Level.NETHER)&&!level.dimension().equals(Level.OVERWORLD))return world.dimension;
        var registration=REGISTRATIONS.get(id);
        if(registration==null)return world.dimension;
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
        var actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,actor!=null&&actor.world==world?actor:null)){
            Dimension provider=registration.original.getWorldProvider();
            if(provider==null)throw new IllegalStateException("Cannot construct registered dimension provider "+registration.original.number);
            world.dimension=provider;provider.setWorld(world);return provider;
        }finally{minecraft.world=previousWorld;}
    }
    public static Dimension provider(int id){
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var key=levelKey(id);if(server==null||key==null)return null;
        var level=server.getLevel(key);return level==null?null:World.mcc$wrap(level).dimension;
    }
    private static <T> T withOwner(Registration registration,Supplier<T> action){
        synchronized(ContentB173.class){
        var previous=ContentB173.current();ContentB173.enter(registration.owner);
        try{return action.get();}finally{if(previous==null)ContentB173.exit();else ContentB173.enter(previous);}
        }
    }
    private static void discover(Registration registration){
        if(registration.prototype!=null)return;
        withOwner(registration,()->{
            var world=new PrototypeWorld();Dimension provider=registration.original.getWorldProvider();
            if(provider==null)throw new IllegalStateException("Cannot initialize registered dimension provider "+registration.original.number);
            world.dimension=provider;provider.setWorld(world);registration.prototype=provider;
            var source=provider.biomeSource;
            if(source==null)throw new IllegalStateException("Registered dimension has no biome source: "+registration.original.number);
            for(Class<?> type=source.getClass();type!=null;type=type.getSuperclass())for(var field:type.getDeclaredFields()){
                if(Modifier.isStatic(field.getModifiers()))continue;
                if(field.getType()!=Biome.class&&!field.getType().equals(Biome[].class))continue;
                try{field.setAccessible(true);Object value=field.get(source);
                    if(value instanceof Biome biome)biomeKey(registration.original.number,biome);
                    else if(value instanceof Biome[] biomes)for(var biome:biomes)if(biome!=null)biomeKey(registration.original.number,biome);
                }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot inspect registered biome source",error);}
            }
            for(int x:new int[]{-4096,-256,0,256,4096})for(int z:new int[]{-4096,-256,0,256,4096})biomeKey(registration.original.number,source.getBiome(x,z));
            return null;
        });
    }
    public static synchronized ResourceLocation biomeKey(int id,Biome biome){
        Objects.requireNonNull(biome,"Original biome source returned null");
        var registration=REGISTRATIONS.get(id);if(registration==null)throw new IllegalArgumentException("Unregistered dimension "+id);
        String vanilla=biome.mcc$modernBiome();if(vanilla!=null){var key=ResourceLocation.parse(vanilla);registration.biomeKeys.add(key);return key;}
        var key=ResourceLocation.fromNamespaceAndPath(registration.key.location().getNamespace(),registration.key.location().getPath()+"/biome_"+fingerprint(biome));
        registration.biomes.putIfAbsent(key,biome);registration.biomeKeys.add(key);return key;
    }
    public static synchronized List<String> biomeIds(int id){
        var registration=REGISTRATIONS.get(id);if(registration==null)return List.of();discover(registration);
        var ids=new LinkedHashSet<String>();
        for(var biome:registration.biomeKeys)ids.add(biome.toString());
        var source=registration.prototype.biomeSource;
        for(int x:new int[]{-4096,-256,0,256,4096})for(int z:new int[]{-4096,-256,0,256,4096})ids.add(biomeKey(id,source.getBiome(x,z)).toString());
        return List.copyOf(ids);
    }
    /** Discover source lists before entity categories become immutable in the native registry. */
    public static synchronized List<Biome> customBiomes(){
        var biomes=new ArrayList<Biome>();var seen=Collections.newSetFromMap(new IdentityHashMap<Biome,Boolean>());
        for(var registration:List.copyOf(REGISTRATIONS.values())){
            if(registration.original.number==0||registration.original.number==-1)continue;
            discover(registration);
            for(var biome:registration.biomes.values())if(seen.add(biome))biomes.add(biome);
        }
        return List.copyOf(biomes);
    }
    /** One original biome may contribute a separately scoped native key to several dimensions. No runtime discovery. */
    public static synchronized List<String> biomeIds(Biome original){
        String vanilla=original.mcc$modernBiome();if(vanilla!=null)return List.of(vanilla);
        var discovered=new LinkedHashMap<ResourceLocation,Biome>();
        for(var registration:REGISTRATIONS.values())discovered.putAll(registration.biomes);
        return biomeIds(original,discovered);
    }
    static List<String> biomeIds(Biome original,Map<ResourceLocation,Biome> discovered){
        String vanilla=original.mcc$modernBiome();if(vanilla!=null)return List.of(vanilla);
        var ids=new LinkedHashSet<String>();String identity=fingerprint(original);
        for(var entry:discovered.entrySet())
            if(entry.getValue()==original||fingerprint(entry.getValue()).equals(identity))ids.add(entry.getKey().toString());
        return List.copyOf(ids);
    }
    static String fingerprint(Biome biome){
        // Registry identity must survive block-ID remapping, spawn configuration, and visual changes.
        String identity=biome.getClass().getName()+"\n"+Objects.toString(biome.name,"");
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)),0,12);}
        catch(java.security.NoSuchAlgorithmException error){throw new AssertionError(error);}
    }
    public static synchronized void contribute(Map<String,Supplier<byte[]>> out){
        for(var registration:REGISTRATIONS.values()){
            int id=registration.original.number;if(id==0||id==-1)continue;
            discover(registration);List<String> biomes=biomeIds(id);if(biomes.isEmpty())throw new IllegalStateException("Registered dimension has no discovered biomes: "+id);
            ResourceLocation key=registration.key.location();
            put(out,key,"dimension_type",dimensionType(registration.prototype));
            var source=new JsonObject();source.addProperty("type","legacyconnector:legacy_b173");source.addProperty("dimension",id);
            var biomeArray=new JsonArray();biomes.forEach(biomeArray::add);source.add("biomes",biomeArray);
            var generator=new JsonObject();generator.addProperty("type","legacyconnector:legacy_b173");generator.addProperty("dimension",id);generator.add("biome_source",source);
            var stem=new JsonObject();stem.addProperty("type",key.toString());stem.add("generator",generator);put(out,key,"dimension",stem);
            for(var biome:registration.biomes.entrySet())put(out,biome.getKey(),"worldgen/biome",biomeDefinition(biome.getValue(),registration.prototype));
        }
    }
    private static void put(Map<String,Supplier<byte[]>> out,ResourceLocation key,String directory,JsonObject value){
        byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);out.put("data/"+key.getNamespace()+"/"+directory+"/"+key.getPath()+".json",()->bytes);
    }
    private static JsonObject dimensionType(Dimension provider){
        var json=new JsonObject();json.addProperty("ultrawarm",provider.evaporatesWater);json.addProperty("natural",provider.hasGround());
        json.addProperty("piglin_safe",false);json.addProperty("respawn_anchor_works",false);json.addProperty("bed_works",provider.hasWorldSpawn());json.addProperty("has_raids",false);
        json.addProperty("has_skylight",!provider.hasNoSky);json.addProperty("has_ceiling",provider.hasCeiling);json.addProperty("coordinate_scale",1);
        json.addProperty("ambient_light",provider.lightLevelToLuminance[0]);json.addProperty("min_y",0);json.addProperty("height",128);json.addProperty("logical_height",128);
        json.addProperty("infiniburn","#minecraft:infiniburn_overworld");json.addProperty("effects",org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173.EFFECTS.toString());
        json.addProperty("monster_spawn_block_light_limit",0);json.addProperty("monster_spawn_light_level",0);return json;
    }
    private static JsonObject biomeDefinition(Biome biome,Dimension provider){
        var json=new JsonObject();json.addProperty("has_precipitation",biome.canRain()||biome.canSnow());json.addProperty("temperature",biome.canSnow()?0:.8);json.addProperty("downfall",biome.canRain()?.4:0);
        var effects=new JsonObject();var fog=provider.getFogColor(0,0);
        effects.addProperty("fog_color",((int)(Math.max(0,Math.min(1,fog.x))*255)<<16)|((int)(Math.max(0,Math.min(1,fog.y))*255)<<8)|(int)(Math.max(0,Math.min(1,fog.z))*255));
        effects.addProperty("water_color",4159204);effects.addProperty("water_fog_color",329011);effects.addProperty("sky_color",biome.getSkyColor(.8F)&0xffffff);
        if(biome.grassColor!=0)effects.addProperty("grass_color",biome.grassColor&0xffffff);if(biome.foliageColor!=0)effects.addProperty("foliage_color",biome.foliageColor&0xffffff);json.add("effects",effects);
        var spawners=new JsonObject();spawners.add("monster",spawns(biome.spawnableMonsters,net.minecraft.world.entity.MobCategory.MONSTER));
        spawners.add("creature",spawns(biome.spawnablePassive,net.minecraft.world.entity.MobCategory.CREATURE));spawners.add("water_creature",spawns(biome.spawnableWaterCreatures,net.minecraft.world.entity.MobCategory.WATER_CREATURE));
        json.add("spawners",spawners);json.add("spawn_costs",new JsonObject());json.add("carvers",new JsonObject());json.add("features",new JsonArray());return json;
    }
    private static JsonArray spawns(List<EntitySpawnGroup> originals,net.minecraft.world.entity.MobCategory category){
        var array=new JsonArray();for(var spawn:originals){
            EntityType<?> type=SpawnRulesB173.resolvedType(spawn.clazz);
            if(type==null||type.getCategory()!=category||spawn.amount<=0)continue;
            var entry=new JsonObject();entry.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());entry.addProperty("weight",spawn.amount);entry.addProperty("minCount",1);entry.addProperty("maxCount",4);array.add(entry);
        }return array;
    }
}
