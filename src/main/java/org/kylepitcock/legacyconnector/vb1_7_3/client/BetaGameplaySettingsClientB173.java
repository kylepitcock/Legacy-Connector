package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.kylepitcock.legacyconnector.vb1_7_3.BetaGameplayConfigB173;
import org.kylepitcock.legacyconnector.vb1_7_3.BetaGameplaySettingsNetworkB173;

/** Food mode requests do not depend on rendered frames or registered game hooks. */
public final class BetaGameplaySettingsClientB173 {
    private static final BetaGameplaySettingsNetworkB173.ClientSession SESSION=new BetaGameplaySettingsNetworkB173.ClientSession();
    private static Boolean hostedFoods;
    private static boolean installed;
    private BetaGameplaySettingsClientB173(){}
    public static synchronized void install(){
        if(installed)return;installed=true;
        BetaGameplaySettingsNetworkB173.installClient((session,hunger)->{
            var mc=Minecraft.getInstance();
            if(mc.getSingleplayerServer()==null&&mc.level!=null&&mc.player!=null
                    &&SESSION.accept(session,mc.getConnection(),mc.level))BetaGameplayConfigB173.applyServerOverride(hunger);
        });
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->tick());
        bus.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)->reset());
    }
    private static void reset(){SESSION.clear();BetaGameplayConfigB173.clearServerOverride();}
    private static void tick(){
        var mc=Minecraft.getInstance();var connection=mc.getConnection();
        if(connection==null||mc.level==null||mc.player==null||mc.getSingleplayerServer()!=null
                ||!connection.hasChannel(LegacyPayload.TYPE)){reset();return;}
        if(hostedFoods==null)hostedFoods=BetaGameplaySettingsNetworkB173.hasHostedFoods();
        if(!hostedFoods){reset();return;}
        if(SESSION.bind(connection,mc.level))BetaGameplayConfigB173.clearServerOverride();
        if(SESSION.shouldRequest(System.nanoTime()))BetaGameplaySettingsNetworkB173.request(SESSION.id());
    }
}
