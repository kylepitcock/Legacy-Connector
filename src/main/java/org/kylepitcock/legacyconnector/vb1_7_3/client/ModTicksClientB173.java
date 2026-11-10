package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.kylepitcock.legacyconnector.vb1_7_3.*;
import net.minecraft.nbt.*;

/** ModLoader frame callbacks use authoritative integrated-server state and replay immutable HUD commands. */
final class ModTicksClientB173 {
    private static final AtomicBoolean PENDING=new AtomicBoolean();
    private static volatile List<Consumer<GuiGraphics>> display=List.of();
    private static long lastTick=Long.MIN_VALUE;
    private static net.minecraft.world.level.Level previous;
    private static Object remoteConnection;
    private static net.minecraft.world.level.Level remoteLevel;
    private static int remoteSession,helloTicks;
    private static boolean remoteReady;
    private static CompoundTag remoteFrame;
    private static final class TickMinecraft extends legacyconnector.vb1_7_3.net.minecraft.client.Minecraft {}
    private record Options(int width,int height,int guiWidth,int guiHeight,int scale,boolean hide,boolean thirdPerson,
            boolean screen,boolean paused,boolean focused,int[] metrics){}
    static void install(){
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderGuiEvent.Post event)->frame(event.getGuiGraphics()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->remoteTick());
        GameHookNetworkB173.installClient(new GameHookNetworkB173.Client(){
            public void ready(int session){var mc=Minecraft.getInstance();if(remoteConnection!=null&&remoteLevel!=null&&session==remoteSession&&remoteConnection==mc.getConnection()&&remoteLevel==mc.level){remoteReady=true;remoteFrame=null;}}
            public void frame(int session,CompoundTag frame){if(session==remoteSession&&remoteReady)remoteFrame=frame.copy();}
        });
    }
    private static void remoteTick(){
        var mc=Minecraft.getInstance();var connection=mc.getConnection();
        if(!ContentB173.hasClientHooks()||connection==null||mc.level==null||mc.player==null||mc.getSingleplayerServer()!=null
                ||!connection.hasChannel(org.kylepitcock.legacyconnector.core.network.LegacyPayload.TYPE)){
            if(remoteConnection==connection&&connection!=null&&(remoteReady||helloTicks>0))GameHookNetworkB173.stop(remoteSession);
            remoteConnection=null;remoteLevel=null;remoteReady=false;remoteFrame=null;helloTicks=0;return;
        }
        if(remoteConnection!=connection||remoteLevel!=mc.level){
            if(remoteConnection==connection&&(remoteReady||helloTicks>0))GameHookNetworkB173.stop(remoteSession);
            remoteConnection=connection;remoteLevel=mc.level;remoteSession++;remoteReady=false;remoteFrame=null;helloTicks=0;
        }
        int[] metrics=new int[256];for(int i=0;i<metrics.length;i++)metrics[i]=mc.font.width(String.valueOf((char)i));
        var context=new ServerGameHooksB173.Context(mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),mc.options.guiScale().get(),mc.options.hideGui,!mc.options.getCameraType().isFirstPerson(),mc.screen!=null,mc.isPaused(),mc.mouseHandler.isMouseGrabbed()&&mc.isWindowActive(),metrics);
        if(remoteReady)GameHookNetworkB173.sendContext(GameHookNetworkB173.ACTIVE,remoteSession,context);
        else if(helloTicks++%20==0)GameHookNetworkB173.sendContext(GameHookNetworkB173.HELLO,remoteSession,context);
    }
    private static void frame(GuiGraphics graphics){
        if(!ContentB173.hasClientHooks()){display=List.of();return;}
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){display=List.of();return;}
        if(previous!=mc.level){previous=mc.level;display=List.of();lastTick=Long.MIN_VALUE;}
        int[] metrics=new int[256];for(int i=0;i<metrics.length;i++)metrics[i]=mc.font.width(String.valueOf((char)i));
        var options=new Options(mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),mc.options.guiScale().get(),mc.options.hideGui,!mc.options.getCameraType().isFirstPerson(),mc.screen!=null,mc.isPaused(),mc.mouseHandler.isMouseGrabbed()&&mc.isWindowActive(),metrics);
        var server=mc.getSingleplayerServer();
        if(server==null&&!remoteReady)display=capture(mc.player,options);
        else if(server!=null&&PENDING.compareAndSet(false,true)){
            var uuid=mc.player.getUUID();var level=mc.level;
            server.execute(()->{
                try{var player=server.getPlayerList().getPlayer(uuid);if(player!=null){var next=capture(player,options);if(previous==level)display=next;}}
                catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Beta client frame callback failed",error);}
                finally{PENDING.set(false);}
            });
        }
        var before=HudState.capture();
        try{graphics.flush();ClientGuiB173.begin(graphics);if(server==null&&remoteReady)replayRemote(graphics);else for(var command:display)command.accept(graphics);}
        finally{try{ClientGuiB173.end();graphics.flush();}finally{before.restore();}}
    }
    private static void replayRemote(GuiGraphics graphics){
        var frame=remoteFrame;if(frame==null||!GameHookCanvasB173.balanced(frame))return;var mc=Minecraft.getInstance();
        var scale=GameHookCanvasB173.scale(frame.getInt("width"),frame.getInt("height"),mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        var commands=frame.getList("commands",Tag.TAG_COMPOUND);
        graphics.pose().pushPose();int depth=0;
        try{
        graphics.pose().scale(scale.x(),scale.y(),1);
        for(Tag raw:commands){var command=(CompoundTag)raw;int[] v=command.getIntArray("values");switch(command.getString("op")){
            case "quad"->{
                var points=command.getList("vertices",Tag.TAG_COMPOUND);if(points.size()!=4)continue;
                var vertices=new ArrayList<RendererContextB173.Vertex>();for(Tag point:points){var p=(CompoundTag)point;vertices.add(new RendererContextB173.Vertex(p.getDouble("x"),p.getDouble("y"),p.getDouble("z"),p.getDouble("u"),p.getDouble("v"),p.getInt("color"),0,1,0));}
                var state=command.getCompound("style");var style=new RendererContextB173.Style(true,false,state.getBoolean("depthTest"),state.getBoolean("depthWrite"),state.getBoolean("blend"),state.getInt("src"),state.getInt("dst"),state.getInt("depthFunc"),GameHookHudB173.matrix(state,"textureMatrix"));
                var texture=net.minecraft.resources.ResourceLocation.tryParse(command.getString("texture"));if(texture!=null)drawQuad(graphics,vertices,GameHookHudB173.matrix(command,"matrix"),texture,command.getInt("color"),style,1,1);
            }
            case "blit"->{if(v.length==6)GuiDrawB173.blit(v[0],v[1],v[2],v[3],v[4],v[5]);}
            case "fill"->{if(v.length==5)GuiDrawB173.fill(v[0],v[1],v[2],v[3],v[4]);}
            case "gradient"->{if(v.length==6)GuiDrawB173.fillGradient(v[0],v[1],v[2],v[3],v[4],v[5]);}
            case "text"->{if(v.length==4){graphics.pose().pushPose();try{graphics.pose().mulPose(GameHookHudB173.matrix(command,"matrix"));GuiDrawB173.text(command.getString("text"),v[0],v[1],v[2],v[3]!=0);}finally{graphics.pose().popPose();}}}
            case "bind"->GuiDrawB173.bind(GuiDrawB173.textureId(command.getString("path")));
            case "color"->GuiDrawB173.color(command.getFloat("r"),command.getFloat("g"),command.getFloat("b"),command.getFloat("a"));
            case "push"->{GuiDrawB173.push();depth++;}case "pop"->{GuiDrawB173.pop();depth--;}
            case "translate"->GuiDrawB173.translate(command.getFloat("x"),command.getFloat("y"),command.getFloat("z"));
            case "scale"->GuiDrawB173.scale(command.getFloat("x"),command.getFloat("y"),command.getFloat("z"));case "click"->GuiDrawB173.click();default->{}
        }}
        }finally{while(depth>0){graphics.pose().popPose();depth--;}graphics.pose().popPose();}
    }
    private static List<Consumer<GuiGraphics>> capture(net.minecraft.world.entity.player.Player nativePlayer,Options options){
        var commands=new ArrayList<Consumer<GuiGraphics>>();var original=new TickMinecraft();
        original.world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(nativePlayer.level());
        original.displayWidth=options.width;original.displayHeight=options.height;original.paused=options.paused;
        original.options.hideHud=options.hide;original.options.thirdPerson=options.thirdPerson;original.options.guiScale=options.scale;
        original.currentScreen=options.screen?new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen():null;
        original.focused=options.focused;
        var scaler=new legacyconnector.vb1_7_3.net.minecraft.client.util.ScreenScaler(original.options,options.width,options.height);
        long tick=nativePlayer.level().getGameTime();boolean advanced=tick!=lastTick;lastTick=tick;
        var sink=new GuiCapture(commands,options.metrics);
        legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$scope(original,()->{
            try(var views=EntityViewsB173.callbackScope()){
                EntityViewsB173.refreshCached(nativePlayer.level());
                var player=(PlayerViewB173)EntityViewsB173.wrap(nativePlayer);
                legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(original,player);
                try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(player.world,player);
                        var renderer=RendererContextB173.open(path->MobTexturesB173.resolve(null,path.replace("%blur%","").replace("%clamp%","")),(draw,texture)->{},
                                new RendererContextB173.Extras(){
                                    @Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f matrix,
                                            net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){
                                        var copy=List.copyOf(vertices);var transform=new org.joml.Matrix4f(matrix);int color=RendererContextB173.argb();
                                        commands.add(g->drawQuad(g,copy,transform,texture,color,style,(float)options.guiWidth/scaler.getScaledWidth(),(float)options.guiHeight/scaler.getScaledHeight()));
                                    }
                                })){
                    PlayerInventoryNetworkB173.capture(player,original,options.screen,()->
                        GuiDrawB173.withSink(sink,()->{ContentB173.clientHooks(original,false,advanced);if(options.screen)ContentB173.clientHooks(original,true,advanced);}));
                    player.writeBack();
                }
            }finally{EntityViewsB173.flush();BlockEntityViewsB173.flush();}
        });
        return List.copyOf(commands);
    }
    private static void drawQuad(GuiGraphics graphics,List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f matrix,
            net.minecraft.resources.ResourceLocation texture,int color,RendererContextB173.Style style,float sx,float sy){
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,minU=1,minV=1,maxU=0,maxV=0;
        for(var vertex:vertices){var p=matrix.transformPosition(new org.joml.Vector3f((float)vertex.x(),(float)vertex.y(),(float)vertex.z()));
            minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);
            minU=Math.min(minU,vertex.u());minV=Math.min(minV,vertex.v());maxU=Math.max(maxU,vertex.u());maxV=Math.max(maxV,vertex.v());}
        var before=HudState.capture();
        try{
            graphics.flush();
            if(style.blend())RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.blendFunc(style.blendSrc(),style.blendDst());
            if(style.depthTest())RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(style.depthWrite());RenderSystem.depthFunc(style.depthFunc());
            RenderSystem.setShaderColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,(color>>>24)/255F);
            // This immediate blit leaves blending to the captured original GL state, including PNG alpha.
            graphics.blit(texture,(int)(minX*sx),(int)(minY*sy),(int)((maxX-minX)*sx),(int)((maxY-minY)*sy),
                    (float)(minU*256),(float)(minV*256),(int)((maxU-minU)*256),(int)((maxV-minV)*256),256,256);
        }finally{before.restore();}
    }
    /** Native cache snapshots avoid GL queries and keep legacy HUD state from leaking into later GUI draws. */
    private record HudState(boolean blend,int srcRgb,int dstRgb,int srcAlpha,int dstAlpha,
            boolean depthTest,boolean depthWrite,int depthFunc,float[] color,net.minecraft.client.renderer.ShaderInstance shader,int texture){
        static HudState capture(){return new HudState(GlStateManager.BLEND.mode.enabled,GlStateManager.BLEND.srcRgb,GlStateManager.BLEND.dstRgb,
                GlStateManager.BLEND.srcAlpha,GlStateManager.BLEND.dstAlpha,GlStateManager.DEPTH.mode.enabled,GlStateManager.DEPTH.mask,
                GlStateManager.DEPTH.func,RenderSystem.getShaderColor().clone(),RenderSystem.getShader(),RenderSystem.getShaderTexture(0));}
        void restore(){
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.depthMask(depthWrite);RenderSystem.depthFunc(depthFunc);if(depthTest)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,texture);
        }
    }
    private static final class GuiCapture implements GuiDrawB173.Sink {
        final List<Consumer<GuiGraphics>> commands;final int[] metrics;final Map<Integer,String> paths=new HashMap<>();
        GuiCapture(List<Consumer<GuiGraphics>> commands,int[] metrics){this.commands=commands;this.metrics=metrics;}
        public void blit(int x,int y,int u,int v,int width,int height){commands.add(g->GuiDrawB173.blit(x,y,u,v,width,height));}
        public void fill(int a,int b,int c,int d,int color){commands.add(g->GuiDrawB173.fill(a,b,c,d,color));}
        public void fillGradient(int a,int b,int c,int d,int top,int bottom){commands.add(g->GuiDrawB173.fillGradient(a,b,c,d,top,bottom));}
        public void text(String text,int x,int y,int color,boolean shadow){commands.add(g->GuiDrawB173.text(text,x,y,color,shadow));}
        public int width(String text){int width=0;for(char c:text.toCharArray())width+=c<metrics.length?metrics[c]:6;return width;}
        public int textureId(String path){int id=paths.size()+1;paths.put(id,path);return id;}
        public int load(java.awt.image.BufferedImage image){throw new UnsupportedOperationException("Dynamic HUD bitmap capture is not implemented");}
        public void bind(int id){String path=paths.get(id);commands.add(g->GuiDrawB173.bind(GuiDrawB173.textureId(path)));}
        public void color(float r,float g,float b,float a){commands.add(gg->GuiDrawB173.color(r,g,b,a));}
        public void push(){commands.add(g->GuiDrawB173.push());}public void pop(){commands.add(g->GuiDrawB173.pop());}
        public void translate(float x,float y,float z){commands.add(g->GuiDrawB173.translate(x,y,z));}
        public void scale(float x,float y,float z){commands.add(g->GuiDrawB173.scale(x,y,z));}
        public void click(){commands.add(g->GuiDrawB173.click());}
    }
}
