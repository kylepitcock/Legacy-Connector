package org.kylepitcock.legacyconnector.vb1_7_3;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Checks native cap decisions after real legacy initialization, without spawning or changing world state. */
public final class SpawnCapsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173SpawnCapsTest");
    private SpawnCapsSelfTestB173() {}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        if(!SpawnCapsB173.hasOverrides())return;
        try{run();}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 SPAWN CAPS TEST failed",error);}
    });}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    private static void run() throws ReflectiveOperationException {
        var constructor=NaturalSpawner.SpawnState.class.getDeclaredConstructor(int.class,Object2IntOpenHashMap.class,PotentialCalculator.class,LocalMobCapCalculator.class);
        constructor.setAccessible(true);
        var globalDecision=NaturalSpawner.SpawnState.class.getDeclaredMethod("canSpawnForCategory",MobCategory.class,ChunkPos.class);globalDecision.setAccessible(true);
        // Isolate the real global decision from player proximity and local counts.
        var permissiveLocal=new LocalMobCapCalculator(null){@Override public boolean canSpawn(MobCategory category,ChunkPos pos){return true;}};
        var localType=Class.forName("net.minecraft.world.level.LocalMobCapCalculator$MobCounts");
        var localConstructor=localType.getDeclaredConstructor();localConstructor.setAccessible(true);
        var localAdd=localType.getDeclaredMethod("add",MobCategory.class);localAdd.setAccessible(true);
        var localDecision=localType.getDeclaredMethod("canSpawn",MobCategory.class);localDecision.setAccessible(true);
        var details=new java.util.ArrayList<String>();
        for(var group:SpawnGroup.values())if(SpawnCapsB173.isOverridden(group)) {
            var category=group.mcc$modern();int cap=group.getCapacity();
            require(category.getMaxInstancesPerChunk()==cap,"native getter lost explicit Beta cap for "+group);
            require(cap>0&&cap<=10000,"runtime cap proof requires a bounded positive configured cap: "+cap);
            var counts=new Object2IntOpenHashMap<MobCategory>();
            var state=constructor.newInstance(289,counts,new PotentialCalculator(),permissiveLocal);
            counts.put(category,cap-1);require((boolean)globalDecision.invoke(state,category,new ChunkPos(0,0)),"native global cap refused below "+group);
            counts.put(category,cap);require(!(boolean)globalDecision.invoke(state,category,new ChunkPos(0,0)),"native global cap admitted at "+group);
            var local=localConstructor.newInstance();
            for(int i=0;i<cap-1;i++)localAdd.invoke(local,category);
            require((boolean)localDecision.invoke(local,category),"native local cap refused below "+group);
            localAdd.invoke(local,category);require(!(boolean)localDecision.invoke(local,category),"native local cap admitted at "+group);
            details.add(group+"="+cap);
        }
        require(MobCategory.AMBIENT.getMaxInstancesPerChunk()==15&&MobCategory.WATER_AMBIENT.getMaxInstancesPerChunk()==20,"unmapped native category defaults changed");
        LOG.info("B1.7.3 SPAWN CAPS TEST: explicit legacy writes, native getter, global 289-chunk boundary and native per-player count boundary passed: {}; untouched ambient defaults preserved",details);
    }
}
