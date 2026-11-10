package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.util.function.Consumer;
import io.netty.buffer.Unpooled;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.core.network.*;

/** Registered original keys; callbacks and held-key repetition belong to the native server actor. */
public final class KeyBindingsB173 {
    public record Registration(String id,String title,BaseMod mod,KeyBinding key,boolean repeat) {}
    private static final Map<String,Registration> KEYS=new LinkedHashMap<>();
    private static final Map<UUID,Map<String,Held>> HELD=new HashMap<>();
    private static final ThreadLocal<Map<String,Held>> ACTOR=new ThreadLocal<>();
    private static Consumer<Registration> client=registration->{};
    private static boolean installed;
    private static final String FAMILY="b1.7.3-keys";
    private static final class KeyMinecraft extends Minecraft {}
    private static final class Held {boolean down,screen;long inputTick,worldTick=Long.MIN_VALUE;Registration definition;final Edge edge=new Edge();}
    /** Original ModLoader repeats every advanced world tick, rather than at the operating system repeat rate. */
    public static final class Edge {
        private boolean previous;
        public boolean sample(boolean down,boolean repeat){boolean fire=down&&(!previous||repeat);previous=down;return fire;}
        public void reset(){previous=false;}
    }
    private KeyBindingsB173(){}
    public static synchronized void register(BaseMod mod,KeyBinding key,boolean repeat){
        Objects.requireNonNull(mod);Objects.requireNonNull(key);
        Registration old=KEYS.values().stream().filter(value->value.mod()==mod&&value.key()==key).findFirst().orElse(null);
        var owner=ContentB173.current();String title=owner==null?mod.getClass().getSimpleName():owner.name();
        String stem=mod.getClass().getName()+"/"+key.translationKey;
        String id=old==null?stem:old.id();
        for(int suffix=2;old==null&&KEYS.containsKey(id);suffix++)id=stem+"/"+suffix;
        if(id.length()>512)throw new IllegalArgumentException("Original key identity exceeds 512 characters");
        var registration=new Registration(id,title,mod,key,repeat);KEYS.put(id,registration);client.accept(registration);
    }
    public static synchronized List<Registration> registered(){return List.copyOf(KEYS.values());}
    public static synchronized void installClient(Consumer<Registration> sink){client=Objects.requireNonNull(sink);for(var key:KEYS.values())sink.accept(key);}
    public static Boolean actorPressed(KeyBinding key){
        var states=ACTOR.get();
        if(states==null){var actor=Minecraft.mcc$currentPlayerContext();if(actor!=null&&actor.mcc$modern instanceof ServerPlayer player)states=HELD.get(player.getUUID());}
        if(states==null)return null;
        for(var registration:registered())if(registration.key()==key){var state=states.get(registration.id());return state!=null&&state.down;}
        return null;
    }
    public static synchronized void install(){
        if(installed)return;installed=true;
        LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{
            if(context.flow()!=net.minecraft.network.protocol.PacketFlow.SERVERBOUND||payload.data().length>1024)return;
            var input=new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            try{
                String id=input.readUtf(512);boolean down=input.readBoolean(),screen=input.readBoolean();
                if(input.isReadable())return;
                context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player)input(player,id,down,screen);});
            }catch(RuntimeException ignored){}finally{input.release();}
        });
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{
            for(var entry:List.copyOf(HELD.entrySet())){
                var player=event.getServer().getPlayerList().getPlayer(entry.getKey());if(player==null){HELD.remove(entry.getKey());continue;}
                for(var registration:registered()){
                    var state=entry.getValue().get(registration.id());if(state==null)continue;
                    if(state.definition!=registration){state.definition=registration;state.edge.reset();state.worldTick=Long.MIN_VALUE;}
                    if(event.getServer().getTickCount()-state.inputTick>40)state.down=false;
                    long tick=player.level().getGameTime();if(state.worldTick==tick)continue;state.worldTick=tick;
                    if(state.edge.sample(state.down,registration.repeat()))invoke(player,registration,state.screen,entry.getValue());
                }
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->HELD.remove(event.getEntity().getUUID()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->HELD.clear());
    }
    /** Only pre-registered identifiers can select a callback; identity is always the receiving connection's player. */
    public static void input(ServerPlayer player,String id,boolean down,boolean screen){
        synchronized(KeyBindingsB173.class){if(!KEYS.containsKey(id))return;}
        var state=HELD.computeIfAbsent(player.getUUID(),ignored->new HashMap<>()).computeIfAbsent(id,ignored->new Held());
        state.down=down;state.screen=screen;state.inputTick=player.server.getTickCount();
    }
    public static LegacyPayload packet(String id,boolean down,boolean screen){
        var output=new FriendlyByteBuf(Unpooled.buffer());
        try{output.writeUtf(id,512);output.writeBoolean(down);output.writeBoolean(screen);byte[] bytes=new byte[output.readableBytes()];output.readBytes(bytes);return new LegacyPayload(FAMILY,"state",0,bytes);}
        finally{output.release();}
    }
    private static void invoke(ServerPlayer player,Registration registration,boolean screen,Map<String,Held> states){
        var minecraft=new KeyMinecraft();minecraft.world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(player.level());
        try{
            MenusB173.withActor(player,()->Minecraft.mcc$scope(minecraft,()->{
                try(var views=EntityViewsB173.callbackScope()){
                    var view=(PlayerViewB173)EntityViewsB173.wrap(player);Minecraft.mcc$set$player(minecraft,view);
                    var session=GuiSessionsB173.currentScreen(player);
                    if(session!=null)minecraft.currentScreen=session;
                    else if(player.containerMenu instanceof LegacyB173Menu menu&&menu.gui() instanceof legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen gui)minecraft.currentScreen=gui;
                    else if(screen)minecraft.currentScreen=new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen();
                    var previous=ACTOR.get();ACTOR.set(states);
                    try(var context=Minecraft.mcc$context(view.world,view)){
                        org.kylepitcock.legacyconnector.core.LegacyModLocks.serverSide(
                                org.kylepitcock.legacyconnector.core.LegacyModLocks.of(registration.mod()),
                                ()->{registration.mod().KeyboardEvent(registration.key());view.writeBack();});
                    }
                    finally{if(previous==null)ACTOR.remove();else ACTOR.set(previous);}
                }
            }));
        }catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Keys").warn("Original key callback {} failed",registration.id(),error);}
    }
}
