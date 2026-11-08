package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.client.render.item.HeldItemRenderer;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class RendererExtrasB173Test {
    record Captured(String kind,Matrix4f transform,RendererContextB173.Style style,Object value){}
    @Test void originalGlStateAndAttachmentsReachSinksWithIndependentTextureTransforms() {
        var captures=new ArrayList<Captured>();
        var extras=new RendererContextB173.Extras(){
            @Override public void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,ItemStack stack,Matrix4f transform,RendererContextB173.Style style){captures.add(new Captured("item",transform,style,stack));}
            @Override public void text(String text,float x,float y,int argb,boolean shadow,Matrix4f transform,RendererContextB173.Style style){assertTrue(shadow);assertEquals(0x80abcdef,argb);captures.add(new Captured("text",transform,style,text));}
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,ResourceLocation texture,RendererContextB173.Style style){assertEquals(ResourceLocation.parse("fixture:skin"),texture);captures.add(new Captured("quad",transform,style,vertices));}
            @Override public int width(String text){return 42;}
        };
        try(var scope=RendererContextB173.open(path->ResourceLocation.parse("fixture:skin"),(draw,texture)->captures.add(new Captured("model",draw.transform(),RendererContextB173.style(),texture)),extras)) {
            GL11.glDisable(3553);GL11.glDisable(2896);GL11.glDisable(2929);GL11.glEnable(3042);
            GL11.glBlendFunc(770,1);GL11.glDepthMask(false);GL11.glDepthFunc(514);
            GL11.glTranslatef(2,3,4);GL11.glMatrixMode(5890);GL11.glLoadIdentity();GL11.glTranslatef(.25F,.5F,0);GL11.glScalef(2,2,1);GL11.glMatrixMode(5888);
            GL11.glBindTexture(3553,RendererContextB173.textureId("/skin.png"));
            var part=new ModelPart(0,0);part.addCuboid(0,0,0,1,1,1);part.render(.0625F);
            var stack=new ItemStack(340,1,0);new HeldItemRenderer().renderItem(null,stack);
            TextRenderer.mcc$instance().drawWithShadow("original",0,0,0x80abcdef);assertEquals(42,TextRenderer.mcc$instance().getWidth("original"));
            var tess=Tessellator.INSTANCE;tess.startQuads();tess.normal(0,0,1);tess.color(0x123456,128);
            tess.vertex(0,0,0,0,0);tess.vertex(1,0,0,1,0);tess.vertex(1,1,0,1,1);tess.vertex(0,1,0,0,1);tess.draw();
            GL11.glMatrixMode(5890);GL11.glLoadIdentity();GL11.glMatrixMode(5888);
            assertEquals(List.of("model","item","text","quad"),captures.stream().map(Captured::kind).toList());
            for(var capture:captures){
                var style=capture.style;assertFalse(style.texture2d());assertFalse(style.lighting());assertFalse(style.depthTest());assertFalse(style.depthWrite());assertTrue(style.blend());
                assertEquals(770,style.blendSrc());assertEquals(1,style.blendDst());assertEquals(514,style.depthFunc());
                assertEquals(new Vector3f(2,3,4),capture.transform.transformPosition(new Vector3f()));
                assertEquals(new Vector3f(.75F,1,0),style.textureMatrix().transformPosition(new Vector3f(.25F,.25F,0)));
            }
            @SuppressWarnings("unchecked") var quad=(List<RendererContextB173.Vertex>)captures.getLast().value;
            assertEquals(0x80123456,quad.getFirst().argb());assertEquals(1,quad.getFirst().nz());
        }
        assertFalse(RendererContextB173.active());
    }
    @Test void nestedContextsRestoreOuterStyleWithoutLeaking() {
        try(var outer=RendererContextB173.open(path->ResourceLocation.parse("fixture:skin"),(draw,texture)->{})) {
            GL11.glEnable(3042);GL11.glDepthMask(false);
            try(var inner=RendererContextB173.open(path->ResourceLocation.parse("fixture:other"),(draw,texture)->{})) {
                assertFalse(RendererContextB173.style().blend());assertTrue(RendererContextB173.style().depthWrite());
            }
            assertTrue(RendererContextB173.style().blend());assertFalse(RendererContextB173.style().depthWrite());
            assertThrows(UnsupportedOperationException.class,()->GL11.glEnable(12345));
        }
    }
}
