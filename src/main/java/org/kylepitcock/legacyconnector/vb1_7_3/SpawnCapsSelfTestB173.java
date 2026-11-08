package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original class census and inclusive Beta budget; native category budgets stay independent. */
public final class SpawnCapsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173SpawnCapsTest");
    private SpawnCapsSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        if(AnimalsB173.all().stream().noneMatch(i->Set.of("legacyconnector_b173.mocreatures","legacyconnector_b173.morecreepsandweirdos").contains(i.registration().type().getPackageName())))return;
        try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 SPAWN CAPS TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static void run(net.minecraft.server.level.ServerLevel level){
        boolean moc=AnimalsB173.all().stream().anyMatch(i->i.registration().type().getPackageName().equals("legacyconnector_b173.mocreatures"));
        require(SpawnGroup.MONSTER.getCapacity()==70&&SpawnGroup.CREATURE.getCapacity()==(moc?30:15)&&SpawnGroup.WATER_CREATURE.getCapacity()==(moc?25:5),"actual original initialization retains MoC70/30/25 or untouched Beta70/15/5 defaults");
        require(MobCategory.MONSTER.getMaxInstancesPerChunk()==70&&MobCategory.CREATURE.getMaxInstancesPerChunk()==10&&MobCategory.WATER_CREATURE.getMaxInstancesPerChunk()==5,"Beta settings do not rewrite native category budgets");
        var footprint=SpawnCapsB173.footprint(List.of(new ChunkPos(-1,-1),new ChunkPos(-1,-1)));
        require(footprint.size()==289&&footprint.contains(new ChunkPos(-9,-9))&&footprint.contains(new ChunkPos(7,7)),"original radius8 footprint is inclusive, deduplicated and negative-coordinate safe");
        require(SpawnCapsB173.footprint(List.of(new ChunkPos(0,0),new ChunkPos(32,32))).size()==578,"disjoint native players contribute distinct Beta chunk footprints");
        for(var group:SpawnGroup.values())for(int chunks:new int[]{0,1,256,289,578}){
            int threshold=group.getCapacity()*chunks/256;
            require(SpawnCapsB173.limit(group,chunks)==threshold,"original /256 integer limit "+group+" chunks="+chunks);
            require(SpawnCapsB173.allows(group,threshold,chunks)&&!SpawnCapsB173.allows(group,threshold+1,chunks),"original equality admits and overshoot blocks "+group+" chunks="+chunks);
        }
        var actors=new ArrayList<net.minecraft.world.entity.Entity>();var expected=new EnumMap<SpawnGroup,Integer>(SpawnGroup.class);for(var g:SpawnGroup.values())expected.put(g,0);
        try{
            var names=Set.of("EntityBunny","EntityFishy","CREEPSEntityFloob","CREEPSEntityHippo","CREEPSEntityEvilLight");
            int tested=0;
            for(var info:AnimalsB173.all())if(names.contains(info.registration().type().getSimpleName())&&Set.of("legacyconnector_b173.mocreatures","legacyconnector_b173.morecreepsandweirdos").contains(info.registration().type().getPackageName())){
                var actor=info.type().create(level);require(actor!=null,"actual census actor constructs");actors.add(actor);tested++;
                for(var g:SpawnGroup.values()){
                    boolean member=g.getCreatureClass().isInstance(actor.legacy());require(SpawnCapsB173.countsMembership(g,actor)==member,"actual original ancestry controls census for "+info.registration().type().getSimpleName()+"/"+g);if(member)expected.merge(g,1,Integer::sum);
                }
                if(info.registration().type().getSimpleName().equals("CREEPSEntityFloob"))require(actor.getType().getCategory()==MobCategory.CREATURE&&SpawnCapsB173.countsMembership(SpawnGroup.MONSTER,actor)&&!SpawnCapsB173.countsMembership(SpawnGroup.CREATURE,actor),"actual Floob CREATURE spawn entry counts toward original MONSTER census");
                actor.setPersistenceRequired();
            }
            require(tested>0&&SpawnCapsB173.census(actors).equals(expected),"actual mixed original census ignores modern persistence exclusions and native categories");
        }finally{actors.forEach(net.minecraft.world.entity.Entity::discard);EntityViewsB173.flush();}
        require(MobCategory.AMBIENT.getMaxInstancesPerChunk()==15&&MobCategory.WATER_AMBIENT.getMaxInstancesPerChunk()==20,"unmanaged category defaults remain native");
        LOG.info("B1.7.3 SPAWN CAPS TEST: actual original ancestry, defaults/configured caps, inclusive /256 boundary, deduplicated footprint and independent native budgets passed");
    }
}
