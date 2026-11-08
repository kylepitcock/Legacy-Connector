package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Original recruitment and army callbacks; the archived mod is a runtime dependency only. */
public final class MoreCreepsGameplayExtraSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsGameplayExtraTest");
    private MoreCreepsGameplayExtraSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{
        try{run(event.getServer().overworld());}catch(ReflectiveOperationException|RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS GAMEPLAY EXTRA TEST failed",error);}
    });}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object get(Object object,String name)throws ReflectiveOperationException{return field(object,name).get(object);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException{field(object,name).set(object,value);}
    private static final class FixedRandom extends Random {
        @Override public int nextInt(int bound){return 0;}
        @Override public float nextFloat(){return .5F;}
    }
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:hunchback-army".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Army");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static List<LegacyB173Animal> army(ServerLevel level){
        var result=new ArrayList<LegacyB173Animal>();
        for(var entity:level.getAllEntities())if(entity instanceof LegacyB173Animal host&&host.legacy().getClass().getSimpleName().equals("CREEPSEntityHunchbackSkeleton"))result.add(host);
        return result;
    }
    private static void interact(LegacyB173Animal host,net.minecraft.server.level.ServerPlayer player){
        require(!host.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"actual Hunchback preserves original false interaction return even when used");
        EntityViewsB173.flush();
    }
    private static void run(ServerLevel level)throws ReflectiveOperationException {
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&r.type().getSimpleName().equals("CREEPSEntityHunchback")).findFirst();
        if(selected.isEmpty())return;
        var registration=selected.get();var before=new HashSet<Integer>();for(var entity:level.getAllEntities())before.add(entity.getId());
        var player=actor(level);var world=World.mcc$wrap(level);var origin=level.getSharedSpawnPos().offset(8,40,8);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk((origin.getX()>>4)+dx,(origin.getZ()>>4)+dz);
        try{
            player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+2.5);level.addNewPlayer(player);
            var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);
            original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"actual Hunchback admitted");
            var host=(LegacyB173Animal)original.mcc$modern;put(original,"random",new FixedRandom());
            var achievement=(legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement)registration.mod().instance.getClass().getField("achievehunchback").get(null);
            var nativeAchievement=level.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));
            require(nativeAchievement!=null,"original Hunchback advancement loaded");player.getAdvancements().revoke(nativeAchievement,AdvancementsB173.CRITERION);
            var armyBefore=new HashSet<>(army(level));
            player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BONE,2));interact(host,player);
            require(player.getMainHandItem().getCount()==2&&army(level).size()==armyBefore.size()&&!(Boolean)get(original,"tamed"),"untamed Hunchback rejects bone without consuming or summoning");
            int base=(Integer)get(original,"basehealth");host.setHealth(base-3);player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.CAKE,2));
            interact(host,player);
            require((Boolean)get(original,"tamed")&&(Boolean)get(original,"used")&&(Integer)get(original,"interest")==15&&(Integer)get(original,"caketimer")==250,"actual cake recruits and raises original interest/timer");
            require(host.getHealth()==base-1&&player.getMainHandItem().getCount()==1,"actual cake heals native host and consumes one selected item");
            require(player.getAdvancements().getOrStartProgress(nativeAchievement).isDone(),"original cake callback awards native Hunchback advancement");
            require(host.texturePath().equals("/mob/creeps/hunchbackcake.png"),"recruited original texture reaches native render state");
            interact(host,player);
            require(host.getHealth()==base&&player.getMainHandItem().isEmpty()&&(Integer)get(original,"interest")==30&&(Integer)get(original,"caketimer")==500,"second cake clamps original health and clears exhausted inventory slot");
            player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BONE,2));interact(host,player);
            var summoned=army(level).stream().filter(entity->!armyBefore.contains(entity)).sorted(Comparator.comparingDouble(net.minecraft.world.entity.Entity::getZ)).toList();
            require(summoned.size()==5&&player.getMainHandItem().getCount()==1,"one original bone summons exactly five registered native Skeleton hosts and consumes one");
            for(int i=0;i<summoned.size();i++){
                var skeleton=summoned.get(i);var identity=skeleton.legacy();
                require(EntityViewsB173.wrap(skeleton)==identity&&identity.mcc$modern==skeleton,"summoned army retains each exact original identity");
                require(Math.abs(skeleton.getX()-(original.x+3))<.000001&&Math.abs(skeleton.getY()-(original.y-identity.standingEyeHeight))<.000001&&Math.abs(skeleton.getZ()-(original.z+i))<.000001,"original army formation coordinates reach native insertion: "+i);
                require(level.getEntities(host,skeleton.getBoundingBox().inflate(.01)).contains(skeleton),"summoned army enters loaded native query sections");
            }
            EntityViewsB173.flush();LOG.info("B1.7.3 CREEPS GAMEPLAY EXTRA TEST: actual Hunchback cake recruitment, native advancement, health/inventory boundaries and five-Skeleton army formation passed");
        }finally{
            EntityViewsB173.flush();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);
            added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();
        }
    }
}
