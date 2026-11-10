package legacyconnector.vb1_7_3.net.minecraft.util.math.noise;
import java.util.Random;
public class OctaveSimplexNoiseSampler extends NoiseSampler {
 private final SimplexNoiseSampler[] samplers;
 public OctaveSimplexNoiseSampler(Random random,int octaves){samplers=new SimplexNoiseSampler[octaves];for(int i=0;i<octaves;i++)samplers[i]=new SimplexNoiseSampler(random);}
 public double[] sample(double[] out,double x,double z,int width,int depth,double xScale,double zScale,double persistence){return sample(out,x,z,width,depth,xScale,zScale,persistence,.5);}
 public double[] sample(double[] out,double x,double z,int width,int depth,double xScale,double zScale,double persistence,double lacunarity){
  if(out==null||out.length<width*depth)out=new double[width*depth];else java.util.Arrays.fill(out,0);
  xScale/=1.5;zScale/=1.5;double frequency=1,amplitude=1;
  for(var sampler:samplers){sampler.create(out,x,z,width,depth,xScale*frequency,zScale*frequency,.55/amplitude);frequency*=persistence;amplitude*=lacunarity;}
  return out;
 }
}
