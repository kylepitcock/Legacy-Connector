package legacyconnector.vb1_7_3.net.minecraft.world.gen.carver;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.chunk.ChunkSource;
/** Chunk-spanning Beta carver base. */
public class Carver {
 protected int range=8;
 protected Random random=new Random();
 protected World world;
 public void carve(ChunkSource source,World world,int chunkX,int chunkZ,byte[] blocks){
  if(blocks==null||blocks.length!=32768)throw new IllegalArgumentException("Beta carvers require 32768 block bytes");
  this.world=world;random.setSeed(world.getSeed());
  long xSalt=random.nextLong()/2L*2L+1L,zSalt=random.nextLong()/2L*2L+1L;
  for(int x=chunkX-range;x<=chunkX+range;x++)for(int z=chunkZ-range;z<=chunkZ+range;z++){
   random.setSeed(((long)x*xSalt+(long)z*zSalt)^world.getSeed());
   carve(world,x,z,chunkX,chunkZ,blocks);
  }
 }
 protected void carve(World world,int x,int z,int chunkX,int chunkZ,byte[] blocks){}
}
