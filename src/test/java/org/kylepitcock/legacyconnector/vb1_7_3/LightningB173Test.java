package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.LightningEntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class LightningB173Test {
    static class Host extends LegacyB173Lightning {
        int bases,removals;
        Host(){super(null,null);}
        @Override public void mcc$baseTick(){bases++;}
        @Override public void baseMarkDead(){removals++;}
    }
    static class EarlyRemoval extends LightningEntity {
        int originals;
        EarlyRemoval(){super(null,1,2,3);}
        @Override public void tick(){originals++;super.tick();markDead();}
    }
    static class DetachedWorld extends World {DetachedWorld(){super();}}
    static class RoutingWorld extends DetachedWorld {
        final List<LightningEntity> weather=new ArrayList<>();boolean accept=true;
        @Override public boolean mcc$spawnLightning(LightningEntity entity){weather.add(entity);return accept;}
    }
    @Test void bothWorldSpawnApisRouteBaseAndOriginalSubclassThroughLightningOnce(){
        var world=new RoutingWorld();var vanilla=new LightningEntity(world,1,2,3);var original=new EarlyRemoval();
        assertTrue(world.spawnEntity(vanilla));assertTrue(world.spawnGlobalEntity(vanilla));
        assertTrue(world.spawnEntity(original));assertTrue(world.spawnGlobalEntity(original));
        assertEquals(List.of(vanilla,vanilla,original,original),world.weather);
        assertNull(original.mcc$modern,"Dispatch retains the original object for the shared host path");
        world.accept=false;assertFalse(world.spawnEntity(original));assertFalse(world.spawnGlobalEntity(original));
        assertEquals(6,world.weather.size(),"A declined lightning spawn must not fall through to a neutral host");
        assertFalse(world.spawnEntity(null));assertFalse(world.spawnGlobalEntity(null));
        assertNull(AnimalsB173.unsupportedBehaviour(EarlyRemoval.class),"Original tick/super.tick/removal closure remains admitted");
    }
    @Test void originalSuperclassEntersWeatherOnceAndKeepsImmediateRemoval()throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var host=(Host)((sun.misc.Unsafe)field.get(null)).allocateInstance(Host.class);
        var original=new EarlyRemoval();original.mcc$modern=host;original.tick();
        assertEquals(1,original.originals);assertEquals(1,host.bases);assertEquals(1,host.removals);assertTrue(original.dead);
    }
    @Test void detachedFlashRetainsOriginalReadAfterWrite(){
        var world=new DetachedWorld();assertEquals(0,world.mcc$get$lightningTicksLeft());world.mcc$set$lightningTicksLeft(2);assertEquals(2,world.mcc$get$lightningTicksLeft());
    }
    @Test void firstBaselineEventBeforeMetadataProducesOneNativePresentationAndKeepsLaterProgress(){
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        int presented=LegacyB173Lightning.advancePresentation(0,0,()->false,calls::incrementAndGet);
        assertEquals(0,calls.get());
        presented=LegacyB173Lightning.advancePresentation(presented,1,()->false,calls::incrementAndGet);
        assertEquals(1,presented);assertEquals(1,calls.get());
        presented=LegacyB173Lightning.advancePresentation(presented,0,()->false,calls::incrementAndGet);
        presented=LegacyB173Lightning.advancePresentation(presented,1,()->false,calls::incrementAndGet);
        assertEquals(1,presented);assertEquals(1,calls.get());
        presented=LegacyB173Lightning.advancePresentation(presented,3,()->false,calls::incrementAndGet);
        assertEquals(3,presented);assertEquals(3,calls.get());
    }
    @Test void metadataBeforeEventAndRepeatedEventsNeverDuplicateFirstBaselineOrReviveRemovedBolt(){
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        int presented=LegacyB173Lightning.advancePresentation(0,1,()->false,calls::incrementAndGet);
        presented=LegacyB173Lightning.advancePresentation(presented,1,()->false,calls::incrementAndGet);
        presented=LegacyB173Lightning.advancePresentation(presented,1,()->false,calls::incrementAndGet);
        assertEquals(1,presented);assertEquals(1,calls.get());
        assertEquals(0,LegacyB173Lightning.advancePresentation(0,1,()->true,()->fail("Removed native bolt must not be revived")));
    }
    @Test void triangleStripAlternatesWindingWithoutDrawingExtraTriangle(){
        var quads=new ArrayList<List<RendererContextB173.Vertex>>();
        try(var scope=RendererContextB173.open(path->null,(draw,texture)->{},new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){quads.add(vertices);}
        })){
            var t=Tessellator.INSTANCE;t.start(5);t.vertex(0,0,0);t.vertex(1,0,0);t.vertex(0,1,0);t.vertex(1,1,0);t.draw();
        }
        assertEquals(2,quads.size());
        for(var quad:quads){var a=quad.get(0);var b=quad.get(1);var c=quad.get(2);assertTrue((b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x())>0);assertEquals(c,quad.get(3));}
    }
    private static List<List<RendererContextB173.Vertex>> geometry(long seed){
        var result=new ArrayList<List<RendererContextB173.Vertex>>();var bolt=new LightningEntity(null,0,0,0);bolt.seed=seed;
        try(var scope=RendererContextB173.open(path->null,(draw,texture)->{},new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){assertFalse(style.texture2d());assertTrue(style.blend());result.add(vertices);}
        })){new LightningEntityRenderer().render(bolt,0,0,0,0,0);}
        return result;
    }
    @Test void originalSeedProducesAllFourShellsAndBothBranches(){
        var a=geometry(42);assertEquals(448,a.size());assertEquals(a,geometry(42));assertNotEquals(a,geometry(43));
        assertEquals(0,a.stream().flatMap(List::stream).mapToDouble(RendererContextB173.Vertex::y).min().orElseThrow());
        assertEquals(128,a.stream().flatMap(List::stream).mapToDouble(RendererContextB173.Vertex::y).max().orElseThrow());
    }
}
