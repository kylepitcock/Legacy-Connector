package org.kylepitcock.legacyconnector.vb1_7_3;

import io.netty.buffer.Unpooled;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.kylepitcock.legacyconnector.core.network.*;

/** Plain entity screens remain on their owning server thread; clients receive drawing commands and send input. */
public final class GuiSessionsB173 {
    private static final String VERSION="b1.7.3-gui";
    private static final AtomicInteger NEXT=new AtomicInteger();
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static boolean installed;
    private static final ThreadLocal<java.util.function.BiConsumer<ServerPlayer,LegacyPayload>> SENDER=new ThreadLocal<>();
    /** Scoped transport for packet-level regression capture; the server screen still executes the production path. */
    public static void withSender(java.util.function.BiConsumer<ServerPlayer,LegacyPayload> sender,Runnable callback){var previous=SENDER.get();SENDER.set(sender);try{callback.run();}finally{if(previous==null)SENDER.remove();else SENDER.set(previous);}}
    private GuiSessionsB173(){}
    /** The original server-owned screen, without constructing or opening a new session. */
    public static Screen currentScreen(ServerPlayer player){var session=SESSIONS.get(player.getUUID());return session==null?null:session.gui;}
    public static synchronized void install(){if(installed)return;installed=true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player)disconnect(player);});
        LegacyNetwork.registerReceiver(VERSION,GuiSessionsB173::receive);
    }
    public static void receive(LegacyPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context){context.enqueueWork(()->{
        CompoundTag tag=decode(payload.data());
        if(payload.discriminator()==0&&context.player() instanceof ServerPlayer player)input(player,tag);
        else if(payload.discriminator()==1&&context.flow()==net.minecraft.network.protocol.PacketFlow.CLIENTBOUND)MenusB173.remoteFrame(tag);
    });}
    private static LegacyPayload packet(int kind,CompoundTag tag){FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());try{buffer.writeNbt(tag);if(buffer.readableBytes()>1_048_576)throw new IllegalStateException("Legacy GUI frame exceeds native payload limit");byte[] data=new byte[buffer.readableBytes()];buffer.readBytes(data);return new LegacyPayload(VERSION,"screen",kind,data);}finally{buffer.release();}}
    private static CompoundTag decode(byte[] bytes){FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));try{CompoundTag tag=buffer.readNbt();return tag==null?new CompoundTag():tag;}finally{buffer.release();}}
    public static void sendInput(CompoundTag tag){PacketDistributor.sendToServer(packet(0,tag));}
    private static void send(ServerPlayer player,CompoundTag frame){var payload=packet(1,frame);var sender=SENDER.get();if(sender==null)PacketDistributor.sendToPlayer(player,payload);else sender.accept(player,payload);}
    public static void openNative(ServerPlayer player,NativeScreenRoutesB173.Route route){
        var session=SESSIONS.get(player.getUUID());if(session!=null)session.suspended=true;
        var frame=new CompoundTag();frame.putString("nativeScreen",route.name());send(player,frame);
    }
    private static final class GuiMinecraft extends legacyconnector.vb1_7_3.net.minecraft.client.Minecraft {}
    private static final class Session {
        final int id=NEXT.incrementAndGet();final ServerPlayer player;final Screen gui;final GuiMinecraft minecraft=new GuiMinecraft();
        final Map<Integer,String> paths=new HashMap<>();int nextTexture;int width=320,height=240;boolean initialized,suspended;int lastTick=Integer.MIN_VALUE;int[] metrics=new int[0];
        /** The menu whose screen this is (its slots stay native on the client), 0 for a screen of its own. */
        int menu;
        Session(ServerPlayer player,Screen gui){this.player=player;this.gui=gui;minecraft.world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(player.level());minecraft.currentScreen=gui;legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(minecraft,PlayerViewB173.of(player));}
    }
    public static void open(ServerPlayer player,Screen gui){
        install();Session previous=SESSIONS.remove(player.getUUID());if(previous!=null)GuiDrawB173.withSink(new Capture(previous),()->run(previous,()->previous.gui.removed()));
        if(gui==null){CompoundTag closed=new CompoundTag();closed.putBoolean("closed",true);closed.putInt("id",previous==null?0:previous.id);send(player,closed);return;}
        Session session=new Session(player,gui);SESSIONS.put(player.getUUID(),session);render(session,new CompoundTag(),false);
    }
    private static void run(Session session,Runnable callback){MenusB173.withActor(session.player,()->legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$scope(session.minecraft,()->{
        try(var views=EntityViewsB173.callbackScope()){legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(session.minecraft,PlayerViewB173.of(session.player));callback.run();((PlayerViewB173)legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(session.minecraft)).writeBack();}
    }));}
    private static void disconnect(ServerPlayer player){
        Session session=SESSIONS.remove(player.getUUID());if(session==null)return;
        try{GuiDrawB173.withSink(new Capture(session),()->run(session,()->session.gui.removed()));}
        catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/GuiSessions").warn("Original screen failed while disconnecting",error);}
        finally{SESSIONS.remove(player.getUUID());}
    }
    /**
     * A remote client that cannot make a menu's original screen (it reads data only the server has: a trader's prices) asks the server to run
     * it: the server draws it against the open menu and sends the frames; the client draws them under its native slots and sends input back.
     */
    private static void attach(ServerPlayer player,CompoundTag input){
        int menu=input.getInt("menu");
        if(!(player.containerMenu instanceof LegacyB173Menu open)||open.containerId!=menu||!(open.serverGui() instanceof Screen gui))return;
        var previous=SESSIONS.get(player.getUUID());if(previous!=null&&previous.menu==menu)return;
        Session session=new Session(player,gui);session.menu=menu;SESSIONS.put(player.getUUID(),session);render(session,input,true);
    }
    /** The menu closed: its screen session ends with it (the screen's own onGuiClosed runs once, here). */
    static void closeMenu(ServerPlayer player,int menu){
        var session=SESSIONS.get(player.getUUID());if(session==null||session.menu!=menu)return;
        SESSIONS.remove(player.getUUID());
        try{GuiDrawB173.withSink(new Capture(session),()->run(session,()->session.gui.removed()));}
        catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/GuiSessions").warn("Original menu screen failed while closing",error);}
    }
    private static void input(ServerPlayer player,CompoundTag input){
        if(input.getString("action").equals("attach")){attach(player,input);return;}
        Session session=SESSIONS.get(player.getUUID());if(session==null||session.id!=input.getInt("id"))return;
        String action=input.getString("action");
        if(session.suspended&&!action.equals("resize"))return;
        session.suspended=false;
        if(action.equals("close")){if(session.menu==0)open(player,null);return;}
        if(action.equals("tick")){if(session.lastTick==player.tickCount)return;session.lastTick=player.tickCount;}
        render(session,input,true);
    }
    private static void render(Session session,CompoundTag input,boolean hasInput){
        Capture capture=new Capture(session);int[] slotClick={-1,0,0,0};
        try{GuiDrawB173.withSink(capture,()->run(session,()->{
            int width=hasInput?Math.max(1,Math.min(8192,input.getInt("width"))):session.width,height=hasInput?Math.max(1,Math.min(8192,input.getInt("height"))):session.height;
            int[] metrics=input.getIntArray("metrics");if(metrics.length==256)session.metrics=metrics;
            if(!session.initialized||width!=session.width||height!=session.height){session.width=width;session.height=height;session.gui.init(session.minecraft,width,height);session.initialized=true;}
            int x=input.getInt("x"),y=input.getInt("y"),button=input.getInt("button");
            switch(input.getString("action")){
                case "press"->{
                    // a menu's screen: Beta's GuiContainer.mouseClicked clicked the slot under the mouse (windowClick); a screen that handles
                    // the click itself (a trader's) and never reaches it clicks none
                    if(session.menu!=0&&session.gui instanceof legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen handled){
                        if(handled.mcc$clickReachesSlots(x,y,button))slotUnder(handled,session.width,session.height,x,y,button,input.getBoolean("shift"),slotClick);
                    }else session.gui.mcc$mouseClicked(x,y,button);
                }
                case "release"->session.gui.mcc$mouseReleased(x,y,button);
                case "drag"->session.gui.mcc$mouseDragged(x,y,button);
                case "scroll"->session.gui.mcc$mouseScrolled(input.getDouble("scroll"));
                case "key"->{
                    if(input.getInt("character")==22)Screen.mcc$withClipboard(input.getString("clipboard"),()->session.gui.mcc$keyPressed((char)22,input.getInt("key")));
                    else session.gui.mcc$keyPressed((char)input.getInt("character"),input.getInt("key"));
                }
                case "tick"->session.gui.tick();
                default->{}
            }
            if(SESSIONS.get(session.player.getUUID())==session&&!MenusB173.hasPendingScreen(session.player)){if(session.gui instanceof legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen handled)handled.mcc$drewSlots=false;session.gui.render(x,y,0);}
        }));
            if(slotClick[3]!=0&&session.player.containerMenu instanceof LegacyB173Menu open&&open.containerId==session.menu){
                open.clicked(slotClick[0],slotClick[1],slotClick[2]!=0?net.minecraft.world.inventory.ClickType.QUICK_MOVE:net.minecraft.world.inventory.ClickType.PICKUP,session.player);
                open.broadcastChanges();
            }
            if(SESSIONS.get(session.player.getUUID())==session&&!session.suspended){CompoundTag frame=new CompoundTag();frame.putInt("id",session.id);if(session.menu!=0)frame.putInt("menu",session.menu);if(session.gui instanceof legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen handled&&!handled.mcc$drewSlots)frame.putBoolean("ownSlots",true);frame.put("commands",capture.commands);send(session.player,frame);}
        }catch(RuntimeException|LinkageError e){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/GuiSessions").error("Original server screen {} failed",session.gui.getClass().getName(),e);SESSIONS.remove(session.player.getUUID());CompoundTag frame=new CompoundTag();frame.putInt("id",session.id);frame.putBoolean("closed",true);send(session.player,frame);}
    }
    /** Beta's {@code getSlotAtPosition}: the slot under the mouse, or -999 outside the screen (drops the carried stack). */
    private static void slotUnder(legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen handled,int width,int height,int x,int y,
            int button,boolean shift,int[] out){
        int left=(width-handled.mcc$backgroundWidth())/2,top=(height-handled.mcc$backgroundHeight())/2;
        var slots=handled.container==null?java.util.List.<legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot>of():handled.container.slots;
        int found=-1;
        for(int i=0;i<slots.size();i++){var slot=slots.get(i);int sx=x-left,sy=y-top;
            if(sx>=slot.x-1&&sx<slot.x+17&&sy>=slot.y-1&&sy<slot.y+17){found=i;break;}}
        boolean outside=x<left||y<top||x>=left+handled.mcc$backgroundWidth()||y>=top+handled.mcc$backgroundHeight();
        if(found<0&&!outside)return;
        out[0]=found<0?-999:found;out[1]=button;out[2]=shift?1:0;out[3]=1;
    }
    private static final class Capture implements GuiDrawB173.Sink {
        final Session session;final ListTag commands=new ListTag();Capture(Session session){this.session=session;}
        CompoundTag op(String kind,int... values){if(commands.size()>=8192)throw new IllegalStateException("Legacy GUI command limit exceeded");CompoundTag tag=new CompoundTag();tag.putString("op",kind);tag.putIntArray("values",values);commands.add(tag);return tag;}
        public void blit(int x,int y,int u,int v,int width,int height){op("blit",x,y,u,v,width,height);}
        public void fill(int a,int b,int c,int d,int color){op("fill",a,b,c,d,color);}
        public void fillGradient(int a,int b,int c,int d,int top,int bottom){op("gradient",a,b,c,d,top,bottom);}
        public void text(String text,int x,int y,int color,boolean shadow){op("text",x,y,color,shadow?1:0).putString("text",text);}
        public int width(String text){int width=0;for(char c:text.toCharArray())width+=c<session.metrics.length?session.metrics[c]:6;return width;}
        public int textureId(String path){for(var e:session.paths.entrySet())if(e.getValue().equals(path))return e.getKey();int id=++session.nextTexture;session.paths.put(id,path);return id;}
        public int load(BufferedImage image){throw new UnsupportedOperationException("Server GUI dynamic bitmap transport is not implemented");}
        public void bind(int id){op("bind").putString("path",session.paths.getOrDefault(id,"/gui/gui.png"));}
        public void color(float r,float g,float b,float a){CompoundTag tag=op("color");tag.putFloat("r",r);tag.putFloat("g",g);tag.putFloat("b",b);tag.putFloat("a",a);}
        public void push(){op("push");}public void pop(){op("pop");}
        public void translate(float x,float y,float z){CompoundTag tag=op("translate");tag.putFloat("x",x);tag.putFloat("y",y);tag.putFloat("z",z);}
        public void scale(float x,float y,float z){CompoundTag tag=op("scale");tag.putFloat("x",x);tag.putFloat("y",y);tag.putFloat("z",z);}
        public void click(){op("click");}
        public void depthTest(boolean enabled){op("depth",enabled?1:0);}
        public void item(net.minecraft.world.item.ItemStack stack,int x,int y,boolean decorations){
            var saved=stack.saveOptional(session.player.registryAccess());
            op("item",x,y,decorations?1:0).put("stack",saved);
        }
    }
}
