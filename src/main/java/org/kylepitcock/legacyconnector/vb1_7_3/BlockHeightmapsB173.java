package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Reconcile saved native material heightmaps with the currently loaded Beta block properties. */
public final class BlockHeightmapsB173 {
    private static final EnumSet<Heightmap.Types> TYPES=EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Heightmap.Types.OCEAN_FLOOR);
    private BlockHeightmapsB173(){}
    public static void register(net.neoforged.bus.api.IEventBus bus){
        bus.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load event)->{
            // Load can precede FULL promotion. Only access the supplied chunk, never the level or neighbors.
            if(event.getChunk() instanceof LevelChunk chunk&&refresh(chunk)&&!event.getLevel().isClientSide())
                chunk.setUnsaved(true);
        });
    }
    static boolean refresh(ChunkAccess chunk){
        boolean affected=false;
        for(var section:chunk.getSections())if(section!=null&&section.maybeHas(state->
                state.getBlock() instanceof LegacyB173Block block&&block.legacy().material!=null
                        &&block.legacy().material.blocksMovement())){affected=true;break;}
        if(!affected)return false;
        var before=new EnumMap<Heightmap.Types,long[]>(Heightmap.Types.class);
        for(var type:TYPES){
            var map=chunk.getOrCreateHeightmapUnprimed(type);var raw=map.getRawData();
            before.put(type,raw.clone());
            // Packed zero means firstAvailable=minBuildHeight, including negative-height native worlds.
            // Prime alone does not clear stale entries in columns that no longer match any block.
            Arrays.fill(raw,0L);
        }
        try{Heightmap.primeHeightmaps(chunk,TYPES);}
        catch(RuntimeException|Error error){
            for(var type:TYPES)chunk.setHeightmap(type,before.get(type));
            throw error;
        }
        for(var type:TYPES)if(!Arrays.equals(before.get(type),chunk.getOrCreateHeightmapUnprimed(type).getRawData()))return true;
        return false;
    }
}
