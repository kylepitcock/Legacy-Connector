package org.kylepitcock.legacyconnector.vb1_7_3.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;
import org.slf4j.LoggerFactory;

/** Samples the original provider's biome manager, preserving registered custom biome identities. */
public final class BiomeSourceB173 extends BiomeSource {
 public static final MapCodec<BiomeSourceB173> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.INT.fieldOf("dimension").forGetter(s->s.dimension),
  Biome.LIST_CODEC.fieldOf("biomes").forGetter(s->s.biomes)).apply(i,BiomeSourceB173::new));
 private final int dimension;
 private final HolderSet<Biome> biomes;
 private final Map<ResourceLocation,Holder<Biome>> byKey=new HashMap<>();
 private final AtomicLong failures=new AtomicLong();
 public BiomeSourceB173(int dimension,HolderSet<Biome> biomes){
  this.dimension=dimension;this.biomes=biomes;
  for(var holder:biomes)holder.unwrapKey().ifPresent(key->byKey.put(key.location(),holder));
 }
 @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
 @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return biomes.stream();}
 /** Beta biomes are per column: one sample serves every height of a column (a chunk asks each of its 24 sections), so the serialized original manager runs once per column. */
 private record Column(long key,Object provider,Holder<Biome> biome){}
 private static final int COLUMNS=4096;
 private final Column[] columns=new Column[COLUMNS];
 private Holder<Biome> fallback;
 @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
  // Bootstrap can ask before server levels exist; the supplied holders are the registered dimension biomes.
  Holder<Biome> fallback=this.fallback;if(fallback==null)this.fallback=fallback=biomes.stream().findFirst().orElseThrow();
  try{
   var provider=DimensionsB173.provider(dimension);
   if(provider==null||provider.biomeSource==null)return fallback;
   long key=((long)x<<32)|(z&0xFFFFFFFFL);int slot=(int)((x*0x9E3779B1L+z*0x85EBCA77L)>>>7)&(COLUMNS-1);
   var cached=columns[slot];
   if(cached!=null&&cached.key()==key&&cached.provider()==provider)return cached.biome();
   var sampled=sample(provider,x,z,fallback);columns[slot]=new Column(key,provider,sampled);return sampled;
  }catch(RuntimeException|LinkageError error){failure(()->error);return fallback;}
 }
 private Holder<Biome> sample(legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension provider,int x,int z,Holder<Biome> fallback){
  {
   var result=new java.util.concurrent.atomic.AtomicReference<Holder<Biome>>(fallback);
   LegacyModLocks.generation(LegacyModLocks.of(provider),()->{
    var manager=provider.biomeSource;
    synchronized(manager){
     var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
     var actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
     try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(provider.world,actor!=null&&actor.world==provider.world?actor:null)){
     var legacy=manager.getBiome((x<<2)+2,(z<<2)+2);
     ResourceLocation key=legacy==null?null:DimensionsB173.biomeKey(dimension,legacy);
     var holder=key==null?null:byKey.get(key);
     if(holder!=null)result.set(holder);
     else failure(()->new IllegalStateException("Unregistered legacy biome "+(legacy==null?"null":legacy.getClass().getName())));
     }finally{minecraft.world=previousWorld;}
    }
   });
   return result.get();
  }
 }
 /** The error is built only when it is logged: a failing column used to construct (and fill the stack trace of) one exception per sample. */
 private void failure(java.util.function.Supplier<Throwable> error){long count=failures.incrementAndGet();if(count<=4||(count&(count-1))==0)LoggerFactory.getLogger("LegacyConnector/B173Dimensions").error("Beta dimension {} biome sampling failed ({} failures)",dimension,count,error.get());}
 public long mcc$failures(){return failures.get();}
}
