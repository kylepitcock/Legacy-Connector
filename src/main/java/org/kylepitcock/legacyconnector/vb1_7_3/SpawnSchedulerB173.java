package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.kylepitcock.legacyconnector.mixin.BetaSpawnStateAccess;

/** Beta cap decisions and cadence around the native selection, placement and insertion pipeline. */
public final class SpawnSchedulerB173 {
    public interface Scope extends AutoCloseable { @Override void close(); }
    private static final Scope NOOP=()->{};
    private enum Lane { NONE, ORDINARY, BETA }
    private static final ThreadLocal<Cycle> CURRENT=new ThreadLocal<>();
    private static final List<Consumer<Opportunity>> OBSERVERS=new CopyOnWriteArrayList<>();
    private static volatile Catalog cached=new Catalog(-1,Map.of(),Set.of());

    /** Observation does not evaluate original predicates or consume selection randomness. */
    public record Opportunity(ServerLevel level,long gameTime,SpawnGroup group,int eligibleChunks,
                              int loadedCount,int limit,boolean allowed,int tickingChunks) {}
    static record Policy(EntityType<?> type,SpawnGroup group,int weight,boolean overworld,List<String> biomes) {
        boolean matches(Holder<Biome> biome){
            return overworld?biome.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD)
                    :org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyEntitySpawns.matchesBiome(biome,biomes);
        }
    }
    static record Catalog(long revision,Map<SpawnGroup,List<Policy>> groups,Set<EntityType<?>> managed) {}
    private record Selection(SpawnGroup group,Holder<Biome> biome,boolean positiveOnly) {}
    private static final class Cycle {
        final ServerLevel level;
        final Catalog catalog;
        final Map<ChunkPos,LevelChunk> chunks=new LinkedHashMap<>();
        final Map<Selection,Set<EntityType<?>>> ownership=new HashMap<>();
        NaturalSpawner.SpawnState state;
        Lane lane=Lane.NONE;
        SpawnGroup group;
        boolean spawned;
        Cycle(ServerLevel level,Catalog catalog){this.level=level;this.catalog=catalog;}
    }
    private SpawnSchedulerB173() {}

    public static void register(net.neoforged.bus.api.IEventBus bus){
        // The native custom-spawner lane already supplies its gamerule/tick gates and side flags.
        // Run first, preserving the ordering of the former invocation hook and every other spawner.
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent event)->
                        event.getCustomSpawners().add(0,(level,enemies,friendlies)->{
                            spawn(level,friendlies,enemies);
                            return 0;
                        }));
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns event)->{
                    Cycle cycle=CURRENT.get();
                    if(cycle==null||cycle.lane==Lane.NONE||event.getLevel()!=cycle.level)return;
                    var original=WeightedRandomList.create(event.getSpawnerDataList());
                    var filtered=filter(cycle.level,event.getMobCategory(),event.getPos(),null,original);
                    if(filtered!=original)for(var entry:List.copyOf(event.getSpawnerDataList()))
                        if(!filtered.unwrap().contains(entry))event.removeSpawnerData(entry);
                });
    }

    private static Catalog catalog(){
        long revision=SpawnRulesB173.revision();
        Catalog previous=cached;
        if(previous.revision==revision)return previous;
        Catalog result=catalog(revision,SpawnRulesB173.changes(),SpawnRulesB173::hostedType,SpawnRulesB173::biomeIds);
        cached=result;
        return result;
    }
    static Catalog catalog(long revision,List<SpawnRulesB173.Entry> entries,
            java.util.function.Function<Class<?>,EntityType<?>> resolve,
            java.util.function.Function<legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome,List<String>> biomeIds){
        var groups=new EnumMap<SpawnGroup,List<Policy>>(SpawnGroup.class);
        var managed=new HashSet<EntityType<?>>();
        for(var entry:entries){
            EntityType<?> type=resolve.apply(entry.type());
            var biomes=new LinkedHashSet<String>();boolean complete=true;
            for(var biome:entry.biomes()){var ids=biomeIds.apply(biome);if(ids.isEmpty()){complete=false;break;}biomes.addAll(ids);}
            if(type!=null&&complete&&!biomes.isEmpty()){
                var standard=legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome.mcc$standardBiomes();
                boolean overworld=entry.biomes().size()==standard.size()&&entry.biomes().containsAll(standard);
                groups.computeIfAbsent(entry.group(),ignored->new ArrayList<>()).add(new Policy(type,entry.group(),entry.weight(),overworld,List.copyOf(biomes)));
                managed.add(type);
            }
        }
        groups.replaceAll((group,policies)->List.copyOf(policies));
        return new Catalog(revision,Map.copyOf(groups),Set.copyOf(managed));
    }
    public static boolean managed(EntityType<?> type){return catalog().managed.contains(type);}
    public static Set<EntityType<?>> ownedTypes(SpawnGroup group,Holder<Biome> biome,boolean positiveOnly){
        return owned(catalog().groups.getOrDefault(group,List.of()),group,biome,positiveOnly);
    }

    private static Set<EntityType<?>> selection(Cycle cycle,SpawnGroup group,Holder<Biome> biome,boolean positiveOnly){
        return cycle.ownership.computeIfAbsent(new Selection(group,biome,positiveOnly),ignored->{
            return owned(cycle.catalog.groups.getOrDefault(group,List.of()),group,biome,positiveOnly);
        });
    }

    private static WeightedRandomList<MobSpawnSettings.SpawnerData> choices(Cycle cycle,SpawnGroup group,LevelChunk chunk){
        var center=new BlockPos(chunk.getPos().getMiddleBlockX(),64,chunk.getPos().getMiddleBlockZ());
        var biome=cycle.level.getBiome(center);
        var entries=new ArrayList<MobSpawnSettings.SpawnerData>();
        var positive=owned(cycle.catalog.groups.getOrDefault(group,List.of()),group,biome,true);
        for(var entry:biome.value().getMobSettings().getMobs(group.mcc$modern()).unwrap())if(positive.contains(entry.type))entries.add(entry);
        return net.neoforged.neoforge.event.EventHooks.getPotentialSpawns(cycle.level,group.mcc$modern(),center,WeightedRandomList.create(entries));
    }

    /** Only hosted legacy entities are scheduled here; vanilla API requests keep native lifecycle rules. */
    public static Scope cycle(ServerLevel level){
        Catalog catalog=catalog();
        if(catalog.managed.isEmpty())return NOOP;
        Cycle previous=CURRENT.get(),current=new Cycle(level,catalog);
        CURRENT.set(current);
        return ()->{if(previous==null)CURRENT.remove();else CURRENT.set(previous);};
    }

    /** Capture only chunks that passed native player, border, gamerule and tick-rate eligibility. */
    public static void ordinary(ServerLevel level,LevelChunk chunk,NaturalSpawner.SpawnState state,
                                boolean friendlies,boolean enemies,Runnable original){
        Cycle cycle=CURRENT.get();
        if(cycle==null||cycle.level!=level){original.run();return;}
        cycle.chunks.put(chunk.getPos(),chunk);
        cycle.state=state;
        Lane previous=cycle.lane;
        cycle.lane=Lane.ORDINARY;
        try{original.run();}finally{cycle.lane=previous;}
    }

    /** Called once from the native custom-spawner stage, independently of its 400-tick passive gate. */
    public static void spawn(ServerLevel level,boolean friendlies,boolean enemies){
        Cycle cycle=CURRENT.get();
        if(cycle==null||cycle.level!=level||cycle.spawned)return;
        cycle.spawned=true;
        var players=new ArrayList<ChunkPos>();
        for(var player:level.players())if(!player.isSpectator())players.add(player.chunkPosition());
        Set<ChunkPos> footprint=SpawnCapsB173.footprint(players);
        if(footprint.isEmpty()||cycle.state==null)return;
        var chunks=cycle.chunks.entrySet().stream().filter(entry->footprint.contains(entry.getKey())).map(Map.Entry::getValue).toList();
        if(chunks.isEmpty())return;
        var state=(BetaSpawnStateAccess)(Object)cycle.state;
        Lane previous=cycle.lane;
        SpawnGroup previousGroup=cycle.group;
        cycle.lane=Lane.BETA;
        try{
            for(var group:SpawnGroup.values()){
                if(cycle.catalog.groups.getOrDefault(group,List.of()).stream().noneMatch(policy->policy.weight>0))continue;
                // Beta gates once before the whole group's chunk loop. Accepted clusters may overshoot.
                int count=SpawnCapsB173.census(level.getAllEntities()).get(group);
                boolean enabled=group.isPeaceful()?friendlies:enemies&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL;
                boolean allowed=enabled&&SpawnCapsB173.allows(group,count,footprint.size());
                var opportunity=new Opportunity(level,level.getGameTime(),group,footprint.size(),count,
                        SpawnCapsB173.limit(group,footprint.size()),allowed,chunks.size());
                for(var observer:OBSERVERS)observer.accept(opportunity);
                if(!allowed)continue;
                cycle.group=group;
                for(var chunk:chunks)BetaSpawnPlacementB173.spawnChunk(group,level,chunk,choices(cycle,group,chunk),
                        state::legacyconnector$canSpawn,state::legacyconnector$afterSpawn);
            }
        }finally{cycle.group=previousGroup;cycle.lane=previous;}
    }

    /** Filter before weighted selection, and use the same view for native membership validation. */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> filter(ServerLevel level,MobCategory category,BlockPos pos,
            Holder<Biome> biome,WeightedRandomList<MobSpawnSettings.SpawnerData> original){
        Cycle cycle=CURRENT.get();
        if(cycle==null||cycle.level!=level||cycle.lane==Lane.NONE)return original;
        SpawnGroup group=null;
        for(var candidate:SpawnGroup.values())if(candidate.mcc$modern()==category){group=candidate;break;}
        if(group==null)return cycle.lane==Lane.BETA?WeightedRandomList.create():original;
        Holder<Biome> current=biome!=null?biome:level.getBiome(pos);
        Set<EntityType<?>> owned=selection(cycle,group,current,false);
        Set<EntityType<?>> selected=cycle.lane==Lane.BETA
                ?selection(cycle,group,current,true):Set.of();
        return filter(original,owned,selected,cycle.lane==Lane.BETA,
                cycle.group!=null&&category==cycle.group.mcc$modern());
    }
    static Set<EntityType<?>> owned(List<Policy> policies,SpawnGroup group,Holder<Biome> biome,boolean positiveOnly){
        var result=new HashSet<EntityType<?>>();
        for(var policy:policies)if(policy.group==group&&(!positiveOnly||policy.weight>0)&&policy.matches(biome))result.add(policy.type);
        return Set.copyOf(result);
    }
    static WeightedRandomList<MobSpawnSettings.SpawnerData> filter(WeightedRandomList<MobSpawnSettings.SpawnerData> original,
            Set<EntityType<?>> managed,Set<EntityType<?>> selected,boolean beta,boolean matchingCategory){
        var entries=original.unwrap();
        var retained=entries.stream().filter(entry->beta?matchingCategory&&selected.contains(entry.type):!managed.contains(entry.type)).toList();
        return retained.size()==entries.size()?original:WeightedRandomList.create(retained);
    }
    static Scope observe(Consumer<Opportunity> observer){OBSERVERS.add(Objects.requireNonNull(observer));return ()->OBSERVERS.remove(observer);}
}
