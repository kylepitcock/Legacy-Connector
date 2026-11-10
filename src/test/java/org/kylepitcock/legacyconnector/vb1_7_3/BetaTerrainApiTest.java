package org.kylepitcock.legacyconnector.vb1_7_3;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource;
import legacyconnector.vb1_7_3.net.minecraft.world.gen.carver.CaveCarver;
import org.junit.jupiter.api.Test;
class BetaTerrainApiTest {
 @Test void generatedChunkRetainsArrayAndUsesUnsigned128HighLayout(){
  byte[] storage=new byte[32768];Chunk chunk=new Chunk(null,storage,-2,5);
  int index=3<<11|7<<7|127;storage[index]=(byte)240;
  assertSame(storage,chunk.mcc$blocks());assertEquals(240,chunk.getBlockId(3,127,7));
  assertEquals(0,chunk.getBlockId(3,128,7));
  assertTrue(chunk.setBlock(3,126,7,241,13));assertEquals(13,chunk.getBlockMeta(3,126,7));
  chunk.setBlockMeta(3,127,7,6);assertEquals(6,chunk.getBlockMeta(3,127,7));assertEquals(13,chunk.getBlockMeta(3,126,7));
  assertFalse(chunk.setBlock(3,128,7,1));assertThrows(IllegalArgumentException.class,()->new Chunk(null,new byte[65536],0,0));
 }
 @Test void detachedHeightMapDoesNotReadBackingWorld(){
  var opacity=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY;
  int previous=opacity[240];try{
   opacity[240]=255;byte[] storage=new byte[32768];Chunk chunk=new Chunk(null,storage,0,0);
   storage[1<<11|2<<7|127]=(byte)240;chunk.populateHeightMap();
   assertEquals(128,chunk.getHeight(1,2));assertEquals(0,chunk.getHeight(2,1));
   chunk.setBlock(1,127,2,0);assertEquals(0,chunk.getHeight(1,2));
  }finally{opacity[240]=previous;}
 }
 private static final class CustomBiome extends Biome {}
 private static final class CustomSource extends BiomeSource {
  final Biome custom=new CustomBiome();
  @Override public Biome getBiome(int x,int z){return custom;}
  @Override public Biome[] getBiomesInArea(Biome[] out,int x,int z,int width,int depth){
   if(out==null||out.length<width*depth)out=new Biome[width*depth];
   temperatureMap=new double[width*depth];downfallMap=new double[width*depth];Arrays.fill(out,custom);return out;
  }
 }
 @Test void originalNoArgBiomeOverridesRetainIdentityAndScratchSemantics(){
  var source=new CustomSource();assertSame(source.custom,source.getBiome(1,2));
  var values=source.getBiomesInArea(0,0,16,16);assertSame(source.custom,values[255]);
  assertEquals(0,source.temperatureMap[255]);assertNotSame(source.custom.spawnablePassive,Biome.PLAINS.spawnablePassive);
 }
 @Test void defaultClimateIsSeededAndNonconstant(){
  World world=new World(){@Override public long getSeed(){return 12345;}};
  var a=new BiomeSource(world);var b=new BiomeSource(world);
  a.getBiomesInArea(-128,64,16,16);b.getBiomesInArea(-128,64,16,16);
  assertArrayEquals(a.temperatureMap,b.temperatureMap);
  assertTrue(Arrays.stream(a.temperatureMap).distinct().count()>1);
 }
 private static final class Chamber extends CaveCarver {
  void dig(byte[] blocks){random.setSeed(17);carve(0,0,blocks,8,64,8);}
 }
 @Test void caveKernelMutatesDetachedStoneDeterministically(){
  byte[] a=new byte[32768],b=new byte[32768];Arrays.fill(a,(byte)1);Arrays.fill(b,(byte)1);
  new Chamber().dig(a);new Chamber().dig(b);assertArrayEquals(a,b);
  int removed=0;for(byte id:a)if(id==0)removed++;assertTrue(removed>0);
 }
}
