package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.BoatEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.TntEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

class BoatApisB173Test {
    @Test void rendersAllFiveOriginalHullPieces(){
        var model=new BoatEntityModel();var draws=new java.util.ArrayList<ModelGeometryB173.Draw>();
        ModelGeometryB173.capture(draws::add,()->model.render(0,0,-.1F,0,0,.0625F));
        assertEquals(5,draws.size());assertEquals(30,draws.stream().mapToInt(draw->draw.cuboids().size()*6).sum());
        var bottom=draws.getFirst().cuboids().getFirst();
        assertEquals(8,bottom.v());assertEquals(24,bottom.width());assertEquals(16,bottom.height());assertEquals(4,bottom.depth());
    }
    @Test void betaEntityCollisionDefaultsAndFuseNbtRemainDistinct(){
        var plain=new Entity(null){};assertNull(plain.getBoundingBox());assertNull(plain.getCollisionAgainstShape(plain));
        assertTrue(plain.mcc$steppingEffects());assertFalse(plain.interact(null));plain.animateHurt();
        var tnt=new TntEntity(null);assertFalse(tnt.mcc$steppingEffects());assertTrue(tnt.isCollidable());
        tnt.fuse=150;var tag=new NbtCompound();tnt.writeNbt(tag);var copy=new TntEntity(null);copy.readNbt(tag);
        assertEquals((byte)150,copy.fuse,"original Beta intentionally stores fuse as a signed byte");
    }
    @Tag("actual-mod") @Test void hullGeometryAndTransformsMatchOriginalBetaJar()throws Exception{
        var jar=java.nio.file.Path.of(System.getProperty("legacyconnector.b173.vanillaJar","build/devtools/b1.7.3/client.jar"));
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(jar),"original Beta fixture unavailable");
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},null)){
            Object original=loader.loadClass("ez").getConstructor().newInstance();
            Object[] parts=(Object[])original.getClass().getField("a").get(original);var shim=new BoatEntityModel();
            var facesField=loader.loadClass("ps").getDeclaredField("k");facesField.setAccessible(true);
            String[] source={"a","b","c","d","e","f"},target={"pivotX","pivotY","pivotZ","pitch","yaw","roll"};
            for(int p=0;p<5;p++){
                for(int field=0;field<6;field++)assertEquals(parts[p].getClass().getField(source[field]).getFloat(parts[p]),shim.parts[p].getClass().getField(target[field]).getFloat(shim.parts[p]),1E-6F);
                Object[] faces=(Object[])facesField.get(parts[p]);var expected=ModelGeometryB173.vertices(shim.parts[p].mcc$cuboids().getFirst());
                for(int f=0;f<6;f++){
                    Object[] vertices=(Object[])faces[f].getClass().getField("a").get(faces[f]);
                    for(int v=0;v<4;v++){
                        Object old=vertices[v],pos=old.getClass().getField("a").get(old);var actual=expected.get(f).get(v);
                        assertEquals(((Number)pos.getClass().getField("a").get(pos)).floatValue(),actual.x(),1E-6F);
                        assertEquals(((Number)pos.getClass().getField("b").get(pos)).floatValue(),actual.y(),1E-6F);
                        assertEquals(((Number)pos.getClass().getField("c").get(pos)).floatValue(),actual.z(),1E-6F);
                        assertEquals(old.getClass().getField("b").getFloat(old),actual.u(),1E-6F);assertEquals(old.getClass().getField("c").getFloat(old),actual.v(),1E-6F);
                    }
                }
            }
        }
    }
}
