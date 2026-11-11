package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Tracks remote presentation and hydrates owner scalars; owner inventory stays on its transaction protocol. */
public final class PlayerPresentationClientB173 {
    private static final int MAX_PENDING=32,MAX_MIRRORS=256,MAX_WAIT=200,OWNER_HELLO_TICKS=200;
    private record Pending(CompoundTag tag,int expires){}
    private record Seen(Player player,long revision){}
    private static final Map<UUID,Pending> PENDING=new LinkedHashMap<>();
    private static final Map<UUID,Seen> SEEN=new LinkedHashMap<>();
    private static Object connection,level;
    private static Seen owner;
    private static Player ownerPlayer;
    private static int ownerHello;
    private static int tick;
    private static boolean installed;
    private PlayerPresentationClientB173(){}
    public static synchronized void install(){
        if(installed)return;installed=true;
        PlayerPresentationNetworkB173.installClient((source,tag)->{
            var mc=Minecraft.getInstance();lifetime();
            if(mc.getConnection()==null||mc.getConnection().getConnection()!=source||mc.level==null||mc.player==null)return;
            var identity=PlayerPresentationNetworkB173.identity(tag);
            if(identity==null||!identity.dimension().equals(mc.level.dimension().location()))return;
            if(identity.subject().equals(mc.player.getUUID())){
                if(!tag.getBoolean("remove"))applyOwner(tag,identity);
                return;
            }
            if(tag.getBoolean("remove")){
                var seen=SEEN.get(identity.subject());
                if(seen!=null&&seen.player().getId()==identity.entity()&&identity.revision()>=seen.revision()){
                    PlayerViewB173.release(seen.player());SEEN.remove(identity.subject());
                }
                var pending=PENDING.get(identity.subject());
                if(pending!=null){var old=PlayerPresentationNetworkB173.identity(pending.tag());if(old!=null&&old.entity()==identity.entity()&&old.revision()<=identity.revision())PENDING.remove(identity.subject());}
                return;
            }
            if(apply(tag))return;
            // A visible different entity at this ID is an identity mismatch, never a spawn-order retry.
            if(mc.level.getEntity(identity.entity())!=null)return;
            var previous=PENDING.get(identity.subject());
            if(previous!=null&&PlayerPresentationNetworkB173.identity(previous.tag()).revision()>=identity.revision())return;
            PENDING.put(identity.subject(),new Pending(tag.copy(),tick+MAX_WAIT));trim(PENDING,MAX_PENDING);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->PlayerPresentationNetworkB173.guard("client-tick",()->{
            lifetime();PlayerViewB173.releaseDepartedClientViews(Minecraft.getInstance().level);tick++;if(level==null)return;
            var mc=Minecraft.getInstance();
            if(PlayerApiB173.hasRegistered()&&mc.player!=null&&(owner==null||owner.player()!=mc.player)&&ownerHello<OWNER_HELLO_TICKS&&mc.getConnection()!=null
                    &&mc.getConnection().hasChannel(org.kylepitcock.legacyconnector.core.network.LegacyPayload.TYPE)
                    &&ownerHello++%20==0)PlayerPresentationNetworkB173.requestOwner();
            var it=PENDING.entrySet().iterator();while(it.hasNext()){
                var entry=it.next();if(entry.getValue().expires()<tick||apply(entry.getValue().tag()))it.remove();
            }
        }));
    }
    private static void lifetime(){
        var mc=Minecraft.getInstance();var current=mc.getConnection();
        if(owner!=null&&(owner.player()!=mc.player||connection!=current))PlayerViewB173.release(owner.player());
        if(connection!=current||level!=mc.level){PENDING.clear();PlayerPresentationStateB173.releaseMirrors(SEEN,seen->PlayerViewB173.release(seen.player()));connection=current;level=mc.level;tick=0;owner=null;ownerHello=0;ownerPlayer=mc.player;}
        if(ownerPlayer!=mc.player){ownerPlayer=mc.player;owner=null;ownerHello=0;}
    }
    private static void applyOwner(CompoundTag tag,PlayerPresentationStateB173.Identity identity){
        var mc=Minecraft.getInstance();var player=mc.player;
        long revision=owner==null?0:PlayerPresentationStateB173.ownerRevision(owner.player(),player,owner.revision());
        if(!PlayerPresentationStateB173.matchesOwner(identity,player.getUUID(),player.getId(),mc.level.dimension().location(),revision))return;
        var view=PlayerViewB173.peek(player);if(view==null)view=PlayerViewB173.of(player);
        PlayerApiB173.ensure(view);
        // Never apply owner inventories or write the mirror back to native health/movement here.
        if(PlayerPresentationStateB173.applyOwnerScalars(view,tag.getCompound("state")))owner=new Seen(player,identity.revision());
    }
    private static <T>void trim(Map<UUID,T> map,int limit){while(map.size()>limit)map.remove(map.keySet().iterator().next());}
    private static boolean apply(CompoundTag tag){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return false;
        var identity=PlayerPresentationNetworkB173.identity(tag);if(identity==null)return true;
        var entity=mc.level.getEntity(identity.entity());if(entity==null)return false;
        if(!(entity instanceof Player player))return true;
        var seen=SEEN.get(identity.subject());long revision=seen!=null&&seen.player()==player?seen.revision():0;
        if(!PlayerPresentationStateB173.matches(identity,mc.player.getUUID(),player.getUUID(),player.getId(),mc.level.dimension().location(),revision))return true;
        boolean created=false;
        try{
            var view=PlayerViewB173.peek(player);if(view==null){created=true;view=PlayerViewB173.of(player);}
            // This initializes only registered mirrors if needed; it never runs onLivingUpdate/readEntityFromNBT.
            if(!PlayerPresentationStateB173.apply(view,tag.getCompound("state"))){if(created)PlayerViewB173.release(player);return true;}
            var replaced=SEEN.put(identity.subject(),new Seen(player,identity.revision()));
            if(replaced!=null&&replaced.player()!=player)PlayerViewB173.release(replaced.player());
            PlayerPresentationStateB173.trimMirrors(SEEN,MAX_MIRRORS,old->PlayerViewB173.release(old.player()));
        }catch(RuntimeException|LinkageError malformed){if(created)PlayerViewB173.release(player);PlayerPresentationNetworkB173.report("client-apply",malformed);return true;}
        return true;
    }
}
