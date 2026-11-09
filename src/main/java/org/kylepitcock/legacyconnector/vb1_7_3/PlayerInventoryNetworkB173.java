package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import io.netty.buffer.Unpooled;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.core.network.*;

/** Player-owned inventory requests and server snapshots, independent of HUD rendering. */
public final class PlayerInventoryNetworkB173 {
    private static final String FAMILY="b1.7.3-player-inventory";
    private static final int CONTEXT=0,CLICK=1,OFFER=2,STATE=3,TRANSACTION=4,MAX_IN=4096,MAX_OUT=131072,MAX_OFFERS=16;
    public record Context(long epoch,long token,int kind) {}
    private static final Context NONE=new Context(0,0,0);
    private static final class Session {
        final ServerPlayer player;
        Context context=NONE;
        final Map<String,HandledScreen> offers=new LinkedHashMap<>();
        CompoundTag snapshot;
        long revision,lastRequest;
        boolean snapshotError,snapshotValid;
        Session(ServerPlayer player){this.player=player;}
    }
    private record Discovery(PlayerViewB173 view,Session session) {}
    public interface Client {
        void offer(long epoch,long token,String identity,String title,int[] slots);
        void state(long epoch,long revision,CompoundTag snapshot);
        void transaction(long epoch,long token,long request,boolean accepted,long revision,int stateId,CompoundTag snapshot,CompoundTag carried);
    }
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final ThreadLocal<Discovery> DISCOVERY=new ThreadLocal<>();
    private static volatile Client client;
    private static boolean installed;
    private PlayerInventoryNetworkB173(){}

