package org.kylepitcock.legacyconnector.vb1_7_3.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

class SpriteQuadBindingB173Test {
    private static final ResourceLocation FIRE=ResourceLocation.parse("minecraft:textures/block/fire_0.png");
    private static final SpriteQuadBindingB173.Bounds CELL=new SpriteQuadBindingB173.Bounds(.25F,.5F,.5F,.75F);
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void animatedSourceUsesAtlasFrameAndAppliesLegacyMatrixBeforeAtlasMapping(){
        var legacy=new Matrix4f().translation(.2F,.3F,0).scale(.5F,.25F,1);var saved=new Matrix4f(legacy);
        var binding=SpriteQuadBindingB173.resolve(FIRE,legacy,true,id->{assertEquals(ResourceLocation.parse("minecraft:block/fire_0"),id);return CELL;});
        assertEquals(TextureAtlas.LOCATION_BLOCKS,binding.texture());assertEquals(saved,legacy);
        var uv=binding.matrix().transformPosition(new Vector3f(1,1,0));
        assertEquals(.425F,uv.x,.000001);assertEquals(.6375F,uv.y,.000001);
        var origin=binding.matrix().transformPosition(new Vector3f());
        assertEquals(.3F,origin.x,.000001);assertEquals(.575F,origin.y,.000001);
    }
    @Test void arbitraryCustomTexturesMissingSpritesAndUntexturedDrawsKeepTheirBinding(){
        var matrix=new Matrix4f().translation(.2F,.3F,0);
        var custom=ResourceLocation.parse("fixture:textures/entity/custom_mob.png");
        var direct=SpriteQuadBindingB173.resolve(custom,matrix,true,id->fail("custom mob skin must not enter atlas lookup"));
        assertEquals(custom,direct.texture());assertEquals(matrix,direct.matrix());assertNotSame(matrix,direct.matrix());
        assertEquals(FIRE,SpriteQuadBindingB173.resolve(FIRE,matrix,true,id->null).texture());
        assertEquals(FIRE,SpriteQuadBindingB173.resolve(FIRE,matrix,false,id->fail("untextured draw does not resolve a sprite")).texture());
        var item=ResourceLocation.parse("fixture:textures/item/custom.png");
        assertEquals(TextureAtlas.LOCATION_BLOCKS,SpriteQuadBindingB173.resolve(item,matrix,true,id->{assertEquals(ResourceLocation.parse("fixture:item/custom"),id);return CELL;}).texture());
    }
    @Test void nativeQuadBackendEmitsAtlasUvsOnceAndRestoresLegacyTextureMatrix(){
        var matrix=new Matrix4f().translation(.2F,.3F,0).scale(.5F,.25F,1);
        var style=new RendererContextB173.Style(true,true,true,true,false,770,771,515,matrix);
        var vertices=List.of(vertex(0,0),vertex(1,0),vertex(1,1),vertex(0,1));
        var capture=RenderContext.beginCapture();
        try{
            NativeRendererExtrasB173.drawQuad(vertices,new Matrix4f(),FIRE,style,new com.mojang.blaze3d.vertex.PoseStack(),id->CELL);
            assertEquals(1,capture.size());assertEquals(TextureAtlas.LOCATION_BLOCKS,capture.texture(0));
            assertEquals(.3,capture.get(0,0,3),.000001);assertEquals(.575,capture.get(0,0,4),.000001);
            assertEquals(.425,capture.get(0,2,3),.000001);assertEquals(.6375,capture.get(0,2,4),.000001);
            var custom=ResourceLocation.parse("fixture:textures/entity/custom_mob.png");RenderContext.bindTexture(custom);
            RenderContext.quad(new double[][]{{0,0,0,0,0,-1,0,1},{1,0,0,1,0,-1,0,1},{1,1,0,1,1,-1,0,1},{0,1,0,0,1,-1,0,1}},0,0,1);
            assertEquals(custom,capture.texture(1));assertEquals(.2,capture.get(1,0,3),.000001);assertEquals(.3,capture.get(1,0,4),.000001);
            assertEquals(.7,capture.get(1,2,3),.000001);assertEquals(.55,capture.get(1,2,4),.000001);
            assertEquals(new Matrix4f().translation(.2F,.3F,0).scale(.5F,.25F,1),style.textureMatrix());
        }finally{RenderContext.endCapture();}
    }
    private static RendererContextB173.Vertex vertex(float u,float v){return new RendererContextB173.Vertex(u,v,0,u,v,-1,0,0,1);}
    @Test void wrappedTextureCoordinatesKeepRawBindingAndTheSingleLegacyTransform(){
        var vertices=List.of(vertex(0,0),vertex(1,0),vertex(1,1),vertex(0,1));
        var capture=RenderContext.beginCapture();
        try{
            for(float offset:new float[]{-.2F,.2F}){
                var matrix=new Matrix4f().translation(offset,0,0);
                assertFalse(SpriteQuadBindingB173.normalized(vertices,matrix));
                var style=new RendererContextB173.Style(true,true,true,true,false,770,771,515,matrix);
                NativeRendererExtrasB173.drawQuad(vertices,new Matrix4f(),FIRE,style,new com.mojang.blaze3d.vertex.PoseStack(),id->fail("wrapped UVs must not enter atlas lookup"));
            }
            assertEquals(FIRE,capture.texture(0));assertEquals(FIRE,capture.texture(1));
            assertEquals(-.2,capture.get(0,0,3),.000001);assertEquals(.8,capture.get(0,2,3),.000001);
            assertEquals(.2,capture.get(1,0,3),.000001);assertEquals(1.2,capture.get(1,2,3),.000001);
            // Check the actual quad domain, not the whole unit square: this transformed subregion is safe.
            var inset=List.of(vertex(0,0),vertex(.5F,0),vertex(.5F,.5F),vertex(0,.5F));
            assertTrue(SpriteQuadBindingB173.normalized(inset,new Matrix4f().translation(.2F,.2F,0)));
        }finally{RenderContext.endCapture();}
    }
}
