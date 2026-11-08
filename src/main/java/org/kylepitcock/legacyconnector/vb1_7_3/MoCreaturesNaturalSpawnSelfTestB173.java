package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Runs the native spawn-selection/placement path against original registrations in a controlled habitat. */
public final class MoCreaturesNaturalSpawnSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173NaturalSpawnTest");
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC NATURAL SPAWN TEST failed",error);}
    });}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:natural-spawn".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Spawn");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static void run(ServerLevel level){
        var types=new HashSet<net.minecraft.world.entity.EntityType<?>>();
        for(var info:AnimalsB173.all())if(info.registration().type().getPackageName().equals("legacyconnector_b173.mocreatures")&&info.type().getCategory()==MobCategory.CREATURE)types.add(info.type());
        if(types.isEmpty())return;
        var existing=new HashSet<Integer>();for(var entity:level.getAllEntities())existing.add(entity.getId());
        var blocks=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var player=actor(level);long previousTime=level.getDayTime();
        var spawn=level.getSharedSpawnPos();
        int centerX=((spawn.getX()+64)>>4)*16+8,centerZ=(spawn.getZ()>>4)*16+8;
        int highest=level.getMinBuildHeight();
        // Keep the complete controlled platform above terrain so native skylight remains authoritative.
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            level.getChunk(new BlockPos(centerX+x,0,centerZ+z));
            highest=Math.max(highest,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,centerX+x,centerZ+z));
        }
        var center=new BlockPos(centerX,highest+2,centerZ);
        try{
            level.setDayTime(6000);
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<=4;y++){
                var pos=center.offset(x,y,z);level.getChunk(pos);blocks.putIfAbsent(pos,level.getBlockState(pos));
                level.setBlock(pos,y==-1?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            }
            player.setPos(spawn.getX()+.5,center.getY(),spawn.getZ()+.5);level.addNewPlayer(player);
            require(level.players().contains(player)&&level.getNearestPlayer(center.getX(),center.getY(),center.getZ(),-1,false)!=null,"native natural spawning has a real nearby player");
            require(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level).getLightLevel(center.getX(),center.getY(),center.getZ())>8,"controlled grass habitat is lit");
            var biome=level.getBiome(center).value();var entries=biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
            require(entries.stream().anyMatch(entry->types.contains(entry.type)&&entry.getWeight().asInt()>0),"actual biome contains original ModLoader spawn entries");
            var spawned=new ArrayList<net.minecraft.world.entity.Mob>();var chunk=level.getChunk(center);
            level.setBlock(center,Blocks.BEDROCK.defaultBlockState(),3);
            NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE,level,chunk,center,(type,pos,access)->types.contains(type),(mob,access)->spawned.add(mob));
            require(spawned.isEmpty(),"native spawn path rejects a solid occupied origin");
            level.setBlock(center,Blocks.AIR.defaultBlockState(),3);
            // Keep actual biome weights and original spawn checks. The filter isolates this mod from vanilla candidates.
            for(int attempt=0;attempt<128&&spawned.isEmpty();attempt++)
                NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE,level,chunk,center,(type,pos,access)->types.contains(type),(mob,access)->spawned.add(mob));
            require(!spawned.isEmpty(),"native spawn selection/placement failed to create an original creature in eligible habitat");
            for(var mob:spawned){
                require(mob instanceof B173LivingHost&&types.contains(mob.getType()),"native spawn factory retains the original creature host");
                var host=(B173LivingHost)mob;
                require(host.legacy()!=null&&host.legacy().mcc$modern==mob&&EntityViewsB173.wrap(mob)==host.legacy(),"naturally spawned original identity is canonical");
                require(level.getEntities((net.minecraft.world.entity.Entity)null,mob.getBoundingBox().inflate(1)).contains(mob),"naturally spawned original enters visible native entity sections");
            }
            EntityViewsB173.flush();
            LOG.info("B1.7.3 MOC NATURAL SPAWN TEST: native weighted selection, occupied-position refusal, original checks, factory and world insertion passed for {}; global cap/tick scheduling and live client spawning not implied",spawned.stream().map(entity->net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()).toList());
        }finally{
            var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!existing.contains(entity.getId()))added.add(entity);
            added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();
            blocks.forEach((pos,state)->level.setBlock(pos,state,3));level.setDayTime(previousTime);EntityViewsB173.flush();
        }
    }
}
