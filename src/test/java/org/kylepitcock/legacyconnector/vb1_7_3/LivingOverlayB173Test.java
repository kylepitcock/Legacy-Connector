package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.LivingEntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class LivingOverlayB173Test {
    record Sample(String model,int color,RendererContextB173.Style style,Matrix4f matrix){}
    static final class Mob extends LivingEntity {
        Mob(){super(null);}
        @Override public float getBrightnessAtEyes(float partial){assertEquals(.5F,partial);return .75F;}
    }
    static final class Probe extends LivingEntityRenderer {
        final List<Sample> samples;
        final List<String> calls=new ArrayList<>();
        int color,extras;
        Probe(List<Sample> samples){super(model("body",samples),.5F);this.samples=samples;setDecorationModel(model("layer",samples));}
        static EntityModel model(String name,List<Sample> samples){return new EntityModel(){
            @Override public void render(float limb,float amount,float age,float yaw,float pitch,float scale){
                samples.add(new Sample(name,RendererContextB173.argb(),RendererContextB173.style(),RendererContextB173.matrix()));
            }
        };}
        @Override public boolean bindTexture(LivingEntity entity,int pass,float partial){calls.add("normal"+pass);return pass==0;}
        @Override public boolean bindDecorationTexture(LivingEntity entity,int pass,float partial){
            calls.add("overlay"+pass);GL11.glColor4f(0,1,0,1);return pass==1;
        }
        @Override public void renderMore(LivingEntity entity,float partial){calls.add("extras");extras++;}
        @Override public int getOverlayColor(LivingEntity entity,float brightness,float partial){
            calls.add("color");assertEquals(.75F,brightness);assertEquals(.5F,partial);return color;
        }
    }
    @Test void originalOrderedPassesColorsAndState(){
        for(int state=0;state<6;state++){
            var samples=new ArrayList<Sample>();var renderer=new Probe(samples);var mob=new Mob();
            mob.hurtTime=state==1||state==3||state==5?2:0;
            mob.deathTime=state==2||state==3?2:0;
            renderer.color=state>=4?0x80402010:0;
            boolean hurt=mob.hurtTime>0||mob.deathTime>0;
            int overlays=(hurt?1:0)+(state>=4?1:0);
            try(var scope=RendererContextB173.open(path->ResourceLocation.withDefaultNamespace("test"),(draw,texture)->{})){
                renderer.render(mob,2,3,4,0,.5F);
                assertEquals(2+2*overlays,samples.size(),"state "+state);
                assertEquals(List.of("normal0","normal1","normal2","normal3","extras","color"),renderer.calls.subList(0,6));
                assertEquals(1,renderer.extras);
                assertEquals("body",samples.get(0).model());assertEquals("layer",samples.get(1).model());
                assertTrue(samples.getFirst().style().texture2d());assertFalse(samples.getFirst().style().blend());
                for(int pass=0;pass<overlays;pass++){
                    assertEquals(List.of("overlay0","overlay1","overlay2","overlay3"),renderer.calls.subList(6+pass*4,10+pass*4));
                    int expected=hurt&&pass==0?0x66bf0000:0x80402010;
                    for(int part=0;part<2;part++){
                        var sample=samples.get(2+pass*2+part);
                        assertEquals(part==0?"body":"layer",sample.model());assertEquals(expected,sample.color());
                        var style=sample.style();assertFalse(style.texture2d());assertTrue(style.blend());
                        assertTrue(style.depthWrite());assertTrue(style.lighting());assertEquals(514,style.depthFunc());
                        assertEquals(770,style.blendSrc());assertEquals(771,style.blendDst());
                        assertTrue(samples.getFirst().matrix().equals(sample.matrix(),.00001F),"overlay reuses original transformed pose");
                    }
                }
                assertEquals(515,RendererContextB173.style().depthFunc());assertFalse(RendererContextB173.style().blend());
                assertTrue(RendererContextB173.style().texture2d());assertTrue(RendererContextB173.style().depthWrite());
                assertTrue(RendererContextB173.matrix().equals(new Matrix4f(),.00001F),"outer pose restored");
            }
        }
    }
    @Test void replacementRendererRetainsControl(){
        int[] draws={0};var mob=new Mob();mob.hurtTime=4;
        var renderer=new LivingEntityRenderer(new EntityModel(){},0){
            @Override public void render(LivingEntity entity,double x,double y,double z,float yaw,float partial){draws[0]++;}
        };
        try(var scope=RendererContextB173.open(path->ResourceLocation.withDefaultNamespace("test"),(draw,texture)->fail("replacement must control its draws"))){
            renderer.render(mob,0,0,0,0,.5F);assertEquals(1,draws[0]);assertEquals(-1,RendererContextB173.argb());
        }
    }
}
