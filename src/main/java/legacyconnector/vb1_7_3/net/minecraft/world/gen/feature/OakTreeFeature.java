package legacyconnector.vb1_7_3.net.minecraft.world.gen.feature;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
/** Small Beta oak tree, used by the base biome's tree selector. */
public class OakTreeFeature extends Feature {
 public boolean generate(World world,Random random,int x,int y,int z){
  int height=random.nextInt(3)+4;
  if(y<1||y+height+1>128)return false;
  for(int yy=y;yy<=y+height+1;yy++){
   int radius=yy==y?0:yy>=y+height-1?2:1;
   for(int xx=x-radius;xx<=x+radius;xx++)for(int zz=z-radius;zz<=z+radius;zz++){
    int id=world.getBlockId(xx,yy,zz);if(id!=0&&id!=18)return false;
   }
  }
  int soil=world.getBlockId(x,y-1,z);if(soil!=2&&soil!=3)return false;
  world.setBlockWithoutNotifyingNeighbors(x,y-1,z,3);
  for(int yy=y+height-3;yy<=y+height;yy++){
   int offset=yy-(y+height),radius=1-offset/2;
   for(int xx=x-radius;xx<=x+radius;xx++)for(int zz=z-radius;zz<=z+radius;zz++){
    if(Math.abs(xx-x)==radius&&Math.abs(zz-z)==radius&&(random.nextInt(2)==0||offset==0))continue;
    int id=world.getBlockId(xx,yy,zz);
    if(id==0||id==18)world.setBlockWithoutNotifyingNeighbors(xx,yy,zz,18);
   }
  }
  for(int yy=0;yy<height;yy++){int id=world.getBlockId(x,y+yy,z);if(id==0||id==18)world.setBlockWithoutNotifyingNeighbors(x,y+yy,z,17);}
  return true;
 }
}
