package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in ordinary tick observation: assert opportunities, not RNG-dependent spawning abundance. */
public final class BetaSpawnSchedulingSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173BetaScheduleTest");
    private static Session active;
    private BetaSpawnSchedulingSelfTestB173(){}
    public static void register(IEventBus bus){
        if(!Boolean.getBoolean("legacyconnector.entitySelfTest")||!Boolean.getBoolean("legacyconnector.betaSpawnSchedulingSelfTest"))return;
        bus.addListener((ServerStartedEvent event)->{
            if(AnimalsB173.all().stream().noneMatch(i->Set.of("legacyconnector_b173.mocreatures","legacyconnector_b173.morecreepsandweirdos").contains(i.registration().type().getPackageName())))return;
            try{active=new Session(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 BETA SCHEDULING TEST initialization failed",error);}
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{if(active!=null)active.tick();});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event)->{
            if(active!=null&&event.getEntity().level()==active.level&&event.getSpawnType()==net.minecraft.world.entity.MobSpawnType.NATURAL&&SpawnSchedulerB173.managed(event.getEntity().getType()))active.positionChecks.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString(),1,Integer::sum);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event)->{
            if(active!=null&&event.getEntity().level()==active.level&&event.getSpawnType()==net.minecraft.world.entity.MobSpawnType.NATURAL&&SpawnSchedulerB173.managed(event.getEntity().getType()))active.finalized.add(event.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent event)->{if(active!=null){active.finish("stopped before complete observation");active=null;}});
    }
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:beta-scheduling".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Schedule");
        var options=net.minecraft.server.level.ClientInformation.createDefault();
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,new net.minecraft.server.level.ClientInformation(options.language(),8,options.chatVisibility(),options.chatColors(),options.modelCustomisation(),options.mainHand(),options.textFilteringEnabled(),options.allowsListing()));
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static final class Session {
        final ServerLevel level;final net.minecraft.server.level.ServerPlayer player;final Set<UUID> before=new HashSet<>();
        final EnumSet<SpawnGroup> expected=EnumSet.noneOf(SpawnGroup.class),adjacent=EnumSet.noneOf(SpawnGroup.class);
        final EnumMap<SpawnGroup,Long> previous=new EnumMap<>(SpawnGroup.class);final EnumMap<SpawnGroup,Integer> opportunities=new EnumMap<>(SpawnGroup.class);
        final Set<UUID> finalized=new HashSet<>(),inserted=new HashSet<>();final Map<String,Integer> confirmedInsertions=new TreeMap<>();
        final Map<String,Integer> positionChecks=new TreeMap<>();final List<String> failures=new ArrayList<>();final SpawnSchedulerB173.Scope observer;
        final int duration=Math.max(80,Integer.getInteger("legacyconnector.betaSpawnSchedulingTestTicks",300));int ticks;
        Session(ServerLevel level){
            this.level=level;for(var entity:level.getAllEntities())before.add(entity.getUUID());
            for(var info:AnimalsB173.all())if(SpawnSchedulerB173.managed(info.type()))for(var rule:SpawnRulesB173.all())if(rule.type()==info.registration().type()&&rule.weight()>0)expected.add(rule.group());
            require(!expected.isEmpty(),"actual original spawn entries managed by Beta scheduler");
            var spawn=level.getSharedSpawnPos();int x=spawn.getX()+128,z=spawn.getZ();level.getChunk(x>>4,z>>4);
            var habitat=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,0,z));
            player=actor(level);player.setPos(habitat.getX()+.5,habitat.getY()+1,habitat.getZ()+.5);player.setInvulnerable(true);level.addNewPlayer(player);
            require(!player.isSpectator()&&level.players().contains(player),"real normal native player enters chunk tracking");
            require(level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING),"ordinary native spawning gamerule enabled");
            observer=SpawnSchedulerB173.observe(this::opportunity);
            LOG.info("B1.7.3 BETA SCHEDULING TEST started: expected={} duration={} native terrain/settings/weights/predicates/RNG unchanged",expected,duration);
        }
        void opportunity(SpawnSchedulerB173.Opportunity event){
            if(event.level()!=level)return;
            try{
                require(event.gameTime()==level.getGameTime(),"opportunity belongs to current native world tick");
                require(event.tickingChunks()>0,"Beta opportunity retains native ticking chunk eligibility");
                int footprint=SpawnCapsB173.footprint(level.players().stream().filter(p->!p.isSpectator()).map(net.minecraft.world.entity.Entity::chunkPosition).toList()).size();
                require(event.eligibleChunks()==footprint,"scheduler uses original deduplicated player footprint");
                require(event.limit()==event.group().getCapacity()*footprint/256&&event.allowed()==(event.loadedCount()<=event.limit()),"group opportunity uses original inclusive /256 budget");
                require(event.loadedCount()==SpawnCapsB173.census(level.getAllEntities()).get(event.group()),"scheduler refreshes actual loaded original-class census before each group");
                Long last=previous.put(event.group(),event.gameTime());require(last==null||last!=event.gameTime(),"group runs at most once per native world tick");
                if(last!=null&&event.gameTime()==last+1)adjacent.add(event.group());opportunities.merge(event.group(),1,Integer::sum);
            }catch(RuntimeException error){failures.add(error.getMessage());}
        }
        void tick(){
            for(var entity:level.getAllEntities())if(finalized.contains(entity.getUUID())&&inserted.add(entity.getUUID())){
                confirmedInsertions.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),1,Integer::sum);
                if(entity instanceof B173EntityHost host){
                    if(host.legacy()==null||host.legacy().mcc$modern!=entity||EntityViewsB173.wrap(entity)!=host.legacy())failures.add("naturally inserted original lost canonical host identity: "+entity.getType());
                }
            }
            EntityViewsB173.flush();
            ticks++;if(player.isRemoved()||!player.isAlive()||(player.isSpectator()&&!Boolean.getBoolean("legacyconnector.betaSpawnSchedulingControlTest"))){finish("native actor lost eligibility");active=null;return;}
            if(ticks>=duration){finish(null);active=null;}
        }
        void finish(String reason){
            observer.close();if(reason!=null)failures.add(reason);if(!adjacent.containsAll(expected))failures.add("missing adjacent native tick group opportunities: expected="+expected+" adjacent="+adjacent);
            LOG.info("B1.7.3 BETA SCHEDULING TEST {}: ticks={} opportunities={} adjacent={} actualOriginalPositionChecks={} confirmedNativeInsertions={} failures={}; insertion abundance is not asserted",failures.isEmpty()?"passed":"FAILED",ticks,opportunities,adjacent,positionChecks,confirmedInsertions,failures);
            if(!failures.isEmpty())LOG.error("B1.7.3 BETA SCHEDULING TEST failed: {}",failures);
            var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getUUID()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();
        }
    }
}
