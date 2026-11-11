package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.src.DimensionBase;
import legacyconnector.vb1_7_3.net.minecraft.src.Loc;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.vb1_7_3.dimension.DimensionRegistrationsB173;

/** Original dimension registrations and player travel backed by native server levels. */
public final class DimensionsB173 {
    public static final ResourceLocation EFFECTS=ResourceLocation.fromNamespaceAndPath("legacyconnector","legacy_b173_dimension");
    private static final Map<UUID,PortalState> PORTALS=new HashMap<>();
    private static final Map<UUID,LinkedList<Integer>> HISTORY=new HashMap<>();
    private static final Set<String> REPORTED=new HashSet<>();
    private static boolean installed;
    private static long failures,travels;
    private static String lastFailure="";
    private static final class PortalState {int target,cooldown;float exposure;long contact=Long.MIN_VALUE;}
    private DimensionsB173(){}
    public static void register(DimensionBase original){DimensionRegistrationsB173.register(original);}
    public static Dimension providerFor(World world){
        if(world==null)return null;
        if(world.dimension!=null&&world.dimension.world==world)return world.dimension;
        var provider=DimensionRegistrationsB173.newProvider(world);
        if(provider!=null&&provider.world!=world)provider.setWorld(world);
        return provider;
    }
    public static Dimension provider(int id){return DimensionRegistrationsB173.provider(id);}
    public static Integer registeredId(Level level){return DimensionRegistrationsB173.registeredId(level);}
    public static int worldId(Level level){return DimensionRegistrationsB173.worldId(level);}
    public static ResourceLocation biomeKey(int id,Biome biome){return DimensionRegistrationsB173.biomeKey(id,biome);}
    public static List<String> biomeIds(int id){return DimensionRegistrationsB173.biomeIds(id);}
    public static ResourceKey<Level> levelKey(int id){return DimensionRegistrationsB173.levelKey(id);}
    public static void contribute(Map<String,Supplier<byte[]>> out){DimensionRegistrationsB173.contribute(out);}
    public static long failures(){return failures;}
    public static long travels(){return travels;}
    public static String lastFailure(){return lastFailure;}
    public static double portalExposure(UUID player){var state=PORTALS.get(player);return state==null?0:state.exposure;}
    public static int portalCooldown(UUID player){var state=PORTALS.get(player);return state==null?0:state.cooldown;}

