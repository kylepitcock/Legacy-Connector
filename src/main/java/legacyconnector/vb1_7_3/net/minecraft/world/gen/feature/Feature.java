package legacyconnector.vb1_7_3.net.minecraft.world.gen.feature;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
public abstract class Feature {
 public abstract boolean generate(World world,Random random,int x,int y,int z);
 public void prepare(double x,double y,double z){}
}
