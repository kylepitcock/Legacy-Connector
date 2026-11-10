package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.PlayerEntityRenderer;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PlayerRenderingB173Test {
    private static final ResourceLocation SKIN=ResourceLocation.withDefaultNamespace("skin");
    private static final class Player extends legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity {Player(){super(null);}}
    private static class Original extends PlayerEntityRenderer {
        final ModelPart extra=new ModelPart(0,0);
        int bases,more;
        Original(){extra.addCuboid(0,0,0,1,1,1);}
        @Override public void render(Entity player,double x,double y,double z,float yaw,float partial){
            GL11.glColor4f(1,0,0,1);extra.render(.0625F);
            super.render(player,x,y,z,yaw,partial);bases++;
            bindTexture("/extra.png");GL11.glColor4f(0,0,1,.5F);GL11.glEnable(3042);extra.render(.0625F);GL11.glDisable(3042);
        }
        @Override public void renderMore(LivingEntity player,float partial){more++;extra.render(.0625F);}
    }
    @Test void nativeBaseHasOneMarkerAndOriginalModelStateSurvivesDeferredReplay(){
        var renderer=new Original();var player=new Player();var colors=new ArrayList<Integer>();var textures=new ArrayList<ResourceLocation>();
        var transforms=new ArrayList<org.joml.Matrix4f>();
        var styles=new ArrayList<RendererContextB173.Style>();
        var plan=PlayerRenderingB173.capture(renderer,player,.5F,false,
                path->path.equals("/mob/char.png")?SKIN:ResourceLocation.withDefaultNamespace("extra"),
                (part,texture,style,color)->{colors.add(color);textures.add(texture);transforms.add(part.transform());styles.add(style);},new RendererContextB173.Extras(){});
        assertFalse(RendererContextB173.active());assertFalse(PlayerRenderPhasesB173.active());
        assertTrue(plan.nativeBase());assertEquals(1,renderer.bases);assertEquals(1,renderer.more);
        assertTrue(colors.isEmpty());plan.beforeBase();assertEquals(List.of(0xFFFF0000),colors);
        plan.afterBase();assertEquals(List.of(0xFFFF0000,0xFFFF0000,0x800000FF),colors);
        assertEquals(List.of(SKIN,SKIN,ResourceLocation.withDefaultNamespace("extra")),textures);
        assertFalse(styles.get(0).blend());assertTrue(styles.get(2).blend());
        // Original player yOffset is removed once; the Beta model is scaled .9375 and translated -1.5078125.
        var origin=transforms.get(1).transformPosition(new Vector3f());
        assertEquals(0,origin.x,1E-6);assertEquals(1.41357421875,origin.y,1E-6);assertEquals(0,origin.z,1E-6);
    }
    @Test void omittedBaseRetainsOriginalVisibilityAndHandUsesSameFinitePhases(){
        var hidden=new PlayerEntityRenderer(){@Override public void render(Entity p,double x,double y,double z,float yaw,float partial){}};
        var plan=PlayerRenderingB173.capture(hidden,new Player(),0,false,p->SKIN,(a,b,c,d)->fail(),new RendererContextB173.Extras(){});
        assertFalse(plan.nativeBase());assertTrue(plan.before().isEmpty());assertTrue(plan.after().isEmpty());
        var order=new ArrayList<String>();
        var hand=new PlayerEntityRenderer(){@Override public void renderHand(){PlayerRenderPhasesB173.emit(()->order.add("before"));super.renderHand();PlayerRenderPhasesB173.emit(()->order.add("after"));}};
        var handPlan=PlayerRenderingB173.capture(hand,new Player(),1,true,p->SKIN,(a,b,c,d)->fail("Native hand must not emit Beta skin geometry"),new RendererContextB173.Extras(){});
        assertTrue(handPlan.nativeBase());handPlan.beforeBase();order.add("native");handPlan.afterBase();assertEquals(List.of("before","native","after"),order);
    }
    @Test void rejectedOrNestedCallbacksRestoreScopesWithoutPartialNativeRendering(){
        assertThrows(UnsupportedOperationException.class,()->PlayerRenderPhasesB173.capture(()->{PlayerRenderPhasesB173.nativeBase();PlayerRenderPhasesB173.nativeBase();}));
        assertFalse(PlayerRenderPhasesB173.active());
        var order=new ArrayList<String>();
        var outer=PlayerRenderPhasesB173.capture(()->{
            PlayerRenderPhasesB173.emit(()->order.add("outer-before"));
            var nested=PlayerRenderPhasesB173.capture(()->PlayerRenderPhasesB173.emit(()->order.add("nested")));
            assertFalse(nested.nativeBase());nested.beforeBase();PlayerRenderPhasesB173.nativeBase();
            PlayerRenderPhasesB173.emit(()->order.add("outer-after"));
        });
        outer.beforeBase();outer.afterBase();assertEquals(List.of("nested","outer-before","outer-after"),order);
        var transformed=new PlayerEntityRenderer(){@Override public void render(Entity p,double x,double y,double z,float yaw,float partial){GL11.glTranslatef(1,0,0);super.render(p,x,y,z,yaw,partial);}};
        assertThrows(UnsupportedOperationException.class,()->PlayerRenderingB173.capture(transformed,new Player(),0,false,p->SKIN,(a,b,c,d)->fail(),new RendererContextB173.Extras(){}));
        assertFalse(RendererContextB173.active());assertFalse(PlayerRenderPhasesB173.active());
    }
    @Test void anotherPlayersRendererRetainsLocalMinecraftIdentityAndPendingLocalState(){
        var local=new Player();var other=new Player();local.health=7;other.health=19;
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        var renderer=new PlayerEntityRenderer(){
            @Override public void render(Entity passed,double x,double y,double z,float yaw,float partial){
                assertSame(other,passed);
                assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft));
                assertNotSame(passed,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft));
                assertEquals(7,local.health);local.health=6;
                PlayerRenderingB173.withClientIdentity(local,local,()->{
                    assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft));
                    assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
                    assertEquals(6,local.health);
                });
                assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft));
                assertSame(other,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
            }
        };
        try(var outer=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(null,local)){
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(minecraft,local);
            PlayerRenderingB173.withClientIdentity(other,local,()->{
                assertSame(other,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
                var plan=PlayerRenderingB173.capture(renderer,other,0,false,p->SKIN,(a,b,c,d)->fail(),new RendererContextB173.Extras(){});
                assertFalse(plan.nativeBase());
            });
            assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft));
            assertSame(local,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
            assertEquals(6,local.health);assertEquals(19,other.health);
        }
    }
}
