package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual Kid carry callbacks must synchronize the carrier's own client, not only tracking observers. */
public final class MoreCreepsCarrierSyncSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CarrierSyncTest");
    private MoreCreepsCarrierSyncSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS CARRIER SYNC TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level,List<ClientboundSetPassengersPacket> packets){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:carrier-self-sync".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Carrier");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            private void record(net.minecraft.network.protocol.Packet<?> packet){if(packet instanceof ClientboundSetPassengersPacket passengers)packets.add(passengers);}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){record(packet);}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){record(packet);}
        };return player;
    }
    private static void run(ServerLevel level)throws ReflectiveOperationException {
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityKid")).findFirst();if(selected.isEmpty())return;
        var before=new HashSet<Integer>();for(var entity:level.getAllEntities())before.add(entity.getId());
        var packets=new ArrayList<ClientboundSetPassengersPacket>();var player=actor(level,packets);
        try{
            var origin=level.getSharedSpawnPos().offset(8,30,8);for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk((origin.getX()>>4)+dx,(origin.getZ()>>4)+dz);
            player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);level.addNewPlayer(player);
            player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            var world=World.mcc$wrap(level);var original=(LivingEntity)selected.get().type().getConstructor(World.class).newInstance(world);
            original.setPosition(origin.getX()+1.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"actual Kid enters native world");
            var host=(LegacyB173Animal)original.mcc$modern;packets.clear();
            require(host.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"actual Kid pickup callback consumes main-hand interaction");EntityViewsB173.flush();
            require(host.getVehicle()==player&&player.getFirstPassenger()==host,"actual Kid pickup establishes reciprocal native relation");
            require(packets.size()==1&&packets.getFirst().getVehicle()==player.getId()&&Arrays.equals(packets.getFirst().getPassengers(),new int[]{host.getId()}),"carrier connection receives exactly one pickup packet naming original Kid");
            double expectedY;
            try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,original);var views=EntityViewsB173.callbackScope()){
                var carrier=EntityViewsB173.wrap(player);expectedY=carrier.y+carrier.getPassengerRidingHeight()+original.getStandingEyeHeight()-original.standingEyeHeight;
            }
            player.positionRider(host);
            require(Math.abs(host.getX()-player.getX())<.000001&&Math.abs(host.getY()-expectedY)<.000001&&Math.abs(host.getZ()-player.getZ())<.000001,"actual Kid native carrier seat uses original head offset");
            require(!host.startRiding(player,true)&&packets.size()==1,"unchanged same-carrier mount sends no redundant self packet");
            var blocked=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);blocked.setPos(origin.getX()+8,origin.getY(),origin.getZ());level.addFreshEntity(blocked);
            var occupant=new net.minecraft.world.entity.animal.Pig(EntityType.PIG,level);occupant.setPos(blocked.position());level.addFreshEntity(occupant);require(occupant.startRiding(blocked,true),"rejected-mount control has occupied native carrier");
            require(!host.startRiding(blocked,false)&&host.getVehicle()==player&&packets.size()==1,"rejected replacement mount retains original carrier and sends no extra self packet");
            packets.clear();require(host.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"actual Kid release callback consumes main-hand interaction");EntityViewsB173.flush();
            require(host.getVehicle()==null&&player.getPassengers().isEmpty(),"actual Kid release removes reciprocal native relation");
            require(packets.size()==1&&packets.getFirst().getVehicle()==player.getId()&&packets.getFirst().getPassengers().length==0,"prior carrier connection receives exactly one empty-passenger release packet");
            host.stopRiding();require(packets.size()==1,"already-detached release sends no redundant self packet");
            LOG.info("B1.7.3 CREEPS CARRIER SYNC TEST: actual Kid pickup/head seat/release synchronize carrier connection; unchanged and rejected native mounts emit no extra packets");
        }finally{
            EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();
        }
    }
}
