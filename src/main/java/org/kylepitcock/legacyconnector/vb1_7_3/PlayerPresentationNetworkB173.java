package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.core.network.*;

/** Read-only presentation snapshots for native player trackers, separate from owner inventory transactions. */
public final class PlayerPresentationNetworkB173 {
    private static final String FAMILY="b1.7.3-player-presentation";
    public static final int MAX_BYTES=196608;
    private record Committed(ServerPlayer player,long revision,CompoundTag state,String dimension,int entity){}
    private record Watch(int entity,String dimension){}
    private static final Map<UUID,Committed> COMMITTED=new HashMap<>();
    private static final Map<UUID,Map<UUID,Watch>> TRACKERS=new HashMap<>();
    private static final Map<UUID,Integer> OWNER_REQUESTS=new HashMap<>();
    private static final Set<String> REPORTED=new HashSet<>();
    public interface Client {void receive(net.minecraft.network.Connection connection,CompoundTag tag);}
    private static volatile Client client;
    private static boolean installed;
    private PlayerPresentationNetworkB173(){}
    public static void guard(String phase,Runnable action){
        try{action.run();}catch(RuntimeException|LinkageError error){report(phase,error);}
    }
    public static synchronized void report(String phase,Throwable error){
        String key=phase+"#"+error.getClass().getName();
        if(REPORTED.size()<32&&REPORTED.add(key))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Original player presentation {} failed; native and owner inventory behavior retained",phase,error);
    }
    private static synchronized void clearReports(){REPORTED.clear();}
    public static void installClient(Client value){client=value;}
    public static synchronized void install(){
        if(installed)return;installed=true;
        PlayerApiLifecycleB173.installPresentationSink(PlayerPresentationNetworkB173::commit);
        LegacyNetwork.registerReceiver(FAMILY,(payload,context)->guard("receive",()->{
            if(!context.flow().isClientbound()){
                if(payload.discriminator()!=1||payload.data().length!=0)return;
                context.enqueueWork(()->guard("owner-ready",()->{
                    if(!(context.player() instanceof ServerPlayer player))return;
                    int now=player.server.getTickCount();var prior=OWNER_REQUESTS.get(player.getUUID());
                    if(prior!=null&&now-prior<20)return;
                    OWNER_REQUESTS.put(player.getUUID(),now);
                    var saved=COMMITTED.get(player.getUUID());
                    if(saved!=null&&saved.player()==player&&saved.entity()==player.getId()
                            &&saved.dimension().equals(player.level().dimension().location().toString()))send(player,envelope(saved,false));
                }));
                return;
            }
            if(!context.flow().isClientbound()||payload.discriminator()!=0)return;
            var tag=decode(payload.data());if(tag==null)return;var connection=context.connection();
            context.enqueueWork(()->guard("client-receive",()->{var sink=client;if(sink!=null)sink.receive(connection,tag);}));
        }));
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event)->guard("start-tracking",()->{
            if(!(event.getEntity() instanceof ServerPlayer tracker)||!(event.getTarget() instanceof ServerPlayer subject)||tracker==subject)return;
            TRACKERS.computeIfAbsent(subject.getUUID(),key->new HashMap<>()).put(tracker.getUUID(),new Watch(subject.getId(),subject.level().dimension().location().toString()));
            var saved=COMMITTED.get(subject.getUUID());
            if(saved!=null&&saved.player()==subject&&saved.dimension().equals(subject.level().dimension().location().toString()))send(tracker,envelope(saved,false));
        }));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StopTracking event)->guard("stop-tracking",()->{
            if(!(event.getEntity() instanceof ServerPlayer tracker)||!(event.getTarget() instanceof ServerPlayer subject))return;
            var set=TRACKERS.get(subject.getUUID());var watch=set==null?null:set.remove(tracker.getUUID());if(set!=null&&set.isEmpty())TRACKERS.remove(subject.getUUID());
            var saved=COMMITTED.get(subject.getUUID());if(watch!=null)send(tracker,removal(subject.getUUID(),watch,saved==null?1:saved.revision()));
        }));
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->guard("logout",()->{
            UUID id=event.getEntity().getUUID();var saved=COMMITTED.remove(id);var watchers=TRACKERS.remove(id);
            OWNER_REQUESTS.remove(id);
            if(event.getEntity() instanceof ServerPlayer subject&&watchers!=null)for(var entry:watchers.entrySet()){
                var tracker=subject.server.getPlayerList().getPlayer(entry.getKey());if(tracker!=null)send(tracker,removal(id,entry.getValue(),saved==null?1:saved.revision()));
            }
            for(var set:TRACKERS.values())set.remove(id);
        }));
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{COMMITTED.clear();TRACKERS.clear();OWNER_REQUESTS.clear();clearReports();});
    }
    private static void commit(ServerPlayer subject,PlayerViewB173 view){
        try{
            if(view==null||subject==null||view.mcc$modern!=subject)return;
            CompoundTag state=PlayerPresentationStateB173.capture(view);var old=COMMITTED.get(subject.getUUID());
            String dimension=subject.level().dimension().location().toString();int entity=subject.getId();
            if(old!=null&&old.player()==subject&&old.entity()==entity&&old.dimension().equals(dimension)&&old.state().equals(state))return;
            var saved=new Committed(subject,old==null?1:old.revision()+1,state.copy(),dimension,entity);
            var tag=envelope(saved,false);if(encode(tag)==null)throw new IllegalStateException("Original player presentation exceeds payload limit");
            COMMITTED.put(subject.getUUID(),saved);
            // Owner scalars need authoritative initial hydration too; owner inventory remains on its transaction protocol.
            send(subject,tag);
            // Native Start/StopTracking membership also lets optional, unmodified connections remain untouched.
            for(var entry:TRACKERS.getOrDefault(subject.getUUID(),Map.of()).entrySet()){
                var watch=entry.getValue();if(watch.entity()!=entity||!watch.dimension().equals(dimension))continue;
                var tracker=subject.server.getPlayerList().getPlayer(entry.getKey());if(tracker!=null&&tracker!=subject)send(tracker,tag);
            }
        }catch(RuntimeException|LinkageError error){report("publish",error);}
    }
    public static void requestOwner(){
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new LegacyPayload(FAMILY,"owner-ready",1,new byte[0]));
    }
    private static CompoundTag envelope(Committed saved,boolean remove){
        var tag=new CompoundTag();tag.putUUID("subject",saved.player().getUUID());tag.putInt("entity",saved.entity());
        tag.putString("dimension",saved.dimension());tag.putLong("revision",saved.revision());tag.putBoolean("remove",remove);
        if(!remove)tag.put("state",saved.state().copy());return tag;
    }
    private static CompoundTag removal(UUID subject,Watch watch,long revision){
        var tag=new CompoundTag();tag.putUUID("subject",subject);tag.putInt("entity",watch.entity());tag.putString("dimension",watch.dimension());
        tag.putLong("revision",revision);tag.putBoolean("remove",true);return tag;
    }
    public static PlayerPresentationStateB173.Identity identity(CompoundTag tag){
        if(!tag.hasUUID("subject")||!tag.contains("entity",Tag.TAG_INT)||tag.getLong("revision")<=0)return null;
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("dimension"));if(dimension==null)return null;
        if(!tag.getBoolean("remove")&&!tag.contains("state",Tag.TAG_COMPOUND))return null;
        return new PlayerPresentationStateB173.Identity(tag.getUUID("subject"),tag.getInt("entity"),dimension,tag.getLong("revision"));
    }
    private static void send(ServerPlayer tracker,CompoundTag tag){
        guard("send",()->{
            if(tracker.connection==null||!tracker.connection.hasChannel(LegacyPayload.TYPE))return;
            byte[] bytes=encode(tag);if(bytes!=null)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(tracker,new LegacyPayload(FAMILY,"subject",0,bytes));
        });
    }
    static byte[] encode(CompoundTag tag){
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{buf.writeNbt(tag);if(buf.readableBytes()>MAX_BYTES)return null;
            byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return bytes;
        }finally{buf.release();}
    }
    static CompoundTag decode(byte[] bytes){
        if(bytes.length>MAX_BYTES)return null;var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try{var tag=buf.readNbt(NbtAccounter.create(MAX_BYTES*4L));return tag instanceof CompoundTag compound&&identity(compound)!=null?compound:null;}
        catch(RuntimeException malformed){return null;}finally{buf.release();}
    }
}
