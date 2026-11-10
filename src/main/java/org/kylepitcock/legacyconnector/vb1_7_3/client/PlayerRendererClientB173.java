package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Native body events are never re-entered. Only admitted original player registrations participate. */
public final class PlayerRendererClientB173 {
    private record Pending(PoseStack pose,PlayerRenderPhasesB173.Plan plan){}
    private static final Map<net.minecraft.world.entity.player.Player,Pending> PENDING=new IdentityHashMap<>();
    private static final Set<Class<?>> REPORTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean installed;
    private PlayerRendererClientB173(){}
    public static void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST,PlayerRendererClientB173::before);
        bus.addListener(PlayerRendererClientB173::after);
        bus.addListener(EventPriority.LOWEST,PlayerRendererClientB173::arm);
        // A different event subscriber can cancel after Pre; such recordings must not retain player views.
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Pre event)->PENDING.clear());
    }
    private static RenderersB173.Info info(){
        return RenderersB173.info(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class);
    }
    private static PlayerRenderPhasesB173.Plan capture(AbstractClientPlayer player,float partial,boolean hand,
            PoseStack pose,MultiBufferSource buffers,RenderersB173.Info info){
        var cached=PlayerRenderingB173.cachedClientView(player);
        final var view=cached==null?PlayerViewB173.of(player):cached;view.mcc$prepareRenderView();
        var nativeLocal=Minecraft.getInstance().player;
        // Resolve once before original callbacks. A getter during rendering must neither borrow
        // the rendered remote player nor refresh the local view and discard pending original state.
        var cachedLocal=PlayerRenderingB173.cachedClientView(nativeLocal);
        var local=nativeLocal==player?view:nativeLocal==null?null:cachedLocal!=null?cachedLocal:PlayerViewB173.of(nativeLocal);
        PlayerRenderPhasesB173.Plan[] result={null};
        LegacyModLocks.serverSide(LegacyModLocks.of(info.mod().instance),()->{
          try(var readOnly=EntityViewsB173.readOnly()){
            PlayerRenderingB173.withClientIdentity(view,local,()->{
            result[0]=PlayerRenderingB173.capture(
                    (PlayerEntityRenderer)info.renderer(),view,partial,hand,
                    path->PlayerRenderingB173.texture(info,path,player.getSkin().texture()),
                    (part,texture,style,color)->{
                        NativeRendererExtrasB173.applyStyle(style);
                        RenderContext.color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
                        RenderContext.bindTexture(texture);ModelPartsB173.draw(part);
                    },NativeRendererExtrasB173.create(player,pose,buffers));
            });
          }
        });
        return result[0];
    }
    private static void replay(Runnable commands,PoseStack pose,MultiBufferSource buffers,int light){
        RenderContext.beginWorld(pose,buffers,light,OverlayTexture.NO_OVERLAY,RenderContext.KIND_WORLD,true);
        pose.pushPose();try{commands.run();}finally{pose.popPose();RenderContext.end();}
    }
    private static void before(RenderPlayerEvent.Pre event){
        PENDING.remove(event.getEntity());if(event.isCanceled())return;
        var info=info();if(info==null||!(info.renderer() instanceof PlayerEntityRenderer)||!(event.getEntity() instanceof AbstractClientPlayer player))return;
        try{
            var plan=capture(player,event.getPartialTick(),false,event.getPoseStack(),event.getMultiBufferSource(),info);
            replay(plan::beforeBase,event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());
            if(plan.nativeBase())PENDING.put(player,new Pending(event.getPoseStack(),plan));
            else event.setCanceled(true);
        }catch(RuntimeException|LinkageError error){report(info,error);}
    }
    private static void after(RenderPlayerEvent.Post event){
        var pending=PENDING.remove(event.getEntity());if(pending==null||pending.pose()!=event.getPoseStack())return;
        try{replay(pending.plan()::afterBase,event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());}
        catch(RuntimeException|LinkageError error){report(info(),error);}
    }
    private static void arm(RenderArmEvent event){
        if(event.isCanceled()||event.getArm()!=HumanoidArm.RIGHT)return;
        var info=info();if(info==null||!(info.renderer() instanceof PlayerEntityRenderer))return;
        try{
            var plan=capture(event.getPlayer(),1,true,event.getPoseStack(),event.getMultiBufferSource(),info);
            // RenderArm has no Post. Invoke just the public native model geometry at the recorded
            // base marker; calling renderRightHand would fire the same event a second time.
            replay(()->{
                plan.beforeBase();
                if(plan.nativeBase())nativeArm(event);
                plan.afterBase();
            },event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());
            event.setCanceled(true);
        }catch(RuntimeException|LinkageError error){report(info,error);}
    }
    private static void nativeArm(RenderArmEvent event){
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(event.getPlayer());
        if(!(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer nativeRenderer))return;
        var model=nativeRenderer.getModel();
        model.setAllVisible(!event.getPlayer().isSpectator());
        model.rightSleeve.visible=event.getPlayer().isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        model.rightArmPose=net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose=net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        model.attackTime=0;model.crouching=false;model.swimAmount=0;
        model.setupAnim(event.getPlayer(),0,0,0,0,0);model.rightArm.xRot=0;
        var texture=event.getPlayer().getSkin().texture();
        model.rightArm.render(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.entitySolid(texture)),event.getPackedLight(),OverlayTexture.NO_OVERLAY);
        model.rightSleeve.xRot=0;
        model.rightSleeve.render(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(texture)),event.getPackedLight(),OverlayTexture.NO_OVERLAY);
    }
    private static void report(RenderersB173.Info info,Throwable error){
        if(info!=null&&REPORTED.add(info.renderer().getClass()))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Render").error("Original Beta player renderer failed; retaining native fallback for this draw",error);
    }
}
