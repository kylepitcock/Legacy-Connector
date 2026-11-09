package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in proof that explicit Beta decoration spawns enter native proto-chunk entity NBT. */
public final class WorldGenEntitiesSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173WorldGenEntitiesTest");
    private WorldGenEntitiesSelfTestB173(){}
    public static void register(IEventBus bus){
        if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
            try{run(event.getServer().overworld());}
            catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 WORLDGEN ENTITY TEST failed",error);}
        });
    }
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static void run(ServerLevel level){
        var step=ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        int radius=step.directDependencies().getRadius(),cx=20_000,cz=20_000;
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var chunks=new java.util.HashMap<ChunkPos,ProtoChunk>();
        var cache=StaticCache2D.<GenerationChunkHolder>create(cx,cz,radius,(x,z)->{
            var pos=new ChunkPos(x,z);var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,biomes,null);chunk.setPersistedStatus(ChunkStatus.CARVERS);chunks.put(pos,chunk);
            return new GenerationChunkHolder(pos){
                @Override public int getTicketLevel(){return 33;}
                @Override public int getQueueLevel(){return 0;}
                @Override public net.minecraft.world.level.chunk.ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status){return chunk;}
            };
        });
        var center=chunks.get(new ChunkPos(cx,cz));var region=new WorldGenRegion(level,cache,step,center);
        var world=World.mcc$wrap(level);
        EntityViewsB173.flush();
        try(var scope=World.mcc$generation(region)){
            // Cache membership is insufficient: a native early proto chunk contains no terrain yet.
            require(region.hasChunk(cx,cz),"fixture center is present in the native region cache");
            for(var status:java.util.List.of(ChunkStatus.EMPTY,ChunkStatus.BIOMES)){
                center.setPersistedStatus(status);
                boolean rejected=false;
                try{world.getBlockId(cx*16,80,cz*16);}
                catch(World.SpawnRegionUnavailable expected){rejected=true;}
                require(rejected,"cached terrain-unready "+status+" read rejects instead of reporting air");
            }
            center.setPersistedStatus(ChunkStatus.NOISE);
            require(world.getBlockId(cx*16,80,cz*16)==0,"terrain-generated native empty space remains readable air");
            center.setPersistedStatus(ChunkStatus.CARVERS);
            var item=new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,cx*16+.5,80,cz*16+.5,
                    new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(280,1,0));
            require(world.spawnEntity(item),"original item API accepts generation spawn");
            var arrow=new legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity(world);
            arrow.setPosition(cx*16+1.5,80,cz*16+.5);
            require(world.spawnEntity(arrow),"original exact arrow API accepts generation spawn");
            require(center.getEntities().size()==2,"both explicit spawns are serialized in the supplied proto chunk");
            for(var original:java.util.List.of(item,arrow)){
                var uuid=original.mcc$modern.getUUID();
                require(center.getEntities().stream().anyMatch(tag->tag.hasUUID("UUID")&&tag.getUUID("UUID").equals(uuid)),"proto-chunk NBT retains spawned UUID");
                require(level.getEntity(uuid)==null,"generation spawn bypasses live server entity insertion");
            }
        }finally{EntityViewsB173.flush();}
        require(world.mcc$access()==level,"generation entity scope restores ordinary level");
        LOG.info("B1.7.3 WORLDGEN ENTITY TEST: original item and arrow serialize into native proto chunks with UUIDs; ordinary backing level restored");
    }
}
