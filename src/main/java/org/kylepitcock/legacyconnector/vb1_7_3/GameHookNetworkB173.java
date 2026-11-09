package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.core.network.*;

/** Optional per-connection presentation context; all simulation identity and equipment come from the server player. */
public final class GameHookNetworkB173 {
    public static final int HELLO=0,READY=1,ACTIVE=2,FRAME=3,STOP=4;
    private static final String FAMILY="b1.7.3-hooks";
    private static final int MAX_CONTEXT=4096,MAX_FRAME=65536;
    private static final Map<UUID,Integer> OFFERED=new HashMap<>();
    private static final Map<UUID,Integer> ACCEPTED=new HashMap<>();
    private static boolean installed;
    public interface Client {void ready(int session);void frame(int session,CompoundTag frame);}
    private static volatile Client client;
    private GameHookNetworkB173(){}
    public static void installClient(Client value){client=value;}
    public static synchronized void install(){
        if(installed)return;installed=true;ServerGameHooksB173.install();ServerGameHooksB173.installFrameSink(GameHookNetworkB173::frame);
        LegacyNetwork.registerReceiver(FAMILY,GameHookNetworkB173::receive);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player){OFFERED.remove(player.getUUID());ACCEPTED.remove(player.getUUID());ServerGameHooksB173.removeContext(player);}});
    }
    private static void receive(LegacyPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context){
        boolean inbound=context.flow()==net.minecraft.network.protocol.PacketFlow.CLIENTBOUND;int limit=inbound?MAX_FRAME:MAX_CONTEXT;
        if(payload.data().length>limit)return;var tag=decode(payload.data(),limit);if(tag==null)return;
        context.enqueueWork(()->{
            int session=tag.getInt("session");
            if(inbound){var target=client;if(target==null)return;if(payload.discriminator()==READY)target.ready(session);else if(payload.discriminator()==FRAME)target.frame(session,tag);return;}
            if(!(context.player() instanceof ServerPlayer player)||!player.getServer().isDedicatedServer())return;
            var id=player.getUUID();
            if(payload.discriminator()==STOP){if(Objects.equals(OFFERED.get(id),session)){OFFERED.remove(id);ACCEPTED.remove(id);ServerGameHooksB173.removeContext(player);}return;}
            if(!ContentB173.hasClientHooks()||player.connection==null||!player.connection.hasChannel(LegacyPayload.TYPE))return;
            if(payload.discriminator()==HELLO){
                if(!Objects.equals(OFFERED.put(id,session),session)){ACCEPTED.remove(id);ServerGameHooksB173.removeContext(player);}
                var ready=new CompoundTag();ready.putInt("session",session);send(player,READY,ready,MAX_CONTEXT);
            }else if(payload.discriminator()==ACTIVE&&Objects.equals(OFFERED.get(id),session)){
                var value=readContext(tag);ACCEPTED.put(id,session);ServerGameHooksB173.updateContext(player,value);
            }
        });
    }
    private static void frame(ServerPlayer player,CompoundTag frame){var session=ACCEPTED.get(player.getUUID());if(session==null)return;var tag=frame.copy();tag.putInt("session",session);send(player,FRAME,tag,MAX_FRAME);}
    private static void send(ServerPlayer player,int kind,CompoundTag tag,int limit){var packet=packet(kind,tag,limit);if(packet!=null)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,packet);}
    public static void sendContext(int kind,int session,ServerGameHooksB173.Context context){
        var tag=writeContext(context);tag.putInt("session",session);var packet=packet(kind,tag,MAX_CONTEXT);if(packet!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
    public static void stop(int session){var tag=new CompoundTag();tag.putInt("session",session);var packet=packet(STOP,tag,MAX_CONTEXT);if(packet!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);}
    static CompoundTag writeContext(ServerGameHooksB173.Context value){
        var tag=new CompoundTag();tag.putInt("width",value.width());tag.putInt("height",value.height());tag.putInt("guiWidth",value.guiWidth());tag.putInt("guiHeight",value.guiHeight());tag.putInt("scale",value.scale());
        tag.putBoolean("hideHud",value.hideHud());tag.putBoolean("thirdPerson",value.thirdPerson());tag.putBoolean("screen",value.screen());tag.putBoolean("paused",value.paused());tag.putIntArray("metrics",value.metrics());return tag;
    }
    static ServerGameHooksB173.Context readContext(CompoundTag tag){return new ServerGameHooksB173.Context(tag.getInt("width"),tag.getInt("height"),tag.getInt("guiWidth"),tag.getInt("guiHeight"),tag.getInt("scale"),tag.getBoolean("hideHud"),tag.getBoolean("thirdPerson"),tag.getBoolean("screen"),tag.getBoolean("paused"),tag.getIntArray("metrics"));}
    private static LegacyPayload packet(int kind,CompoundTag tag,int limit){var buf=new FriendlyByteBuf(Unpooled.buffer());try{buf.writeNbt(tag);if(buf.readableBytes()>limit)return null;byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return new LegacyPayload(FAMILY,"frame",kind,bytes);}finally{buf.release();}}
    private static CompoundTag decode(byte[] bytes,int limit){var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));try{Tag tag=buf.readNbt(NbtAccounter.create(limit*4L));return tag instanceof CompoundTag compound?compound:null;}catch(RuntimeException error){return null;}finally{buf.release();}}
}