    public static void installClient(Client value){client=value;}
    public static synchronized void install(){
        if(installed)return;installed=true;
        LegacyNetwork.registerReceiver(FAMILY,PlayerInventoryNetworkB173::receive);
        PlayerApiLifecycleB173.installSnapshotSink(PlayerInventoryNetworkB173::synchronize);
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->SESSIONS.remove(event.getEntity().getUUID()));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->SESSIONS.clear());
    }
    private static Session session(ServerPlayer player){
        return SESSIONS.compute(player.getUUID(),(id,old)->old!=null&&old.player==player?old:new Session(player));
    }
    public static Context context(ServerPlayer player){var s=SESSIONS.get(player.getUUID());return s!=null&&s.player==player?s.context:NONE;}

    /** A typed callback view discovers original replacement requests without replacing the native inventory. */
    public static void capture(PlayerViewB173 player,Minecraft minecraft,boolean screen,Runnable action){
        Session session=player.mcc$modern instanceof ServerPlayer nativePlayer?SESSIONS.get(nativePlayer.getUUID()):null;
        Context context=session==null||session.player!=player.mcc$modern?NONE:session.context;
        Screen previous=minecraft.currentScreen;
        Discovery prior=DISCOVERY.get();
        boolean active=screen&&context.epoch()>0&&context.token()>0&&(context.kind()==1||context.kind()==2);
        if(active){minecraft.currentScreen=new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.InventoryScreen(player);DISCOVERY.set(new Discovery(player,session));}
        else DISCOVERY.remove();
        try{action.run();}
        finally{minecraft.currentScreen=previous;if(prior==null)DISCOVERY.remove();else DISCOVERY.set(prior);}
    }
    public static boolean offer(Screen screen){var d=DISCOVERY.get();return d!=null&&offer(d.view(),screen);}
    public static boolean offer(PlayerEntity actor,Screen screen){
        Discovery d=DISCOVERY.get();
        if(d==null||actor!=d.view()||!PlayerMenuContextB173.offer(actor,screen))return false;
        var gui=(HandledScreen)screen;String id=gui.getClass().getName();
        int[] slots=PlayerInventoryPanelB173.slots(d.view());
        if(slots.length==0)return true;
        if(id.length()>512||!d.session().offers.containsKey(id)&&d.session().offers.size()>=MAX_OFFERS)return true;
        boolean fresh=!d.session().offers.containsKey(id);
        d.session().offers.put(id,gui);
        if(fresh){
            String title="Legacy inventory";
            try{var bindings=PlayerInventoryStateB173.bindings(PlayerApiB173.bases(d.view()),actor.inventory);if(bindings.size()==1){String name=bindings.getFirst().inventory().getName();if(name!=null&&!name.isBlank())title=ContentB173.translate(name);}}
            catch(RuntimeException|LinkageError ignored){}
            var tag=new CompoundTag();tag.putLong("epoch",d.session().context.epoch());tag.putLong("token",d.session().context.token());tag.putString("identity",id);tag.putString("title",title.length()>80?title.substring(0,80):title);
            tag.putIntArray("slots",slots);
            send(d.session().player,OFFER,tag);
        }
        return true;
    }
    public static void synchronize(ServerPlayer player){
        if(!PlayerApiB173.hasRegistered())return;
        Session session=SESSIONS.get(player.getUUID());
        if(session==null||session.player!=player||session.context.epoch()<=0)return;
        if(!updateSnapshot(session))return;
        var tag=new CompoundTag();tag.putLong("epoch",session.context.epoch());tag.putLong("revision",session.revision);tag.put("snapshot",session.snapshot);
        send(player,STATE,tag);
    }
    private static boolean updateSnapshot(Session session){
        CompoundTag snapshot;
        try{snapshot=PlayerInventoryStateB173.capture(PlayerViewB173.of(session.player));}
        catch(RuntimeException|LinkageError error){
            session.snapshotValid=false;
            if(!session.snapshotError){session.snapshotError=true;org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("Cannot synchronize PlayerAPI inventory for {}",session.player.getUUID(),error);}
            return false;
        }
        session.snapshotValid=true;
        if(snapshot.equals(session.snapshot))return false;
        session.snapshot=snapshot.copy();session.revision++;
        return true;
    }
    private static void receive(LegacyPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context){
        boolean inbound=context.flow()==net.minecraft.network.protocol.PacketFlow.CLIENTBOUND;
        int limit=inbound?MAX_OUT:MAX_IN;if(payload.data().length>limit)return;
        var tag=decode(payload.data(),limit);if(tag==null)return;
        context.enqueueWork(()->{
            long epoch=tag.getLong("epoch");if(epoch<=0)return;
            if(inbound){
                Client receiver=client;if(receiver==null)return;
                if(payload.discriminator()==OFFER){
                    int[] slots=tag.getIntArray("slots");if(slots.length>PlayerInventoryStateB173.MAX_SLOTS)return;
                    receiver.offer(epoch,tag.getLong("token"),tag.getString("identity"),tag.getString("title"),slots);
                }else if(payload.discriminator()==STATE)receiver.state(epoch,tag.getLong("revision"),tag.getCompound("snapshot"));
                else if(payload.discriminator()==TRANSACTION)receiver.transaction(epoch,tag.getLong("token"),tag.getLong("request"),tag.getBoolean("accepted"),tag.getLong("revision"),tag.getInt("stateId"),tag.getCompound("snapshot"),tag.contains("carried",Tag.TAG_COMPOUND)?tag.getCompound("carried"):null);
                return;
            }
            if(!(context.player() instanceof ServerPlayer player)||!PlayerApiB173.hasRegistered()||player.connection==null||!player.connection.hasChannel(LegacyPayload.TYPE))return;
            Session session=session(player);
            long token=tag.getLong("token");
            if(payload.discriminator()==CONTEXT){
                int kind=tag.getInt("kind");if(kind<0||kind>2||token<0||kind!=0&&token==0)return;
                Context next=new Context(epoch,kind==0?0:token,kind);
                if(!next.equals(session.context)){session.offers.clear();if(epoch!=session.context.epoch()){session.snapshot=null;session.lastRequest=0;}session.context=next;}
                synchronize(player);
            }else if(payload.discriminator()==CLICK&&epoch==session.context.epoch()&&token==session.context.token()&&session.context.kind()!=0){
                click(session,tag);
            }
        });
    }
    private static void click(Session session,CompoundTag tag){
        long request=tag.getLong("request");if(request<=0)return;
        ServerPlayer player=session.player;
        boolean accepted=false;
        try{
            // Refresh before validating: callbacks may have changed the equipment since the last client snapshot.
            updateSnapshot(session);
            int slot=tag.getInt("slot"),button=tag.getInt("button");
            HandledScreen gui=session.offers.get(tag.getString("identity"));
            PlayerViewB173 view=PlayerViewB173.of(player);
            boolean current=session.snapshotValid&&request>session.lastRequest&&tag.getLong("revision")==session.revision
                &&tag.getInt("stateId")==player.inventoryMenu.getStateId()&&player.containerMenu==player.inventoryMenu
                &&player.isAlive()&&!player.isSpectator()&&gui!=null&&gui.container==view.playerScreenHandler
                &&(button==0||button==1)&&Arrays.stream(PlayerInventoryPanelB173.slots(view)).anyMatch(index->index==slot);
            session.lastRequest=Math.max(session.lastRequest,request);
            if(current&&transactionFits(session,view,slot)){
                PlayerApiB173.withPlayer(view,()->PlayerInventoryPanelB173.click(view,slot,button,tag.getBoolean("quick")));
                view.writeBack();accepted=true;
            }
        }catch(RuntimeException|LinkageError error){
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("PlayerAPI equipment transaction failed for {}",player.getUUID(),error);
        }finally{
            updateSnapshot(session);
            // Full contents also correct creative's cursor; its screen ignores ordinary cursor-only packets.
            player.inventoryMenu.broadcastChanges();player.inventoryMenu.sendAllDataToRemote();
            if(session.snapshot!=null){
                var reply=new CompoundTag();reply.putLong("epoch",session.context.epoch());reply.putLong("token",session.context.token());
                reply.putLong("request",request);reply.putBoolean("accepted",accepted);reply.putLong("revision",session.revision);
                reply.putInt("stateId",player.inventoryMenu.getStateId());reply.put("snapshot",session.snapshot);
                reply.put("carried",player.inventoryMenu.getCarried().saveOptional(player.registryAccess()));
                if(!send(player,TRANSACTION,reply)){
                    // The native full contents packet already carries this stack; keep the bounded acknowledgement deliverable.
                    reply.remove("carried");send(player,TRANSACTION,reply);
                }
            }
        }
    }
    private static boolean transactionFits(Session session,PlayerViewB173 view,int slot){
        if(session.snapshot==null)return false;
        var player=session.player;
        var carried=player.inventoryMenu.getCarried().saveOptional(player.registryAccess());
        var selected=StacksB173.toModern(view.playerScreenHandler.slots.get(slot).getStack()).saveOptional(player.registryAccess());
        // Leave room for a right-click split (two component-bearing copies) before invoking arbitrary original callbacks.
        return encodedSize(session.snapshot)+2L*encodedSize(carried)+2L*encodedSize(selected)<PlayerInventoryStateB173.MAX_BYTES-1024;
    }
    private static int encodedSize(Tag tag){
        if(!(tag instanceof CompoundTag compound))return MAX_OUT;
        try{var bytes=new java.io.ByteArrayOutputStream();try(var output=new java.io.DataOutputStream(bytes)){NbtIo.write(compound,output);}return bytes.size();}
        catch(java.io.IOException|RuntimeException error){return MAX_OUT;}
    }
    public static void sendContext(long epoch,long token,int kind){var tag=new CompoundTag();tag.putLong("epoch",epoch);tag.putLong("token",token);tag.putInt("kind",kind);sendToServer(CONTEXT,tag);}
    public static void requestClick(long epoch,long token,String identity,int slot,int button,boolean quick,long request,long revision,int stateId){
        if(identity==null||identity.length()>512||request<=0||slot<0||slot>=4096||button<0||button>1)return;
        var tag=new CompoundTag();tag.putLong("epoch",epoch);tag.putLong("token",token);tag.putString("identity",identity);
        tag.putInt("slot",slot);tag.putInt("button",button);tag.putBoolean("quick",quick);tag.putLong("request",request);
        tag.putLong("revision",revision);tag.putInt("stateId",stateId);sendToServer(CLICK,tag);
    }
    private static void sendToServer(int kind,CompoundTag tag){var packet=packet(kind,tag,MAX_IN);if(packet!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);}
    private static boolean send(ServerPlayer player,int kind,CompoundTag tag){var packet=packet(kind,tag,MAX_OUT);if(packet==null)return false;net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,packet);return true;}
    private static LegacyPayload packet(int kind,CompoundTag tag,int limit){var buffer=new FriendlyByteBuf(Unpooled.buffer());try{buffer.writeNbt(tag);if(buffer.readableBytes()>limit)return null;byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return new LegacyPayload(FAMILY,"inventory",kind,bytes);}finally{buffer.release();}}
    private static CompoundTag decode(byte[] bytes,int limit){var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));try{Tag tag=buffer.readNbt(NbtAccounter.create(limit*4L));return tag instanceof CompoundTag compound?compound:null;}catch(RuntimeException error){return null;}finally{buffer.release();}}
}
