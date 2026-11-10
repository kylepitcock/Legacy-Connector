package org.kylepitcock.legacyconnector.vb1_7_3.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.chunk.ChunkSource;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs a registered Beta dimension's original terrain and population at the native generation stages. */
public final class ChunkGeneratorB173 extends ChunkGenerator {
 private static final Logger LOG=LoggerFactory.getLogger("LegacyConnector/B173Dimensions");
 public static final MapCodec<ChunkGeneratorB173> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.INT.fieldOf("dimension").forGetter(g->g.dimension),
  BiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.biomeSource)).apply(i,ChunkGeneratorB173::new));
 private final int dimension;
 private volatile ChunkSource legacy;
 private volatile ChunkSource detachedSource;
 private final ThreadLocal<Boolean> readingDetached=ThreadLocal.withInitial(()->false);
 private boolean creationFailed;
 private final AtomicLong generated=new AtomicLong(),populated=new AtomicLong(),failures=new AtomicLong();
 private record Terrain(byte[] blocks,byte[] metadata){}
 /** Detached snapshots only: height queries never request live or unfinished native chunks. */
 private final Map<Long,Terrain> cache=new LinkedHashMap<>(32,.75F,true){
  @Override protected boolean removeEldestEntry(Map.Entry<Long,Terrain> entry){return size()>64;}
 };
 public ChunkGeneratorB173(int dimension,BiomeSource biomes){super(biomes);this.dimension=dimension;}
 @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
 private synchronized ChunkSource legacy(){
  if(legacy!=null||creationFailed)return legacy;
  var provider=DimensionsB173.provider(dimension);
  if(provider==null)return null;
  try{
   LegacyModLocks.generation(LegacyModLocks.of(provider),()->{
    synchronized(provider){
     var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
     try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(provider.world,null)){
      legacy=provider.createChunkGenerator();
     }finally{minecraft.world=previousWorld;}
    }
   });
   if(legacy==null){creationFailed=true;failure("create",0,0,new IllegalStateException("Provider returned no chunk source"));}
  }catch(RuntimeException|LinkageError error){creationFailed=true;failure("create",0,0,error);}
  return legacy;
 }
 /** Preserve the shared biome manager's caller-owned scratch arrays across detached neighbour generation. */
 private static final class BiomeScratch {
  private final legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource manager;
  private final double[] temperature,downfall,weirdness,temperatureCopy,downfallCopy,weirdnessCopy;
  private final legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[] biomes,biomesCopy;
  BiomeScratch(legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource manager){
   this.manager=manager;
   temperature=manager.temperatureMap;downfall=manager.downfallMap;weirdness=manager.weirdnessMap;biomes=manager.biomes;
   temperatureCopy=temperature==null?null:temperature.clone();downfallCopy=downfall==null?null:downfall.clone();weirdnessCopy=weirdness==null?null:weirdness.clone();biomesCopy=biomes==null?null:biomes.clone();
  }
  void restore(){
   if(temperature!=null)System.arraycopy(temperatureCopy,0,temperature,0,temperature.length);
   if(downfall!=null)System.arraycopy(downfallCopy,0,downfall,0,downfall.length);
   if(weirdness!=null)System.arraycopy(weirdnessCopy,0,weirdness,0,weirdness.length);
   if(biomes!=null)System.arraycopy(biomesCopy,0,biomes,0,biomes.length);
   manager.temperatureMap=temperature;manager.downfallMap=downfall;manager.weirdnessMap=weirdness;manager.biomes=biomes;
  }
 }
 /** Missing population neighbours come from a distinct original source, never the active populator's Random. */
 private BlockState terrainState(int x,int y,int z){
  if(y<0||y>=128)return Blocks.AIR.defaultBlockState();
  if(readingDetached.get()){failure("recursive detached terrain read",x>>4,z>>4,null);return null;}
  readingDetached.set(true);
  try{
   Terrain terrain=terrain(x>>4,z>>4,true);if(terrain==null)return null;
   int i=(x&15)<<11|(z&15)<<7|y;
   return BlocksB173.modernState(terrain.blocks[i]&255,terrain.metadata[i]&15);
  }finally{readingDetached.remove();}
 }
 private Terrain terrain(int x,int z){return terrain(x,z,false);}
 private Terrain terrain(int x,int z,boolean independent){
  long key=((long)x<<32)|(z&0xffffffffL);
  synchronized(cache){var hit=cache.get(key);if(hit!=null)return hit;}
  try{
   ChunkSource source=legacy();var provider=DimensionsB173.provider(dimension);
   if(source==null||provider==null){failure("terrain unavailable",x,z,null);return null;}
   Terrain[] result=new Terrain[1];
   LegacyModLocks.generation(LegacyModLocks.of(provider),()->{
    synchronized(source){synchronized(provider.biomeSource==null?source:provider.biomeSource){
     synchronized(cache){var hit=cache.get(key);if(hit!=null){result[0]=hit;return;}}
     var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
     BiomeScratch scratch=independent&&provider.biomeSource!=null?new BiomeScratch(provider.biomeSource):null;
     try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(provider.world,null)){
      ChunkSource terrainSource=source;
      if(independent){
       if(detachedSource==null)detachedSource=provider.createChunkGenerator();
       terrainSource=detachedSource;
       if(terrainSource==null)throw new IllegalStateException("Provider returned no detached terrain source");
       if(terrainSource==source)throw new IllegalStateException("Provider reused the active population source for detached terrain");
      }
      var chunk=terrainSource.getChunk(x,z);
      if(chunk==null||!chunk.mcc$hasArrays()||chunk.mcc$blocks().length!=32768)
       throw new IllegalStateException("Original provider did not return a detached Beta chunk");
      byte[] metadata=new byte[32768];for(int i=0;i<metadata.length;i++)metadata[i]=(byte)chunk.mcc$metadata(i);
      result[0]=new Terrain(chunk.mcc$blocks().clone(),metadata);
      synchronized(cache){cache.put(key,result[0]);}
      long count=generated.incrementAndGet();if(count==1)LOG.info("Beta dimension {} original terrain completed at {},{} (32768 blocks)",dimension,x,z);
     }finally{try{if(scratch!=null)scratch.restore();}finally{minecraft.world=previousWorld;}}
    }}
   });
   return result[0];
  }catch(RuntimeException|LinkageError error){failure("terrain",x,z,error);return null;}
 }
 @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
  return CompletableFuture.supplyAsync(()->{
   int cx=chunk.getPos().x,cz=chunk.getPos().z;
   try{
    Terrain terrain=terrain(cx,cz);if(terrain==null)return chunk;
    var pos=new BlockPos.MutableBlockPos();
    for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<128;y++){
     int i=x<<11|z<<7|y;
     BlockState state=BlocksB173.modernState(terrain.blocks[i]&255,terrain.metadata[i]&15);
     if(!state.isAir())chunk.setBlockState(pos.set((cx<<4)+x,y,(cz<<4)+z),state,false);
    }
    Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG));
   }catch(RuntimeException|LinkageError error){failure("native copy",cx,cz,error);}
   return chunk;
  },Util.backgroundExecutor());
 }
 @Override public void applyBiomeDecoration(WorldGenLevel level,ChunkAccess chunk,StructureManager structures){
  int x=chunk.getPos().x,z=chunk.getPos().z;
  try{
   var source=legacy();var provider=DimensionsB173.provider(dimension);
   if(source==null||provider==null){failure("population unavailable",x,z,null);return;}
   LegacyModLocks.generation(LegacyModLocks.of(provider),()->{
    synchronized(source){synchronized(provider.biomeSource==null?source:provider.biomeSource){
     var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
     try(var generation=World.mcc$generation(level,this::terrainState);
         var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(provider.world,null)){
      try{
       try{source.decorate(source,x,z);}
       finally{try{BlockEntityViewsB173.flush();}finally{EntityViewsB173.flush();}}
       long count=populated.incrementAndGet();if(count==1)LOG.info("Beta dimension {} original population completed at {},{}",dimension,x,z);
      }catch(RuntimeException|Error error){org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenEntities.abort(level.getLevel());throw error;}
     }finally{minecraft.world=previousWorld;}
    }}
   });
   // Native biome modifiers may add features to this owned dimension after its original population.
   super.applyBiomeDecoration(level,chunk,structures);
  }catch(RuntimeException|LinkageError error){failure("population",x,z,error);}
 }
 private void failure(String phase,int x,int z,Throwable error){
  long count=failures.incrementAndGet();if(count<=8||(count&(count-1))==0){
   if(error==null)LOG.warn("Beta dimension {} {} at {},{} ({} failures)",dimension,phase,x,z,count);
   else LOG.error("Beta dimension {} {} failed at {},{} ({} failures)",dimension,phase,x,z,count,error);
  }
 }
 public Map<String,Long> mcc$diagnostics(){return Map.of("generated",generated.get(),"populated",populated.get(),"failures",failures.get());}
 @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step){}
 @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
 @Override public void spawnOriginalMobs(WorldGenRegion region){}
 @Override public int getGenDepth(){return 128;}
 @Override public int getSeaLevel(){return 64;}
 @Override public int getMinY(){return 0;}
 @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor level,RandomState random){
  try{
   Terrain terrain=terrain(x>>4,z>>4);if(terrain==null)return 0;
   int column=(x&15)<<11|(z&15)<<7;
   for(int y=Math.min(127,level.getMaxBuildHeight()-1);y>=Math.max(0,level.getMinBuildHeight());y--)
    if(type.isOpaque().test(BlocksB173.modernState(terrain.blocks[column|y]&255,terrain.metadata[column|y]&15)))return y+1;
  }catch(RuntimeException|LinkageError error){failure("height query",x>>4,z>>4,error);}
  return 0;
 }
 @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor level,RandomState random){
  int min=level.getMinBuildHeight(),height=level.getHeight();BlockState[] states=new BlockState[height];
  java.util.Arrays.fill(states,Blocks.AIR.defaultBlockState());
  try{
   Terrain terrain=terrain(x>>4,z>>4);if(terrain!=null){int column=(x&15)<<11|(z&15)<<7;
    for(int y=Math.max(0,min);y<Math.min(128,min+height);y++)states[y-min]=BlocksB173.modernState(terrain.blocks[column|y]&255,terrain.metadata[column|y]&15);
   }
  }catch(RuntimeException|LinkageError error){failure("column query",x>>4,z>>4,error);}
  return new NoiseColumn(min,states);
 }
 @Override public void addDebugScreenInfo(List<String> info,RandomState random,BlockPos pos){info.add("Beta dimension "+dimension+" "+mcc$diagnostics());}
}
