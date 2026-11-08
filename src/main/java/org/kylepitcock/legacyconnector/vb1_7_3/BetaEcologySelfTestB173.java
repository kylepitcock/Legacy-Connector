package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.NaturalSpawner;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual hosted-mod ecology acceptance; native features and population policies remain unchanged. */
public final class BetaEcologySelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173EcologyTest");
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest")&&Boolean.getBoolean("legacyconnector.betaEcologySelfTest"))bus.addListener((ServerStartedEvent event)->{
        if(AnimalsB173.all().stream().noneMatch(i->Set.of("legacyconnector_b173.mocreatures","legacyconnector_b173.morecreepsandweirdos").contains(i.registration().type().getPackageName())))return;
        try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 ECOLOGY TEST failed",error);}
    });}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:ecology".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Ecology");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };return player;
    }
    private static void run(ServerLevel level)throws ReflectiveOperationException {
        var before=new HashSet<UUID>();for(var entity:level.getAllEntities())before.add(entity.getUUID());var actors=new ArrayList<Entity>();var player=actor(level);actors.add(player);
        var spawn=level.getSharedSpawnPos();var origin=spawn.offset(512,80,0);level.getChunk(origin);level.getChunk(origin.offset(200,0,0));
        player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);level.addNewPlayer(player);
        try{
            var shapePos=origin.above(16);var saved=level.getBlockState(shapePos);
            var shapeCases=List.of(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                    net.minecraft.world.level.block.Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),
                    net.minecraft.world.level.block.Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE),
                    net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState(),net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(),
                    net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState(),net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(),
                    net.minecraft.world.level.block.Blocks.PISTON.defaultBlockState(),net.minecraft.world.level.block.Blocks.STICKY_PISTON.defaultBlockState());
            try{for(int i=0;i<shapeCases.size();i++){
                level.setBlock(shapePos,shapeCases.get(i),2);
                require(BetaSpawnPlacementB173.solid(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level),shapePos)==(i==0||i==2||i==6),"legacy candidate geometry respects original full-cube flags and transparent material: "+shapeCases.get(i));
            }}finally{level.setBlock(shapePos,saved,2);}
            var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
            var hosted=SpawnSchedulerB173.ownedTypes(SpawnGroup.CREATURE,biome,false);require(!hosted.isEmpty(),"actual hosted Beta creature policy exists in bound plains biome");
            require(!SpawnSchedulerB173.managed(EntityType.COW)&&!SpawnSchedulerB173.managed(EntityType.PIG)
                    &&!SpawnSchedulerB173.managed(EntityType.SQUID),"vanilla entity types keep their native spawning lane");
            var source=biome.value().getMobSettings().getMobs(MobCategory.CREATURE);var filtered=BetaEcologyB173.population(biome,source);
            var expected=source.unwrap().stream().filter(entry->!hosted.contains(entry.type)).toList();
            require(filtered.unwrap().equals(expected),"native population removes only hosted original entries");
            require(expected.stream().anyMatch(entry->net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).getNamespace().equals("minecraft")),"actual vanilla population entries retained");
            for(int i=0;i<expected.size();i++)require(filtered.unwrap().get(i)==expected.get(i),"every unowned native/foreign population entry preserves identity, weight and pack sizes");
            int[] hostedPositions={0},hostedFinalizations={0},nativePositions={0};
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck> position=e->{if(e.getLevel()==level&&e.getSpawnType()==MobSpawnType.CHUNK_GENERATION){if(e.getEntity() instanceof B173EntityHost)hostedPositions[0]++;else nativePositions[0]++;}};
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent> finalize=e->{if(e.getLevel()==level&&e.getSpawnType()==MobSpawnType.CHUNK_GENERATION&&e.getEntity() instanceof B173EntityHost)hostedFinalizations[0]++;};
            NeoForge.EVENT_BUS.addListener(position);NeoForge.EVENT_BUS.addListener(finalize);
            try{NaturalSpawner.spawnMobsForChunkGeneration(level,biome,new net.minecraft.world.level.ChunkPos(origin),level.getRandom());}finally{NeoForge.EVENT_BUS.unregister(position);NeoForge.EVENT_BUS.unregister(finalize);}
            require(hostedPositions[0]==0&&hostedFinalizations[0]==0,"native terrain population does not duplicate hosted originals; native fauna remains eligible");
            for(var type:List.of(EntityType.COW,EntityType.PIG)){
                var wild=type.create(level);require(wild!=null,"native wild actor constructs");actors.add(wild);wild.setPos(origin.getX()+200.5,origin.getY(),origin.getZ()+.5);level.addFreshEntity(wild);wild.checkDespawn();require(!wild.isRemoved(),"ordinary native wild fauna retains vanilla far-player persistence: "+type);
            }
            var originalCow=new legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level)){
                @Override public int getLimitPerChunk(){return 7;}
            };
            var hostedCow=new LegacyB173Cow(EntityType.COW,level,originalCow);actors.add(hostedCow);hostedCow.setPos(origin.getX()+200.5,origin.getY(),origin.getZ()+.5);level.addFreshEntity(hostedCow);
            require(hostedCow.getMaxSpawnClusterSize()==7,"hosted original Cow preserves dynamic cluster-size override");
            hostedCow.checkDespawn();hostedCow.customServerAiStep();require(hostedCow.isRemoved(),"hosted original Cow executes Beta far-player phase while native Cow remains untouched");
            for(var info:AnimalsB173.all())if(info.registration().type().getPackageName().equals("legacyconnector_b173.mocreatures")&&info.registration().type().getSimpleName().equals("EntityBunny"))for(boolean tame:List.of(false,true)){
                var bunny=info.type().create(level);require(bunny!=null,"actual Bunny constructs");actors.add(bunny);var field=bunny.legacy().getClass().getDeclaredField("tamed");field.setBoolean(bunny.legacy(),tame);bunny.setPos(origin.getX()+200.5,origin.getY(),origin.getZ()+.5);level.addFreshEntity(bunny);require(bunny.legacy().mcc$canDespawn()!=tame,"actual original Bunny canDespawn override retained");bunny.checkDespawn();bunny.serverAIPhase();require(bunny.isRemoved()!=tame,"native host respects original Bunny taming despawn policy");
            }
            LOG.info("B1.7.3 ECOLOGY TEST passed: hosted-only generation filter, unchanged native population entries, native CHUNK_GENERATION position checks observed={}, native Cow/Pig persistence and original Bunny policies when present; GenerateSurface is covered separately by existing worldgen smoke",nativePositions[0]);
        }finally{actors.forEach(Entity::discard);var added=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getUUID()))added.add(entity);added.forEach(Entity::discard);EntityViewsB173.flush();}
    }
}
