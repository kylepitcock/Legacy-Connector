package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in observation of actual ServerChunkCache natural-spawn scheduling, without selection or predicate overrides. */
public final class NaturalSpawningTickSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173SpawnTickTest");
    private static Session active;
    private NaturalSpawningTickSelfTestB173(){}
    public static void register(IEventBus bus){
        if(!Boolean.getBoolean("legacyconnector.naturalSpawnTickSelfTest"))return;
        bus.addListener((ServerStartedEvent e)->{if(AnimalsB173.all().isEmpty())return;try{active=new Session(e.getServer().overworld());}catch(RuntimeException error){LOG.error("B1.7.3 NATURAL SPAWN TICK TEST initialization failed",error);}});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)->{if(active!=null)active.tick();});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck e)->{if(active!=null&&e.getSpawnType()==MobSpawnType.NATURAL&&e.getEntity().level()==active.level)active.positions.merge(key(e.getEntity()),1,Integer::sum);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent e)->{if(active!=null&&e.getSpawnType()==MobSpawnType.NATURAL&&e.getEntity().level()==active.level){active.finalized.add(e.getEntity().getUUID());active.finalizations.merge(key(e.getEntity()),1,Integer::sum);}});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e)->{if(active!=null){active.finish("server stopped before observation window completed");active=null;}});
    }
    private static String key(net.minecraft.world.entity.Entity entity){return entity.getClassification(true).name()+"/"+net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:natural-spawn-ticks".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173TickSpawn");
        var options=net.minecraft.server.level.ClientInformation.createDefault();
        var normalView=new net.minecraft.server.level.ClientInformation(options.language(),8,options.chatVisibility(),options.chatColors(),options.modelCustomisation(),options.mainHand(),options.textFilteringEnabled(),options.allowsListing());
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,normalView);
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static final class Session {
        final ServerLevel level;final net.minecraft.server.level.ServerPlayer player;final Set<UUID> baseline=new HashSet<>(),finalized=new HashSet<>(),observed=new HashSet<>();
        final Map<String,Integer> positions=new TreeMap<>(),finalizations=new TreeMap<>(),insertions=new TreeMap<>();final Set<String> expected=new TreeSet<>(),expectedCreatures=new TreeSet<>(),admitted=new TreeSet<>(),admittedCreatures=new TreeSet<>();
        final long startTime;final int limit;final BlockPos habitat;int ticks,cycles,eligibleChunks,maxEligibleChunks;Map<String,Integer> caps=new TreeMap<>();
        Session(ServerLevel level){this.level=level;for(var entity:level.getAllEntities())baseline.add(entity.getUUID());for(var info:AnimalsB173.all()){expected.add(info.registration().type().getPackageName());if(info.type().getCategory()==MobCategory.CREATURE)expectedCreatures.add(info.registration().type().getPackageName());}startTime=level.getGameTime();limit=Math.max(800,Integer.getInteger("legacyconnector.naturalSpawnTickTestTicks",2400));habitat=habitat(level);player=actor(level);player.setPos(habitat.getX()+.5,habitat.getY(),habitat.getZ()+.5);player.setInvulnerable(true);level.addNewPlayer(player);
            if(!level.players().contains(player)||player.isSpectator())throw new IllegalStateException("normal real player was not admitted to native player/chunk tracking");
            LOG.info("B1.7.3 NATURAL SPAWN TICK TEST started: habitat={} biome={} expected={} duration={} ticks; gamerule={} difficulty={} dayTime={}; actual generated blocks/weights/caps/predicates unchanged",habitat,level.getBiome(habitat).unwrapKey(),expected,limit,level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING),level.getDifficulty(),level.getDayTime());
        }
        void tick(){
            if(player.isRemoved()||!player.isAlive()||!level.players().contains(player)||player.isSpectator()){finish("real actor lost normal native tracking eligibility");active=null;return;}
            ticks++;if(level.getGameTime()%400==0)cycles++;
            var state=level.getChunkSource().getLastSpawnState();if(state!=null){eligibleChunks=state.getSpawnableChunkCount();maxEligibleChunks=Math.max(maxEligibleChunks,eligibleChunks);caps=new TreeMap<>();for(var category:MobCategory.values())caps.put(category.name()+"/cap"+category.getMaxInstancesPerChunk(),state.getMobCategoryCounts().getInt(category));}
            for(var entity:level.getAllEntities())if(finalized.contains(entity.getUUID())&&!baseline.contains(entity.getUUID())&&observed.add(entity.getUUID())){insertions.merge(key(entity),1,Integer::sum);if(entity instanceof B173LivingHost host&&host.legacy()!=null){if(host.legacy().mcc$modern!=entity||EntityViewsB173.wrap(entity)!=host.legacy())throw new IllegalStateException("naturally admitted original lost canonical identity");admitted.add(host.legacy().getClass().getPackageName()+"/"+entity.getClassification(true).name());if(entity.getClassification(true)==MobCategory.CREATURE)admittedCreatures.add(host.legacy().getClass().getPackageName());}}
            EntityViewsB173.flush();if(ticks%400==0)LOG.info("B1.7.3 NATURAL SPAWN TICK TEST progress: ticks={} cycles={} eligibleChunks={} categories={} originalFamilies={} positions={} finalizations={} insertions={}",ticks,cycles,eligibleChunks,caps,admitted,positions,finalizations,insertions);
            if(ticks>=limit){finish(null);active=null;}
        }
        void finish(String reason){
            boolean anyCategory=expected.stream().allMatch(family->admitted.stream().anyMatch(key->key.startsWith(family+"/")));boolean passed=reason==null&&maxEligibleChunks>0&&cycles>0&&admittedCreatures.containsAll(expectedCreatures);
            LOG.info("B1.7.3 NATURAL SPAWN TICK TEST {}: ticks={} actualWorldTicks={} creatureCycles={} maxEligibleChunks={} nativeCategoryCounts={} expectedCreatureFamilies={} admittedCreatureFamilies={} anyCategoryComplete={} admittedByCategory={} positionChecks={} naturalFinalizations={} confirmedInsertions={} reason={}",passed?"passed":"FAILED",ticks,level.getGameTime()-startTime,cycles,maxEligibleChunks,caps,expectedCreatures,admittedCreatures,anyCategory,admitted,positions,finalizations,insertions,reason);
            if(!passed)LOG.error("B1.7.3 NATURAL SPAWN TICK TEST failed: ordinary engine scheduling did not replenish every registered CREATURE family; inspect gamerule/difficulty/caps/eligible chunks and rejection funnel above");
            EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!baseline.contains(entity.getUUID()))added.add(entity);for(var entity:added)entity.discard();player.discard();
        }
    }
    private static BlockPos habitat(ServerLevel level){
        var spawn=level.getSharedSpawnPos();for(int distance:List.of(256,512,768,1024))for(int direction=0;direction<4;direction++){
            int x=spawn.getX()+(direction<2?(direction==0?distance:-distance):0),z=spawn.getZ()+(direction>=2?(direction==2?distance:-distance):0);
            for(int dx=-24;dx<=24;dx+=8)for(int dz=-24;dz<=24;dz+=8){level.getChunk((x+dx)>>4,(z+dz)>>4);var pos=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x+dx,0,z+dz));if(!level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||!level.getBlockState(pos).isAir())continue;
                boolean originals=level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(e->AnimalsB173.all().stream().anyMatch(info->info.type()==e.type)&&e.getWeight().asInt()>0);if(originals)return pos;
            }
        }
        throw new IllegalStateException("no naturally generated grassy original-creature habitat found in sampled overworld chunks");
    }
}
