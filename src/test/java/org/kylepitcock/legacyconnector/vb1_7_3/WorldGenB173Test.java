package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class WorldGenB173Test {
    public static class Defaults extends BaseMod {@Override public String Version(){return "fixture";}}
    public static class Surface extends Defaults {
        int calls,x,z;World world;Random random;
        @Override public void GenerateSurface(World world,Random random,int x,int z){calls++;this.world=world;this.random=random;this.x=x;this.z=z;}
    }
    public static class Inherited extends Surface {}
    public static class Nether extends Defaults {
        int calls,x,z;
        @Override public void GenerateNether(World world,Random random,int x,int z){calls++;this.x=x;this.z=z;}
    }
    @Test void defaultsDoNotRegisterAndInheritedOverridesDo(){
        assertFalse(WorldGenB173.overrides(Defaults.class,false));assertFalse(WorldGenB173.overrides(Defaults.class,true));
        assertTrue(WorldGenB173.overrides(Surface.class,false));assertFalse(WorldGenB173.overrides(Surface.class,true));
        assertTrue(WorldGenB173.overrides(Inherited.class,false));assertTrue(WorldGenB173.overrides(Nether.class,true));
    }
    @Test void dispatchUsesOnlyMatchingDimensionAndBlockOrigins(){
        var surface=new Surface();var nether=new Nether();var random=new Random(5);
        WorldGenB173.dispatch(surface,0,null,random,-3,7);
        assertEquals(1,surface.calls);assertEquals(-48,surface.x);assertEquals(112,surface.z);assertSame(random,surface.random);
        WorldGenB173.dispatch(surface,-1,null,random,8,9);WorldGenB173.dispatch(surface,1,null,random,8,9);
        assertEquals(1,surface.calls);
        WorldGenB173.dispatch(nether,0,null,random,8,9);WorldGenB173.dispatch(nether,1,null,random,8,9);assertEquals(0,nether.calls);
        WorldGenB173.dispatch(nether,-1,null,random,8,-9);assertEquals(1,nether.calls);assertEquals(128,nether.x);assertEquals(-144,nether.z);
    }
    @Test void exactBetaSignedDivisionSeedsIncludeNegativeSaltsAndChunks(){
        // Audited ModLoader.PopulateChunk uses nextLong()/2*2+1, unlike FML's shifted salts.
        assertEquals(-4962768465676381896L,WorldGenB173.seededRandom(0,0,0).nextLong());
        assertEquals(2014880941284176763L,WorldGenB173.seededRandom(1,-2,3).nextLong());
        assertEquals(-330439659629180781L,WorldGenB173.seededRandom(-1,13,-7).nextLong());
        assertEquals(-8938490438770598892L,WorldGenB173.seededRandom(42,-9,-11).nextLong());
        assertEquals(-2788771387660887069L,WorldGenB173.seededRandom(Long.MIN_VALUE,-1,1).nextLong());
    }
    @Test void featureAndFallbackCallsShareOneStreamWithoutCrossChunkOrRegionReuse(){
        var region=new Object();var first=WorldGenB173.chunkRandom(region,1,-2,3);
        assertEquals(2014880941284176763L,first.nextLong());
        var second=WorldGenB173.chunkRandom(region,1,-2,3);assertSame(first,second);
        assertEquals(6833502985283993525L,second.nextLong(),"later mods consume the stream after earlier mods");
        assertNotSame(first,WorldGenB173.chunkRandom(region,1,-2,4));
        assertEquals(2014880941284176763L,WorldGenB173.chunkRandom(new Object(),1,-2,3).nextLong(),"another dimension/region starts its own population stream");
    }
    @Test void originalCallbackExceptionsAreNotSwallowed(){
        var expected=new IllegalStateException("original failure");
        var mod=new Defaults(){@Override public void GenerateSurface(World world,Random random,int x,int z){throw expected;}};
        assertSame(expected,assertThrows(IllegalStateException.class,()->WorldGenB173.dispatch(mod,0,null,new Random(),0,0)));
    }
}
