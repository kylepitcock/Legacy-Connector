package org.kylepitcock.legacyconnector.vb1_7_3.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

class WorldOverlayStateB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void exactWorldPassCarriesEqualDepthAndStandardAlphaWithoutChangingDefaultOrNestedFrames(){
        var selected=new ArrayList<RenderType>();MultiBufferSource buffers=type->{selected.add(type);return null;};var pose=new PoseStack();
        RenderContext.beginWorld(pose,buffers,0,OverlayTexture.NO_OVERLAY,RenderContext.KIND_WORLD,true);
        try{
            RenderContext.setTexture2d(false);RenderContext.setBlend(true);RenderContext.setBlendFunc(770,771);RenderContext.setDepthFunc(514);
            RenderContext.geometryBuffer();String exact=selected.getLast().toString();
            assertTrue(exact.contains("legacy_depth_514w_c15"),exact);assertTrue(exact.contains("legacy_blend_770_771"),exact);
            assertEquals(OverlayTexture.NO_OVERLAY,RenderContext.overlay());
            RenderContext.beginWorld(pose,buffers,0,OverlayTexture.NO_OVERLAY);
            try{RenderContext.geometryBuffer();assertFalse(selected.getLast().toString().contains("legacy_depth_514"));assertFalse(selected.getLast().toString().contains("legacy_blend_770_771"));}
            finally{RenderContext.end();}
            RenderContext.geometryBuffer();assertEquals(exact,selected.getLast().toString());
        }finally{RenderContext.end();}
        RenderContext.beginWorld(pose,buffers,0,OverlayTexture.NO_OVERLAY);
        try{
            RenderContext.setTexture2d(false);RenderContext.setBlend(true);RenderContext.setDepthFunc(514);RenderContext.geometryBuffer();
            assertFalse(selected.getLast().toString().contains("legacy_depth_514"));
            assertFalse(selected.getLast().toString().contains("legacy_blend_770_771"));
        }finally{RenderContext.end();}
    }
}
