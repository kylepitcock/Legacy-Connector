package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.ByteBuffer;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;

/** Optional food settings exchange, independent of ModLoader game hooks. */
public final class BetaGameplaySettingsNetworkB173 {
    private static final String FAMILY="b1.7.3-settings",CHANNEL="food";
    static final int HELLO=0,MODE=1;
    public interface Client { void mode(int session,boolean hunger); }
    private static volatile Client client;
    private static boolean installed;
    private BetaGameplaySettingsNetworkB173(){}

    public static boolean hasHostedFoods(){
        for(var item:ContentB173.items())
            if(item instanceof legacyconnector.vb1_7_3.net.minecraft.item.FoodItem&&item.mcc$modern instanceof LegacyB173Item)return true;
        return false;
    }
    public static void installClient(Client target){client=target;}
    public static synchronized void install(){
        if(installed)return;installed=true;
        LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{
            if(!CHANNEL.equals(payload.channel()))return;
            boolean inbound=context.flow()==PacketFlow.CLIENTBOUND;
            var message=decode(payload.discriminator(),payload.data());
            if(message==null||payload.discriminator()!=(inbound?MODE:HELLO))return;
            context.enqueueWork(()->{
                if(inbound){var target=client;if(target!=null)target.mode(message.session(),message.hunger());return;}
                if(!(context.player() instanceof ServerPlayer player)||player.connection==null
                        ||!player.connection.hasChannel(LegacyPayload.TYPE)||!hasHostedFoods())return;
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        packet(MODE,message.session(),BetaGameplayConfigB173.localFoodUsesHunger()));
            });
        });
    }
    public static void request(int session){net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet(HELLO,session,false));}
    private static LegacyPayload packet(int kind,int session,boolean hunger){return new LegacyPayload(FAMILY,CHANNEL,kind,encode(kind,session,hunger));}
    record Message(int session,boolean hunger){}
    static byte[] encode(int kind,int session,boolean hunger){
        if(kind!=HELLO&&kind!=MODE)throw new IllegalArgumentException("Unknown settings message");
        var bytes=ByteBuffer.allocate(kind==HELLO?4:5).putInt(session);
        if(kind==MODE)bytes.put((byte)(hunger?1:0));
        return bytes.array();
    }
    static Message decode(int kind,byte[] bytes){
        if(bytes==null||(kind!=HELLO&&kind!=MODE)||bytes.length!=(kind==HELLO?4:5))return null;
        var data=ByteBuffer.wrap(bytes);int session=data.getInt();
        if(kind==HELLO)return new Message(session,false);
        byte mode=data.get();return mode==0||mode==1?new Message(session,mode==1):null;
    }

    /** Identity-bound client state; no player identity or configuration value is sent in a request. */
    public static final class ClientSession {
        private Object connection,world;
        private int serial;
        private boolean received,requested;
        private long requestedAt;
        public boolean bind(Object connection,Object world){
            if(connection==null||world==null)throw new IllegalArgumentException("Missing remote session");
            if(this.connection==connection&&this.world==world)return false;
            clear();this.connection=connection;this.world=world;serial++;return true;
        }
        public void clear(){connection=null;world=null;received=false;requested=false;requestedAt=0;}
        public int id(){return serial;}
        public boolean shouldRequest(long nanos){
            if(connection==null||received||(requested&&nanos-requestedAt<1_000_000_000L))return false;
            requested=true;requestedAt=nanos;return true;
        }
        public boolean accept(int session,Object connection,Object world){
            if(this.connection==null||this.connection!=connection||this.world!=world||serial!=session)return false;
            received=true;return true;
        }
    }
}
