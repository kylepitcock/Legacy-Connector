package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original oversized spawn queries against a native bounded generation region. */
public final class MoreCreepsGenerationEdgeSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173GenerationEdgeTest");
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 GENERATION EDGE TEST failed",error);}});}
    public static void run(ServerLevel level)throws ReflectiveOperationException {
        var registration=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityFloobShip")).findFirst();if(registration.isEmpty())return;
        var original=(LivingEntity)registration.get().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));
        var random=legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class.getDeclaredField("random");random.setAccessible(true);random.set(original,new Random(){@Override public int nextInt(int bound){return 0;}});
        var spawn=level.getSharedSpawnPos();int cx=spawn.getX()>>4,cz=spawn.getZ()>>4;double y=level.getMaxBuildHeight()-12;
        original.setPosition(cx*16+8,y,cz*16+8);require(World.mcc$wrap(level).spawnEntity(original)&&original.mcc$modern instanceof LegacyB173Animal,"actual FloobShip enters generic native host");EntityViewsB173.flush();
        var host=(LegacyB173Animal)original.mcc$modern;var world=World.mcc$wrap(level);long time=level.getDayTime();
        var step=net.minecraft.world.level.chunk.status.ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.SPAWN);int radius=step.directDependencies().getRadius();
        var center=level.getChunk(cx,cz);
        var cache=net.minecraft.util.StaticCache2D.<net.minecraft.server.level.GenerationChunkHolder>create(cx,cz,radius,(x,z)->{
            var chunk=level.getChunk(x,z);
            return new net.minecraft.server.level.GenerationChunkHolder(new net.minecraft.world.level.ChunkPos(x,z)){
                @Override public int getTicketLevel(){return 33;}
                @Override public int getQueueLevel(){return 0;}
                @Override public net.minecraft.world.level.chunk.ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status){return chunk;}
            };
        });
        var region=new net.minecraft.server.level.WorldGenRegion(level,cache,step,center);
        var stone=new net.minecraft.core.BlockPos(cx*16+8,(int)y,cz*16+8);var before=level.getBlockState(stone);
        try{
            level.setDayTime(6000);require(original.width==4&&original.height==3,"actual FloobShip keeps original oversized dimensions");
            require(host.checkSpawnRules(region,MobSpawnType.CHUNK_GENERATION),"actual FloobShip accepts clear interior of available region");
            int edge=(cx+radius)*16;
            for(double x:new double[]{edge+15.5,edge+13}){
                host.setPos(x,y,cz*16+8);require(!host.checkSpawnRules(region,MobSpawnType.CHUNK_GENERATION),"actual FloobShip rejects unavailable collision body/shell at generation edge x="+x);
                require(world.mcc$access()==level,"generation context is restored after rejected original spawn");
            }
            host.setPos(cx*16+8,y,cz*16+8);level.setBlock(stone,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            require(!host.checkSpawnRules(region,MobSpawnType.CHUNK_GENERATION),"actual original collision checks still reject available solid interior");level.setBlock(stone,before,3);
            host.setPos(edge+15.5,y,cz*16+8);level.getChunk((int)Math.floor(host.getX()+4)>>4,cz);
            require(host.checkSpawnRules(level,MobSpawnType.NATURAL),"ordinary loaded-level spawn query keeps original clear-site behavior");
            try(var scope=World.mcc$generation(region)){
                try{world.getBlockId((cx+radius+1)*16,(int)y,cz*16+8);throw new IllegalStateException("missing generation block read was treated as air");}catch(World.SpawnRegionUnavailable expected){}
                try{world.mcc$requireSpawnCollisionArea(new net.minecraft.world.phys.AABB(edge+13,y,cz*16+6,edge+15,y+3,cz*16+10));throw new IllegalStateException("missing collision shell was treated as empty");}catch(World.SpawnRegionUnavailable expected){}
            }
            require(world.mcc$access()==level,"normal backing level restored after explicit unavailable-read rejection");
            host.setPos(cx*16+8,y,cz*16+8);
            random.set(original,new Random(){@Override public int nextInt(int bound){throw new IllegalArgumentException("original spawn sentinel");}});
            for(var access:java.util.List.<net.minecraft.world.level.LevelAccessor>of(region,level)){
                boolean propagated=false;try{host.checkSpawnRules(access,MobSpawnType.CHUNK_GENERATION);}catch(IllegalArgumentException expected){require("original spawn sentinel".equals(expected.getMessage()),"unrelated original exception identity preserved");propagated=true;}
                require(propagated&&world.mcc$access()==level,"unrelated original exceptions propagate and restore region context");
            }
            LOG.info("B1.7.3 GENERATION EDGE TEST: actual FloobShip interior, body/shell boundary rejection, solid collision, ordinary fallback and scope cleanup passed");
        }finally{level.setBlock(stone,before,3);level.setDayTime(time);host.discard();EntityViewsB173.flush();}
    }
}
