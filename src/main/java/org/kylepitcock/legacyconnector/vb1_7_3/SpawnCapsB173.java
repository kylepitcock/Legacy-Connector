package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/** Original Beta spawn-group budgets and class-based census, independent of native categories. */
public final class SpawnCapsB173 {
    private static volatile Set<SpawnGroup> overrides=Set.of();
    private SpawnCapsB173() {}

    /** Records successful mapped writes for diagnostics; default capacities are equally authoritative. */
    public static synchronized void afterWrite(Field field,Object instance) {
        if(field.getDeclaringClass()!=SpawnGroup.class||!field.getName().equals("capacity")||!(instance instanceof SpawnGroup group))return;
        var updated=EnumSet.noneOf(SpawnGroup.class);updated.addAll(overrides);updated.add(group);overrides=Set.copyOf(updated);
    }
    public static boolean hasOverrides(){return !overrides.isEmpty();}
    public static boolean isOverridden(SpawnGroup group){return overrides.contains(group);}

    public static int limit(SpawnGroup group,int eligibleChunkCount){return group.getCapacity()*eligibleChunkCount/256;}

    /** Evaluate once before the group's chunk loop, rather than recapping after each insertion. */
    public static boolean allows(SpawnGroup group,int loadedCount,int eligibleChunkCount){return loadedCount<=limit(group,eligibleChunkCount);}

    public static Set<ChunkPos> footprint(Collection<ChunkPos> playerChunks){
        var chunks=new HashSet<ChunkPos>();
        for(var center:playerChunks)for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)chunks.add(new ChunkPos(center.x+x,center.z+z));
        return chunks;
    }

    public static boolean countsMembership(SpawnGroup group,Entity entity){
        if(entity instanceof B173EntityHost host&&host.legacy()!=null)return countsType(group,host.legacy().getClass(),entity.getClass());
        return countsType(group,null,entity.getClass());
    }

    /** Class-only seam keeps ancestry checks testable without initializing a game world. */
    static boolean countsType(SpawnGroup group,Class<?> originalType,Class<?> nativeType){
        if(originalType!=null)return group.getCreatureClass().isAssignableFrom(originalType);
        return switch(group){
            case MONSTER->net.minecraft.world.entity.monster.Enemy.class.isAssignableFrom(nativeType);
            case CREATURE->net.minecraft.world.entity.animal.Animal.class.isAssignableFrom(nativeType);
            case WATER_CREATURE->net.minecraft.world.entity.animal.WaterAnimal.class.isAssignableFrom(nativeType);
        };
    }

    /** Beta counts every loaded entity, including persistent entities and corpses awaiting removal. */
    public static EnumMap<SpawnGroup,Integer> census(Iterable<? extends Entity> loaded){
        var counts=new EnumMap<SpawnGroup,Integer>(SpawnGroup.class);
        for(var group:SpawnGroup.values())counts.put(group,0);
        for(var entity:loaded)for(var group:SpawnGroup.values())if(countsMembership(group,entity))counts.merge(group,1,Integer::sum);
        return counts;
    }
}
