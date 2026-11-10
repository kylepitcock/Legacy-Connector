package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.core.network.*;

/** Actor-owned original record text uses the native timed, tinted overlay without replaying audio. */
public final class HudOverlayB173 {
    private static final String FAMILY="b1.7.3-overlay";
    private static final int MAX_BYTES=16384;
    private static volatile Consumer<String> client;
    private HudOverlayB173(){}
    public static void installClient(Consumer<String> sink){client=sink;}
    public static void install(){LegacyNetwork.registerReceiver(FAMILY,(payload,context)->{
        if(!context.flow().isClientbound()||payload.discriminator()!=0)return;
        String text=decode(payload.data());if(text==null)return;
        context.enqueueWork(()->{var sink=client;if(sink!=null)sink.accept(text);});
    });}
    static String text(String name){return "Now playing: "+name;}
    static byte[] encode(String text){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);return bytes.length<=MAX_BYTES?bytes:null;}
    static String decode(byte[] bytes){return bytes.length<=MAX_BYTES?new String(bytes,StandardCharsets.UTF_8):null;}
    static void dispatch(Player actor,boolean clientSide,String text,Consumer<ServerPlayer> remote,Consumer<String> local){
        if(actor instanceof ServerPlayer server)remote.accept(server);
        else if(actor!=null&&clientSide)local.accept(text);
    }
    public static void record(String name){
        String text=text(name);byte[] bytes=encode(text);if(bytes==null)return;
        var actor=MenusB173.currentActor();
        var view=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        Player player=actor!=null?actor:view!=null&&view.mcc$modern instanceof Player p?p:null;
        dispatch(player,view!=null&&view.world!=null&&view.world.isRemote,text,
                server->{if(server.connection!=null&&server.connection.hasChannel(LegacyPayload.TYPE))
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new LegacyPayload(FAMILY,"record",0,bytes));},
                value->{var sink=client;if(sink!=null)sink.accept(value);});
    }
}
