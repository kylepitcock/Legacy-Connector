package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;

/** Runs integrated-client dimension requests on their authoritative server player. */
public final class DimensionClientB173 {
    private static boolean presentationInstalled;
    private static net.minecraft.client.player.LocalPlayer presentationPlayer;
    private static net.minecraft.client.multiplayer.ClientLevel presentationLevel;
    private static int contactTick=Integer.MIN_VALUE,cooldown;
    private static float exposure;
    private DimensionClientB173() {}

    /** Presentation only: an original portal block's local-player collision never creates a native portal process. */
    public static void portalContact(net.minecraft.world.entity.Entity entity,int target) {
        var minecraft=Minecraft.getInstance();
        if(target==0||entity!=minecraft.player||minecraft.player==null||minecraft.level==null||DimensionsB173.levelKey(target)==null)return;
        var realm=minecraft.level.dimension();
        if(DimensionsB173.registeredId(minecraft.level)==null&&!realm.equals(net.minecraft.world.level.Level.OVERWORLD)
                &&!realm.equals(net.minecraft.world.level.Level.NETHER))return;
        if(!presentationInstalled){
            presentationInstalled=true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    (net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->tickPresentation());
        }
        if(presentationPlayer!=minecraft.player||presentationLevel!=minecraft.level)resetPresentation(minecraft);
        if(cooldown>0){cooldown=10;return;}
        contactTick=minecraft.player.tickCount;
    }

    private static void resetPresentation(Minecraft minecraft){
        boolean sameActor=presentationPlayer!=null&&minecraft.player!=null
                &&presentationPlayer.getUUID().equals(minecraft.player.getUUID());
        int transferCooldown=sameActor?cooldown:0;
        presentationPlayer=minecraft.player;presentationLevel=minecraft.level;
        contactTick=Integer.MIN_VALUE;cooldown=transferCooldown;exposure=0;
    }

    private static void tickPresentation(){
        var minecraft=Minecraft.getInstance();
        if(presentationPlayer!=minecraft.player||presentationLevel!=minecraft.level)resetPresentation(minecraft);
        var player=minecraft.player;if(player==null||minecraft.level==null||minecraft.isPaused())return;
        // Native portals and nausea own these same fields. Leave their state and their sounds entirely native.
        if(player.getActivePortalLocalTransition()!=net.minecraft.world.level.block.Portal.Transition.NONE
                ||player.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)){
            exposure=0;contactTick=Integer.MIN_VALUE;return;
        }
        if(contactTick==player.tickCount&&cooldown==0){
            exposure=Math.min(1,exposure+.0125F);
            // Native aiStep has already saved the previous intensity and applied normal decay.
            // Raise only this frame's intensity; retain any stronger effect another renderer supplied.
            player.spinningEffectIntensity=Math.max(player.spinningEffectIntensity,exposure);
            if(exposure>=1){exposure=0;cooldown=10;}
        }else exposure=Math.max(0,exposure-.05F);
        contactTick=Integer.MIN_VALUE;
        if(cooldown>0)cooldown--;
    }

    public static void usePortal(int dimension) {
        onServer(player -> DimensionsB173.usePortal(player, dimension));
    }

    public static void respawn(boolean force, int dimension) {
        onServer(player -> DimensionsB173.respawn(player, force, dimension));
    }

    private static void onServer(Consumer<ServerPlayer> action) {
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        var player = minecraft.player;
        if (server == null || player == null) return;
        var uuid = player.getUUID();
        server.execute(() -> {
            var authoritative = server.getPlayerList().getPlayer(uuid);
            if (authoritative != null) action.accept(authoritative);
        });
    }
}