    public static synchronized void install(){
        if(installed)return;installed=true;
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event)->{
            long tick=event.getServer().getTickCount();
            for(var player:List.copyOf(event.getServer().getPlayerList().getPlayers())){
                var state=PORTALS.get(player.getUUID());if(state==null)continue;
                if(state.contact==tick&&state.cooldown==0){
                    if(state.exposure==0)portalSound(player,state.target,false);
                    state.exposure=Math.min(1,state.exposure+.0125F);
                    if(state.exposure>=1){portalSound(player,state.target,true);usePortal(player,state.target);state.exposure=0;state.cooldown=10;}
                }else state.exposure=Math.max(0,state.exposure-.05F);
                if(state.cooldown>0)state.cooldown--;
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{
            var leaving=event.getEntity();
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Dimensions").info("{} saved at ({}, {}, {}) in {}",leaving.getGameProfile().getName(),
                    Math.round(leaving.getX()*10)/10.0,Math.round(leaving.getY()*10)/10.0,Math.round(leaving.getZ()*10)/10.0,leaving.level().dimension().location());
            PORTALS.remove(event.getEntity().getUUID());HISTORY.remove(event.getEntity().getUUID());
        });
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{PORTALS.clear();HISTORY.clear();REPORTED.clear();});
    }
    public static void portalContact(World world,Entity entity,int target){
        if(world==null||target==0)return;
        if(world.isRemote){
            if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                org.kylepitcock.legacyconnector.vb1_7_3.client.DimensionClientB173.portalContact(entity.mcc$modern,target);
            return;
        }
        if(!(entity.mcc$modern instanceof ServerPlayer player)||player.level()!=world.mcc$level())return;
        if(!player.server.isSameThread()){fail("Portal contact off server thread",null);return;}
        var realm=player.level().dimension();
        if(registeredId(player.level())==null&&!realm.equals(Level.OVERWORLD)&&!realm.equals(Level.NETHER))return;
        var state=PORTALS.computeIfAbsent(player.getUUID(),id->new PortalState());
        state.target=target;
        if(state.cooldown>0){state.cooldown=10;return;}
        state.contact=player.server.getTickCount();
    }
    private static legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity actor(){
        var player=Minecraft.mcc$currentPlayerContext();
        return player!=null?player:Minecraft.mcc$get$player(Minecraft.mcc$instance());
    }
    public static void usePortal(int target){
        var player=actor();if(player==null){fail("Portal requested without a player context",null);return;}
        if(player.mcc$modern instanceof ServerPlayer serverPlayer){
            try(var scope=Minecraft.mcc$context(player.world,player)){usePortal(serverPlayer,target);}return;
        }
        if(player.mcc$modern instanceof net.minecraft.world.entity.player.Player nativePlayer&&nativePlayer.level().isClientSide
                &&net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            org.kylepitcock.legacyconnector.vb1_7_3.client.DimensionClientB173.usePortal(target);
    }
    public static void respawn(boolean force,int target){
        var player=actor();if(player==null){fail("Respawn requested without a player context",null);return;}
        if(player.mcc$modern instanceof ServerPlayer serverPlayer){respawn(serverPlayer,force,target);return;}
        if(player.mcc$modern instanceof net.minecraft.world.entity.player.Player nativePlayer&&nativePlayer.level().isClientSide
                &&net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            org.kylepitcock.legacyconnector.vb1_7_3.client.DimensionClientB173.respawn(force,target);
    }
    private static PlayerViewB173 view(ServerPlayer player){
        var current=Minecraft.mcc$currentPlayerContext();
        return current instanceof PlayerViewB173 view&&view.mcc$modern==player?view:PlayerViewB173.of(player);
    }
    public static void usePortal(ServerPlayer player,int requested){
        if(!player.server.isSameThread()){player.server.execute(()->usePortal(player,requested));return;}
        var current=player.serverLevel();Integer owned=registeredId(current);
        if(owned==null&&!current.dimension().equals(Level.OVERWORLD)&&!current.dimension().equals(Level.NETHER)){
            fail("Portal requested from an unrelated native dimension "+current.dimension().location(),null);return;
        }
        int sourceId=worldId(current);
        var order=new LinkedList<>(HISTORY.getOrDefault(player.getUUID(),new LinkedList<>()));
        int targetId=selectDestination(order,sourceId,requested);
        if(travel(player,sourceId,targetId))HISTORY.put(player.getUUID(),order);
    }
    /** Original stack transitions, scoped to one player instead of a process-global client list. */
    static int selectDestination(LinkedList<Integer> order,int current,int requested){
        int target=current==requested?0:requested;
        if(target!=0)order.push(target);
        if(target==0&&!order.isEmpty())target=order.pop();
        return target==current?0:target;
    }
    private static boolean travel(ServerPlayer player,int sourceId,int targetId){
        var source=DimensionBase.getDimByNumber(sourceId);var destination=DimensionBase.getDimByNumber(targetId);
        var key=levelKey(targetId);var target=key==null?null:player.server.getLevel(key);
        if(source==null||destination==null||target==null){fail("Registered portal destination unavailable: "+targetId,null);return false;}
        var legacy=view(player);legacy.writeBack();
        var sourceLevel=player.serverLevel();
        double sourceX=player.getX(),sourceY=player.getY(),sourceZ=player.getZ();
        float sourceYaw=player.getYRot(),sourcePitch=player.getXRot();
        var callbackActor=Minecraft.mcc$currentPlayerContext();var minecraft=Minecraft.mcc$instance();var previousWorld=minecraft.world;
        try{
            Loc position=source.getDistanceScale(new Loc(legacy.x,legacy.z),true);
            position=destination.getDistanceScale(position,false);
            if(position==null||!finite(position.x,legacy.y,position.z,legacy.yaw,legacy.pitch))throw new IllegalStateException("Non-finite portal placement");
            player.teleportTo(target,position.x,legacy.y-legacy.standingEyeHeight,position.z,legacy.yaw,legacy.pitch);
            if(player.serverLevel()!=target)throw new IllegalStateException("Native portal transfer did not enter its registered destination");
            legacy.mcc$afterTravel();sync(player);
            var teleporter=destination.getTeleporter();if(teleporter==null)teleporter=source.getTeleporter();
            if(teleporter!=null){
                try(var scope=Minecraft.mcc$context(legacy.world,legacy)){
                    teleporter.moveToPortal(legacy.world,legacy);
                    if(!finite(legacy.x,legacy.y,legacy.z,legacy.yaw,legacy.pitch))throw new IllegalStateException("Non-finite original teleporter placement");
                    legacy.writeBack();
                }
            }
            legacy.mcc$afterTravel();sync(player);travels++;
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Dimensions").info("Portal travel {} {} -> {}: ({}, {}, {}) in {} to ({}, {}, {}) in {}",
                    player.getGameProfile().getName(),sourceId,targetId,Math.round(sourceX*10)/10.0,Math.round(sourceY*10)/10.0,Math.round(sourceZ*10)/10.0,
                    sourceLevel.dimension().location(),Math.round(player.getX()*10)/10.0,Math.round(player.getY()*10)/10.0,Math.round(player.getZ()*10)/10.0,player.level().dimension().location());
            return true;
        }catch(RuntimeException|LinkageError error){
            // A failed original placement must not strand the actor in the destination with source history.
            // Inventory writes and original world changes remain authoritative; restore only realm and placement.
            try{player.teleportTo(sourceLevel,sourceX,sourceY,sourceZ,sourceYaw,sourcePitch);}
            catch(RuntimeException|LinkageError rollbackError){error.addSuppressed(rollbackError);}
            legacy.mcc$afterTravel();sync(player);
            fail("Portal travel "+sourceId+" -> "+targetId+" failed",error);
            if(player.serverLevel()!=sourceLevel){
                fail("Portal placement rollback did not restore source realm "+sourceLevel.dimension().location(),error);
                // If another native hook prevented rollback, retain the history of the realm actually entered.
                return target!=sourceLevel&&player.serverLevel()==target;
            }
            return false;
        }
        finally{minecraft.world=callbackActor==legacy?legacy.world:previousWorld;}
    }
    public static void respawn(ServerPlayer player,boolean force,int requested){
        if(!player.server.isSameThread()){player.server.execute(()->respawn(player,force,requested));return;}
        var sourceLevel=player.serverLevel();
        if(registeredId(sourceLevel)==null&&!sourceLevel.dimension().equals(Level.OVERWORLD)&&!sourceLevel.dimension().equals(Level.NETHER)){
            fail("Respawn requested from an unrelated native dimension "+sourceLevel.dimension().location(),null);return;
        }
        try{
        var sourceProvider=providerFor(World.mcc$wrap(player.level()));
        // Original paramInt labels the recreated legacy player; it does not choose a new world.
        // The native view always reports its actual realm to avoid confusing other modern dimensions.
        int targetId=sourceProvider!=null&&!sourceProvider.hasWorldSpawn()?0:worldId(sourceLevel);
        var key=levelKey(targetId);var target=key==null?null:player.server.getLevel(key);
        if(target==null){fail("Registered respawn destination unavailable: "+targetId,null);return;}
            view(player).writeBack();
            var previousDimension=player.getRespawnDimension();var previousPosition=player.getRespawnPosition();
            float previousAngle=player.getRespawnAngle();boolean previousForced=player.isRespawnForced();
            // The original flag ignores the bed; it never requests keeping the old inventory.
            boolean ignoreBed=force||!previousDimension.equals(target.dimension());
            if(ignoreBed)player.setRespawnPosition(target.dimension(),target.getSharedSpawnPos(),target.getSharedSpawnAngle(),true,false);
            ServerPlayer replacement;
            try{replacement=player.server.getPlayerList().respawn(player,false,net.minecraft.world.entity.Entity.RemovalReason.KILLED);}
            finally{if(ignoreBed)player.setRespawnPosition(previousDimension,previousPosition,previousAngle,previousForced,false);}
            if(ignoreBed)replacement.setRespawnPosition(previousDimension,previousPosition,previousAngle,previousForced,false);
            if(replacement.serverLevel()!=target){
                var spawn=target.getSharedSpawnPos();replacement.teleportTo(target,spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,target.getSharedSpawnAngle(),0);
            }
            PlayerViewB173.release(player);var replacementView=PlayerViewB173.of(replacement);replacementView.mcc$afterTravel();sync(replacement);
            var callbackActor=Minecraft.mcc$currentPlayerContext();
            if(callbackActor!=null&&callbackActor.mcc$modern==player)Minecraft.mcc$instance().world=replacementView.world;
        }catch(RuntimeException|LinkageError error){fail("Native respawn failed",error);}
    }
    private static boolean finite(double... values){for(double value:values)if(!Double.isFinite(value))return false;return true;}
    /** Original portal sounds are local to the entering actor, through the existing named-audio channel. */
    private static void portalSound(ServerPlayer player,int target,boolean travel){
        var dimension=DimensionBase.getDimByNumber(target);
        if(dimension==null){fail("Portal sound requested for unregistered dimension "+target,null);return;}
        String name=travel?dimension.soundTravel:dimension.soundTrigger;
        if(name==null||name.isEmpty())return;
        try{
            var request=new AudioB173.Request(AudioB173.Kind.SOUND,name,0,0,0,1,
                    player.getRandom().nextFloat()*.4F+.8F,true);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new org.kylepitcock.legacyconnector.core.network.LegacyPayload("b1.7.3-audio","play",0,AudioB173.encode(request)));
        }catch(RuntimeException|LinkageError error){fail("Portal "+(travel?"travel":"trigger")+" sound failed for "+target,error);}
    }
    private static void sync(ServerPlayer player){if(player.connection!=null)player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());}
    private static void fail(String message,Throwable error){
        failures++;lastFailure=message;
        if(REPORTED.add(message))org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173Dimensions").error(message,error);
    }
}
