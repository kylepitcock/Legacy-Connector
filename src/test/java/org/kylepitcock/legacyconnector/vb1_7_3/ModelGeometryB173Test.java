package org.kylepitcock.legacyconnector.vb1_7_3;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import org.joml.Vector3f;
class ModelGeometryB173Test {
    @Test void betaUvInsetTopOrientationReplacementAndMirror(){
        var part=new ModelPart(4,8);part.addCuboid(0,0,0,2,3,4);part.mirror=true;part.addCuboid(1,2,3,2,3,4,.5F);
        assertEquals(1,part.mcc$cuboids().size());var box=part.mcc$cuboids().getFirst();
        assertEquals(new ModelGeometryB173.Uv(10,8,12,12),box.faces().get(3));
        var face=ModelGeometryB173.vertices(box).get(0);assertEquals(4,face.size());
        assertEquals((14-.1F)/64,face.get(0).u(),.00001F);assertEquals((15-.1F)/32,face.get(0).v(),.00001F);
        assertEquals(.5F,face.get(0).x());
    }
    @Test void originalDrawCaptureSharesTransformsAndHonorsVisibility(){
        var part=new ModelPart(0,0);part.addCuboid(0,0,0,1,1,1);part.setPivot(16,0,0);part.roll=(float)Math.PI/2;
        var draws=new java.util.ArrayList<ModelGeometryB173.Draw>();
        ModelGeometryB173.capture(draws::add,()->{part.render(1F/16);part.visible=false;part.render(1F/16);});
        assertEquals(1,draws.size());var v=draws.getFirst().transform().transformPosition(new Vector3f(1,0,0));
        assertEquals(1,v.x,.00001);assertEquals(1,v.y,.00001);
        float quarter=(float)Math.PI/2;
        var normal=ModelGeometryB173.transform(0,0,0,quarter,quarter,quarter,1,false).transformPosition(new Vector3f(1,0,0));
        var forced=ModelGeometryB173.transform(0,0,0,quarter,quarter,quarter,1,true).transformPosition(new Vector3f(1,0,0));
        assertEquals(0,normal.x,.00001);assertEquals(0,normal.y,.00001);assertEquals(-1,normal.z,.00001);
        assertEquals(1,forced.x,.00001);assertEquals(0,forced.y,.00001);assertEquals(0,forced.z,.00001);
    }
    @Test void matchesOriginalBetaCuboidVerticesAndUvs() throws Exception {
        var jar=java.nio.file.Path.of("build/devtools/b1.7.3/client.jar");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(jar),"original Beta fixture unavailable");
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("ps");var facesField=type.getDeclaredField("k");facesField.setAccessible(true);
            for(boolean mirror:new boolean[]{false,true})for(int depth:new int[]{0,4})for(float grow:new float[]{0F,.5F}){
                Object original=type.getConstructor(int.class,int.class).newInstance(4,8);type.getField("g").setBoolean(original,mirror);
                type.getMethod("a",float.class,float.class,float.class,int.class,int.class,int.class,float.class).invoke(original,1F,2F,3F,2,3,depth,grow);
                Object[] faces=(Object[])facesField.get(original);
                var expected=ModelGeometryB173.vertices(new ModelGeometryB173.Cuboid(4,8,1,2,3,2,3,depth,grow,mirror));
                for(int f=0;f<6;f++){
                    Object[] vertices=(Object[])faces[f].getClass().getField("a").get(faces[f]);
                    for(int v=0;v<4;v++){
                        Object old=vertices[v],pos=old.getClass().getField("a").get(old);var actual=expected.get(f).get(v);
                        assertEquals(((Number)pos.getClass().getField("a").get(pos)).floatValue(),actual.x(),.00001F);
                        assertEquals(((Number)pos.getClass().getField("b").get(pos)).floatValue(),actual.y(),.00001F);
                        assertEquals(((Number)pos.getClass().getField("c").get(pos)).floatValue(),actual.z(),.00001F);
                        assertEquals(old.getClass().getField("b").getFloat(old),actual.u(),.00001F);
                        assertEquals(old.getClass().getField("c").getFloat(old),actual.v(),.00001F);
                    }
                }
            }
        }
    }
}
