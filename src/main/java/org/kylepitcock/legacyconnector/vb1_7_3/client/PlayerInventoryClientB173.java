package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerInventoryNetworkB173;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerInventoryStateB173;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173;

/** Server-owned accessory transactions beside the unchanged native inventory. */
final class PlayerInventoryClientB173 {
    private static Object connection, level;
    private static net.minecraft.world.entity.player.Player previousPlayer;
    private static long token=-1,epoch,revision=-1,nextRequest;
    private record Pending(long epoch,long token,long request) {}
    private static Pending pending;
    private static int kind,heartbeat;
    private PlayerInventoryClientB173() {}

    static void install() {
        InventoryPanelB173.install();
        PlayerInventoryNetworkB173.installClient(new PlayerInventoryNetworkB173.Client() {
            @Override public void offer(long incomingEpoch,long incomingToken,String identity,String title,int[] slots,int[][] layout) {
                if(incomingEpoch!=epoch||!currentLifetime())return;
                InventoryPanelB173.offer(incomingToken,identity,title,slots,layout);
            }
            @Override public void state(long incomingEpoch,long incomingRevision,CompoundTag snapshot) {
                apply(incomingEpoch,incomingRevision,snapshot,null,0,0,-1);
            }
            @Override public void transaction(long incomingEpoch,long incomingToken,long request,boolean accepted,
                    long incomingRevision,int stateId,CompoundTag snapshot,CompoundTag carried) {
                apply(incomingEpoch,incomingRevision,snapshot,carried,incomingToken,request,stateId);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->tick());
    }

    private static void apply(long incomingEpoch,long incomingRevision,CompoundTag snapshot,CompoundTag carried,long incomingToken,long request,int stateId) {
        if(incomingEpoch!=epoch||!currentLifetime())return;
        Minecraft mc=Minecraft.getInstance();
        Object expectedConnection=mc.getConnection(),expectedLevel=mc.level;
        var expectedPlayer=mc.player;
        CompoundTag copy=snapshot.copy(),carry=carried==null?null:carried.copy();
        mc.execute(()->{
            if(incomingEpoch!=epoch||!currentLifetime()||expectedConnection!=mc.getConnection()
                    ||expectedLevel!=mc.level||expectedPlayer!=mc.player)return;
            boolean current=incomingRevision>=revision;
            if(current){
                PlayerInventoryStateB173.apply(PlayerViewB173.of(mc.player),copy);
                revision=incomingRevision;
            }
            if(pending!=null&&pending.epoch()==incomingEpoch&&pending.token()==incomingToken&&pending.request()==request){
                if(carry!=null&&current&&InventoryPanelB173.matchesContext(incomingToken)&&mc.player.inventoryMenu.getStateId()==stateId)
                    mc.player.inventoryMenu.setCarried(ItemStack.parseOptional(mc.player.registryAccess(),carry));
                pending=null;
            }
        });
    }

    static boolean pending(){return pending!=null;}
    static void click(long screenToken,String identity,int slot,int button,boolean quick) {
        if(pending!=null||revision<0||!currentLifetime()||!InventoryPanelB173.matchesContext(screenToken))return;
        Minecraft mc=Minecraft.getInstance();
        long request=++nextRequest;
        pending=new Pending(epoch,screenToken,request);
        PlayerInventoryNetworkB173.requestClick(epoch,screenToken,identity,slot,button,quick,request,revision,mc.player.inventoryMenu.getStateId());
    }
    private static boolean currentLifetime() {
        Minecraft mc=Minecraft.getInstance();
        return connection!=null&&connection==mc.getConnection()&&level!=null&&level==mc.level&&previousPlayer!=null&&previousPlayer==mc.player;
    }
    private static void tick() {
        Minecraft mc=Minecraft.getInstance();var currentConnection=mc.getConnection();
        if(connection!=currentConnection||level!=mc.level||previousPlayer!=mc.player){
            if(previousPlayer!=null)PlayerViewB173.release(previousPlayer);
            previousPlayer=mc.player;connection=currentConnection;level=mc.level;
            epoch++;if(epoch<=0)epoch=1;
            token=-1;kind=heartbeat=0;revision=-1;pending=null;
            InventoryPanelB173.reset();
        }
        if(currentConnection==null||mc.level==null||mc.player==null||!PlayerApiB173.hasRegistered()
                ||!currentConnection.hasChannel(org.kylepitcock.legacyconnector.core.network.LegacyPayload.TYPE))return;
        var context=InventoryPanelB173.currentContext();
        if(token!=context.token()||kind!=context.kind()||++heartbeat>=20){
            if(token!=context.token()||kind!=context.kind())pending=null;
            token=context.token();kind=context.kind();heartbeat=0;
            PlayerInventoryNetworkB173.sendContext(epoch,token,kind);
        }
    }
}
