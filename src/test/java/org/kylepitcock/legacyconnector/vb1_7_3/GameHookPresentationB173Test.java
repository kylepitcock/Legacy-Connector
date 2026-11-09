package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GameHookPresentationB173Test {
    @BeforeAll static void bootstrap()throws Exception{
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static ServerGameHooksB173.Context context(){int[] metrics=new int[256];Arrays.fill(metrics,6);metrics['A']=14;return new ServerGameHooksB173.Context(854,480,427,240,0,false,false,false,false,metrics);}
    @Test void contextTransportContainsPresentationOnlyAndRoundTripsOptions(){
        var value=context();var encoded=GameHookNetworkB173.writeContext(value);
        assertEquals(Set.of("width","height","guiWidth","guiHeight","scale","hideHud","thirdPerson","screen","paused","metrics"),encoded.getAllKeys());
        encoded.putString("uuid","ignored");encoded.putFloat("fallDistance",-1);encoded.putBoolean("wearingBoots",true);
        var decoded=GameHookNetworkB173.readContext(encoded);assertEquals(value.width(),decoded.width());assertEquals(value.height(),decoded.height());assertEquals(value.guiWidth(),decoded.guiWidth());assertArrayEquals(value.metrics(),decoded.metrics());
        var again=GameHookNetworkB173.writeContext(decoded);assertFalse(again.contains("uuid"));assertFalse(again.contains("fallDistance"));assertFalse(again.contains("wearingBoots"));
    }
    @Test void originalImmediateHudCallsRetainAlphaBlendDepthAndTransformsWithoutGraphics(){
        int[] width={0};var frame=GameHookHudB173.capture(context(),()->{
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glEnable(3042);
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glBlendFunc(770,771);
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glDisable(2929);
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glDepthMask(false);
            legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glTranslatef(2,3,0);
            width[0]=legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer.mcc$instance().getWidth("A");
            var t=legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.INSTANCE;
            t.startQuads();t.vertex(0,240,0,0,1);t.vertex(427,240,0,1,1);t.vertex(427,0,0,1,0);t.vertex(0,0,0,0,0);t.draw();
        });
        assertEquals(14,width[0]);assertEquals(427,frame.getInt("width"));assertEquals(240,frame.getInt("height"));
        var commands=frame.getList("commands",Tag.TAG_COMPOUND);assertEquals(1,commands.size());var command=commands.getCompound(0);assertEquals("quad",command.getString("op"));
        var state=command.getCompound("style");assertTrue(state.getBoolean("blend"));assertEquals(770,state.getInt("src"));assertEquals(771,state.getInt("dst"));assertFalse(state.getBoolean("depthTest"));assertFalse(state.getBoolean("depthWrite"));
        assertEquals(4,command.getList("vertices",Tag.TAG_COMPOUND).size());
        var origin=GameHookHudB173.matrix(command,"matrix").transformPosition(new org.joml.Vector3f());assertEquals(2,origin.x);assertEquals(3,origin.y);
    }
    @Test void hudDrawingBoundDoesNotAbortTheOriginalGameplayCallback(){
        boolean[] completed={false};var frame=GameHookHudB173.capture(context(),()->{
            GuiDrawB173.push();for(int i=0;i<1000;i++)GuiDrawB173.fill(0,0,1,1,-1);GuiDrawB173.pop();completed[0]=true;
        });
        assertTrue(completed[0]);assertTrue(frame.getList("commands",Tag.TAG_COMPOUND).isEmpty());
    }
}
