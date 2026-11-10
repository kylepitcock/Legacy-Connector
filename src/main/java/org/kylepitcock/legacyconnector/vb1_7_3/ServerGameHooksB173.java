package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.util.function.BiConsumer;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Dedicated callbacks use server actors and clocks; clients contribute bounded presentation state only. */
public final class ServerGameHooksB173 {
    public record Context(int width,int height,int guiWidth,int guiHeight,int scale,boolean hideHud,boolean thirdPerson,boolean screen,boolean paused,boolean focused,int[] metrics){
        public Context(int width,int height,int guiWidth,int guiHeight,int scale,boolean hideHud,boolean thirdPerson,boolean screen,boolean paused,int[] metrics){
            this(width,height,guiWidth,guiHeight,scale,hideHud,thirdPerson,screen,paused,false,metrics);
        }
        public Context{
            width=clamp(width,1,8192);height=clamp(height,1,8192);guiWidth=clamp(guiWidth,1,8192);guiHeight=clamp(guiHeight,1,8192);scale=clamp(scale,0,16);
            int[] copy=new int[256];for(int i=0;i<copy.length;i++)copy[i]=metrics!=null&&i<metrics.length?clamp(metrics[i],0,64):6;metrics=copy;
        }
        @Override public int[] metrics(){return metrics.clone();}
        private static int clamp(int value,int low,int high){return Math.max(low,Math.min(high,value));}
    }
    private static final class HookMinecraft extends Minecraft {}
    static final class Clock {
        private Object world;private long worldTick=Long.MIN_VALUE,serverTick=Long.MIN_VALUE;
        boolean enter(long serverTick){if(this.serverTick==serverTick)return false;this.serverTick=serverTick;return true;}
        boolean advanced(Object world,long tick){boolean changed=this.world!=world||worldTick!=tick;this.world=world;worldTick=tick;return changed;}
    }
    private static final class Session {
        Context context;final Clock clock=new Clock();final ContentB173.ActorHooks hooks=new ContentB173.ActorHooks();
        Session(Context context){this.context=context;}
    }
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static BiConsumer<ServerPlayer,CompoundTag> frameSink=(player,frame)->{};
    private static boolean installed;
    private ServerGameHooksB173(){}
    public static void installFrameSink(BiConsumer<ServerPlayer,CompoundTag> sink){frameSink=Objects.requireNonNull(sink);}
    public static void updateContext(ServerPlayer player,Context context){
        if(!player.server.isDedicatedServer())return;
        SESSIONS.compute(player.getUUID(),(id,old)->{if(old==null)return new Session(context);old.context=context;return old;});
    }
    public static void removeContext(ServerPlayer player){SESSIONS.remove(player.getUUID());HookActorStateB173.remove(player.getUUID());}
    public static synchronized void install(){
        if(installed)return;installed=true;
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event)->{
            var server=event.getServer();if(!server.isDedicatedServer()||!ContentB173.hasClientHooks())return;
            for(var level:server.getAllLevels())for(var player:List.copyOf(level.players())){
                Session session=SESSIONS.get(player.getUUID());if(session==null||!session.clock.enter(server.getTickCount()))continue;
                run(player,session);
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player)removeContext(player);});
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload event)->{if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)HookActorStateB173.clearLevel(level);});
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{SESSIONS.clear();HookActorStateB173.clear();});
    }
    private static void run(ServerPlayer nativePlayer,Session session){
        Context options=session.context;var minecraft=new HookMinecraft();minecraft.world=World.mcc$wrap(nativePlayer.level());
        minecraft.displayWidth=options.width();minecraft.displayHeight=options.height();minecraft.options.guiScale=options.scale();
        minecraft.options.hideHud=options.hideHud();minecraft.options.thirdPerson=options.thirdPerson();
        minecraft.currentScreen=options.screen()?new Screen():null;
        minecraft.focused=options.focused();
        // A dedicated world keeps ticking while a remote player opens a pause menu.
        minecraft.paused=false;boolean advanced=session.clock.advanced(nativePlayer.level(),nativePlayer.level().getGameTime());
        CompoundTag[] frame={null};
        try{
            MenusB173.withActor(nativePlayer,()->Minecraft.mcc$scope(minecraft,()->{
                try(var views=EntityViewsB173.callbackScope()){
                    for(var level:nativePlayer.server.getAllLevels())EntityViewsB173.refreshCached(level);
                    var player=(PlayerViewB173)EntityViewsB173.wrap(nativePlayer);Minecraft.mcc$set$player(minecraft,player);
                    try(var context=Minecraft.mcc$context(player.world,player)){
                        PlayerInventoryNetworkB173.capture(player,minecraft,options.screen(),()->frame[0]=GameHookHudB173.capture(options,()->{
                            try{ContentB173.serverHooks(minecraft,false,advanced,session.hooks);if(options.screen())ContentB173.serverHooks(minecraft,true,advanced,session.hooks);}
                            finally{player.writeBack();}
                        }));
                    }
                }finally{EntityViewsB173.flush();BlockEntityViewsB173.flush();}
            }));
        }catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Dedicated Beta actor frame failed",error);}
        // Presentation transport failure cannot unregister an original gameplay callback or roll back its writes.
        if(frame[0]!=null)try{frameSink.accept(nativePlayer,frame[0]);}
        catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Dedicated Beta actor frame was not delivered",error);}
    }
}
