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
public final class MoCreaturesWaterSpawnSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173WaterSpawnTest");
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC WATER SPAWN TEST failed",error);}
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
        var selected=AnimalsB173.all().stream().filter(info->info.registration().type().getPackageName().equals("legacyconnector_b173.mocreatures")&&Set.of("EntityFishy","EntityDolphin","EntityShark").contains(info.registration().type().getSimpleName())).toList();
        if(selected.isEmpty())return;require(selected.size()==3,"all three original aquatic species registered");
        var existing=new HashSet<Integer>();for(var entity:level.getAllEntities())existing.add(entity.getId());
        var blocks=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var player=actor(level);var spawn=level.getSharedSpawnPos();
        var center=new BlockPos(((spawn.getX()+64)>>4)*16+8,level.getSeaLevel()-8,(spawn.getZ()>>4)*16+8);
        var failures=new ArrayList<String>();
        try{
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-3;y<=4;y++){
                var pos=center.offset(x,y,z);level.getChunk(pos);blocks.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
            }
            player.setPos(spawn.getX()+.5,center.getY()+8,spawn.getZ()+.5);level.addNewPlayer(player);
            require(level.players().contains(player),"aquatic natural spawn proof has native player");
            var chunk=level.getChunk(center);var biome=level.getBiome(center).value();
            for(var info:selected){
                String name=info.registration().type().getSimpleName();var type=info.type();
                try{
                    require(type.getCategory()==MobCategory.WATER_CREATURE,"original aquatic category retained: "+name);
                    require(biome.getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream().anyMatch(entry->entry.type==type&&entry.getWeight().asInt()>0),"actual biome weighted aquatic entry present: "+name);
                    var candidate=type.create(level);require(candidate!=null,"native aquatic candidate factory: "+name);
                    try{
                        candidate.moveTo(center.getX()+.5,center.getY(),center.getZ()+.5,0,0);
                        require(candidate.legacy() instanceof legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity,"actual original water superclass: "+name);
                        require(net.minecraft.world.entity.SpawnPlacements.getPlacementType(type).isSpawnPositionOk(level,center,type),"native water placement admits habitat: "+name);
                        require(candidate.checkSpawnRules(level,net.minecraft.world.entity.MobSpawnType.NATURAL),"unchanged original aquatic frequency/difficulty/entity-overlap predicate admits habitat: "+name);
                        require(candidate.checkSpawnObstruction(level),"native final obstruction admits original water creature in water: "+name);
                    }finally{candidate.discard();EntityViewsB173.flush();}
                    var spawned=new ArrayList<net.minecraft.world.entity.Mob>();
                    for(int attempt=0;attempt<256&&spawned.isEmpty();attempt++)
                        NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_CREATURE,level,chunk,center,(candidateType,pos,access)->candidateType==type,(mob,access)->spawned.add(mob));
                    require(!spawned.isEmpty(),"complete native aquatic selection/finalization/insertion path admits actual species: "+name);
                    for(var mob:spawned){require(mob.getType()==type&&mob instanceof B173LivingHost,"native aquatic spawn retains original host: "+name);require(level.getEntities((net.minecraft.world.entity.Entity)null,mob.getBoundingBox().inflate(.1)).contains(mob),"native aquatic spawn reaches loaded query sections: "+name);mob.discard();}
                    EntityViewsB173.flush();LOG.info("B1.7.3 MOC WATER SPAWN TEST: {} original rules, native water obstruction and complete spawn insertion passed",name);
                }catch(RuntimeException|LinkageError error){failures.add(name+": "+error);LOG.error("B1.7.3 MOC WATER SPAWN TEST failed: {}",name,error);}
            }
        }finally{
            var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!existing.contains(entity.getId()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();
            blocks.forEach((pos,state)->level.setBlock(pos,state,3));EntityViewsB173.flush();
        }
        require(failures.isEmpty(),"B1.7.3 MOC WATER SPAWN TEST: "+failures);LOG.info("B1.7.3 MOC WATER SPAWN TEST: all three actual aquatic species passed unchanged original predicates and complete native spawn path");
    }
}
