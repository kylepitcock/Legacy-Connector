package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyGenerators;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;

/** ModLoader chunk callbacks as per-mod native placed features, in registration order. */
public final class WorldGenB173 {
    private record Entry(BaseMod mod,String modId,ReentrantLock lock) {}
    private static final List<Entry> ENTRIES=new ArrayList<>();
    // PopulateChunk passed one Random through the mod list. Native features share its
    // stream for their generation region/chunk; values never retain the weak region key.
    private static final Map<Object,Map<Long,Random>> RANDOMS=new WeakHashMap<>();
    private static final boolean DIAGNOSTICS=Boolean.getBoolean("legacyconnector.worldGenSelfTest");
    private static final Map<String,java.util.concurrent.atomic.AtomicLong> CALLBACKS=new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean hooked;
    private WorldGenB173(){}

    /** Opt-in native-feature proof: successful original callbacks by namespace, dimension and chunk. */
    public static Map<String,Long> callbackCounts(){
        var snapshot=new java.util.TreeMap<String,Long>();
        CALLBACKS.forEach((key,value)->snapshot.put(key,value.get()));return Map.copyOf(snapshot);
    }

    /** Called only after the mod's ModsLoaded callback completed successfully. */
    public static synchronized void register(LegacyModRecord record){
        if(!(record.instance instanceof BaseMod mod)||(!overrides(mod.getClass(),false)&&!overrides(mod.getClass(),true)))return;
        if(ENTRIES.stream().anyMatch(entry->entry.mod==mod))return;
        ENTRIES.add(new Entry(mod,record.modernId,LegacyModLocks.of(mod)));
        if(!hooked){
            hooked=true;
            LegacyGenerators.addSource(WorldGenB173::features);
            LegacyWorldGenFeature.addDecorator(WorldGenB173::decorate);
        }
    }

    static boolean overrides(Class<? extends BaseMod> type,boolean nether){
        try{return type.getMethod(nether?"GenerateNether":"GenerateSurface",World.class,Random.class,int.class,int.class).getDeclaringClass()!=BaseMod.class;}
        catch(NoSuchMethodException error){throw new IllegalStateException("Missing Beta BaseMod generation API",error);}
    }

    private static synchronized List<Entry> entries(){return List.copyOf(ENTRIES);}
    private static List<LegacyGenerators.Generator> features(){
        var result=new ArrayList<LegacyGenerators.Generator>();
        for(var entry:entries())result.add(new LegacyGenerators.Generator(entry.mod,entry.modId,
                (level,x,z)->generate(level,x,z,entry)));
        return result;
    }

    /** Only registrations too late for their own placed feature use the existing catch-all. */
    private static void decorate(WorldGenLevel level,int chunkX,int chunkZ,long seed){
        for(var entry:entries())if(!LegacyGenerators.covered(level,entry.mod))generate(level,chunkX,chunkZ,entry);
    }

    static Random seededRandom(long worldSeed,int chunkX,int chunkZ){
        var random=new Random(worldSeed);
        long xSalt=random.nextLong()/2L*2L+1L,zSalt=random.nextLong()/2L*2L+1L;
        random.setSeed(((long)chunkX*xSalt+(long)chunkZ*zSalt)^worldSeed);
        return random;
    }

    static synchronized Random chunkRandom(Object region,long worldSeed,int chunkX,int chunkZ){
        long chunk=((long)chunkX<<32)|(chunkZ&0xffffffffL);
        return RANDOMS.computeIfAbsent(region,key->new HashMap<>()).computeIfAbsent(chunk,key->seededRandom(worldSeed,chunkX,chunkZ));
    }

    static void dispatch(BaseMod mod,int dimension,World world,Random random,int chunkX,int chunkZ){
        if(dimension==0)mod.GenerateSurface(world,random,chunkX<<4,chunkZ<<4);
        else if(dimension==-1)mod.GenerateNether(world,random,chunkX<<4,chunkZ<<4);
    }

    private static void generate(WorldGenLevel region,int chunkX,int chunkZ,Entry entry){
        var dimension=region.getLevel().dimension();
        int id=dimension==Level.OVERWORLD?0:dimension==Level.NETHER?-1:1;
        if(id==1||!overrides(entry.mod.getClass(),id==-1))return;
        var world=World.mcc$wrap(region.getLevel());
        var random=chunkRandom(region,region.getSeed(),chunkX,chunkZ);
        LegacyModLocks.generation(entry.lock,()->{
            try(var generation=World.mcc$generation(region);
                var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,null)){
                try{
                    dispatch(entry.mod,id,world,random,chunkX,chunkZ);
                    if(DIAGNOSTICS){
                        String key=entry.modId+"@"+id+":"+chunkX+","+chunkZ;
                        long count=CALLBACKS.computeIfAbsent(key,ignored->new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
                        if(count==1)org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173WorldGenTest").info("B1.7.3 WORLDGEN TEST: native feature callback {} completed",key);
                    }
                }
                finally{try{BlockEntityViewsB173.flush();}finally{EntityViewsB173.flush();}}
            }
        });
    }
}
