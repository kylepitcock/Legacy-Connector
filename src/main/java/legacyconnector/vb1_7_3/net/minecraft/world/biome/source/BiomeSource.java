package legacyconnector.vb1_7_3.net.minecraft.world.biome.source;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import legacyconnector.vb1_7_3.net.minecraft.util.math.ChunkPos;
import legacyconnector.vb1_7_3.net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
/** Stateful Beta climate provider. Callers serialize access with terrain/population. */
public class BiomeSource {
 public OctaveSimplexNoiseSampler temperatureSampler,downfallSampler,weirdnessSampler;
 public double[] temperatureMap,downfallMap,weirdnessMap;
 public Biome[] biomes;
 public BiomeSource(){}
 public BiomeSource(World world){long seed=world.getSeed();temperatureSampler=new OctaveSimplexNoiseSampler(new Random(seed*9871L),4);downfallSampler=new OctaveSimplexNoiseSampler(new Random(seed*39811L),4);weirdnessSampler=new OctaveSimplexNoiseSampler(new Random(seed*543321L),2);}
 public Biome getBiome(ChunkPos pos){return getBiome(pos.x,pos.z);}
 public Biome getBiome(int x,int z){return getBiomesInArea(x,z,1,1)[0];}
 public double getTemperature(int x,int z){return create(null,x,z,1,1)[0];}
 public double[] create(double[] out,int x,int z,int width,int depth){
  if(temperatureSampler==null)throw new IllegalStateException("Uninitialized biome climate provider");
  out=temperatureSampler.sample(out,x,z,width,depth,.025,.025,.25);
  weirdnessMap=weirdnessSampler.sample(weirdnessMap,x,z,width,depth,.25,.25,.5882352941176471);
  for(int i=0;i<width*depth;i++){double t=(out[i]*.15+.7)*.99+(weirdnessMap[i]*1.1+.5)*.01;out[i]=clamp(1-(1-t)*(1-t));}
  return out;
 }
 public Biome[] getBiomesInArea(int x,int z,int width,int depth){biomes=getBiomesInArea(biomes,x,z,width,depth);return biomes;}
 public Biome[] getBiomesInArea(Biome[] out,int x,int z,int width,int depth){
  if(temperatureSampler==null)throw new IllegalStateException("Uninitialized biome climate provider");
  if(out==null||out.length<width*depth)out=new Biome[width*depth];
  temperatureMap=temperatureSampler.sample(temperatureMap,x,z,width,depth,.025,.025,.25);
  downfallMap=downfallSampler.sample(downfallMap,x,z,width,depth,.05,.05,1.0/3);
  weirdnessMap=weirdnessSampler.sample(weirdnessMap,x,z,width,depth,.25,.25,.5882352941176471);
  for(int i=0;i<width*depth;i++){
   double weird=weirdnessMap[i]*1.1+.5;
   double t=(temperatureMap[i]*.15+.7)*.99+weird*.01;
   temperatureMap[i]=clamp(1-(1-t)*(1-t));downfallMap[i]=clamp((downfallMap[i]*.15+.5)*.998+weird*.002);
   out[i]=Biome.getBiome(temperatureMap[i],downfallMap[i]);
  }
  return out;
 }
 private static double clamp(double value){return Math.max(0,Math.min(1,value));}
}
