package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual HorseSaddle.useOnEntity is compared with the native player's item/entity interaction path. */
public final class MoCreaturesHorseSaddleSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173HorseSaddleTest");
    private MoCreaturesHorseSaddleSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 MOC HORSE SADDLE TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static boolean saddled(LegacyB173Animal host)throws ReflectiveOperationException{return field(host.legacy(),"rideable").getBoolean(host.legacy());}
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final ContentB173.Registration registration;
        final legacyconnector.vb1_7_3.net.minecraft.item.Item saddle;final net.minecraft.server.level.ServerPlayer player;
        final List<net.minecraft.world.entity.Entity> entities=new ArrayList<>();
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration)throws ReflectiveOperationException {
            this.level=level;this.registration=registration;world=World.mcc$wrap(level);saddle=(legacyconnector.vb1_7_3.net.minecraft.item.Item)registration.mod().instance.getClass().getField("horsesaddle").get(null);
            var profile=new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("legacyconnector:horse-saddle".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Saddle");
            player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
            };
            var origin=level.getSharedSpawnPos().offset(8,40,8);level.getChunk(origin);player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+2.5);
        }
        LegacyB173Animal horse(boolean adult)throws ReflectiveOperationException {
            var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);put(original,"adult",adult);put(original,"rideable",false);put(original,"tamed",false);
            original.setPosition(player.getX(),player.getY(),player.getZ()-2);require(world.spawnEntity(original),"actual unsaddled horse host admitted");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);return host;
        }
        void hold(int count){player.getInventory().selected=0;player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(saddle.id,count,0));}
        @Override public void close(){player.stopRiding();player.closeContainer();entities.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();}
    }
    @FunctionalInterface private interface Proof {void run(Context context)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")&&r.type().getSimpleName().equals("EntityHorse")).findFirst();if(selected.isEmpty())return;
        var cases=List.of(new Case("original item callback contract",MoCreaturesHorseSaddleSelfTestB173::original),new Case("native adult horse saddle interaction",MoCreaturesHorseSaddleSelfTestB173::nativeAdult),new Case("native rejected saddle targets",MoCreaturesHorseSaddleSelfTestB173::rejections),new Case("creative saddle count and components",MoCreaturesHorseSaddleSelfTestB173::creative),new Case("offhand and default-item isolation",MoCreaturesHorseSaddleSelfTestB173::isolation));var failures=new ArrayList<String>();
        for(var proof:cases)try(var context=new Context(level,selected.get())){proof.proof.run(context);LOG.info("B1.7.3 MOC HORSE SADDLE TEST: {} passed",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 MOC HORSE SADDLE TEST failed: {}",proof.name,error);}
        if(!failures.isEmpty())LOG.error("B1.7.3 MOC HORSE SADDLE TEST: {} failures: {}",failures.size(),failures);else LOG.info("B1.7.3 MOC HORSE SADDLE TEST: all {} actual-item proofs passed",cases.size());
    }
    private static void original(Context c)throws ReflectiveOperationException {
        var horse=c.horse(true);var stack=new ItemStack(c.saddle,2);var callback=c.saddle.getClass().getMethod("useOnEntity",ItemStack.class,LivingEntity.class);
        callback.invoke(c.saddle,stack,horse.legacy());require(saddled(horse)&&stack.count==1,"actual original adult saddle callback equips and consumes exactly one");
        callback.invoke(c.saddle,stack,horse.legacy());require(stack.count==1,"actual already-saddled callback consumes nothing");
        put(horse.legacy(),"adult",false);put(horse.legacy(),"rideable",false);callback.invoke(c.saddle,stack,horse.legacy());require(!saddled(horse)&&stack.count==1,"actual juvenile rejection preserves saddle stack");
    }
    private static void nativeAdult(Context c)throws ReflectiveOperationException {
        var horse=c.horse(true);c.hold(2);var label=net.minecraft.network.chat.Component.literal("Actual saddle components");c.player.getMainHandItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,label);var result=c.player.interactOn(horse,InteractionHand.MAIN_HAND);EntityViewsB173.flush();
        require(saddled(horse)&&c.player.getMainHandItem().getCount()==1&&result.consumesAction(),"native player interaction must invoke original useOnEntity: result="+result+" rideable="+saddled(horse)+" stack="+c.player.getMainHandItem());
        require(label.equals(c.player.getMainHandItem().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),"native survival saddle callback preserves caller components");
        require(c.player.getVehicle()==null,"saddling an unsaddled horse does not also mount player");
        c.player.interactOn(horse,InteractionHand.MAIN_HAND);require(c.player.getMainHandItem().getCount()==1,"subsequent original horse interaction never consumes another saddle");
    }
    private static void rejections(Context c)throws ReflectiveOperationException {
        var juvenile=c.horse(false);c.hold(2);c.player.interactOn(juvenile,InteractionHand.MAIN_HAND);EntityViewsB173.flush();require(!saddled(juvenile)&&c.player.getMainHandItem().getCount()==2,"native juvenile saddle attempt preserves original predicate and count");
        var cow=net.minecraft.world.entity.EntityType.COW.create(c.level);require(cow!=null,"plain native cow constructed");c.entities.add(cow);cow.setPos(c.player.getX(),c.player.getY(),c.player.getZ()-2);require(c.level.addFreshEntity(cow),"plain native rejection target admitted");
        c.player.getMainHandItem().getItem().interactLivingEntity(c.player.getMainHandItem(),c.player,cow,InteractionHand.MAIN_HAND);EntityViewsB173.flush();require(c.player.getMainHandItem().getCount()==2&&cow.getType()==net.minecraft.world.entity.EntityType.COW,"original horse saddle ignores plain native non-horse target");
    }
    private static void creative(Context c)throws ReflectiveOperationException {
        var horse=c.horse(true);c.hold(2);var label=net.minecraft.network.chat.Component.literal("Creative saddle components");c.player.getMainHandItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,label);
        c.player.getAbilities().instabuild=true;
        try{var result=c.player.interactOn(horse,InteractionHand.MAIN_HAND);EntityViewsB173.flush();require(result.consumesAction()&&saddled(horse)&&c.player.getMainHandItem().getCount()==2,"creative native interaction equips original saddle while retaining native creative count");require(label.equals(c.player.getMainHandItem().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),"creative saddle callback preserves custom components");}
        finally{c.player.getAbilities().instabuild=false;}
    }
    private static void isolation(Context c)throws ReflectiveOperationException {
        var horse=c.horse(true);c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);c.player.setItemInHand(InteractionHand.OFF_HAND,StacksB173.toModern(c.saddle.id,2,0));
        var result=c.player.interactOn(horse,InteractionHand.OFF_HAND);EntityViewsB173.flush();require(result==net.minecraft.world.InteractionResult.PASS&&!saddled(horse)&&c.player.getOffhandItem().getCount()==2,"Beta main-hand-only item API ignores native offhand without equipping or consuming");
        var medallion=(legacyconnector.vb1_7_3.net.minecraft.item.Item)c.registration.mod().instance.getClass().getField("medallion").get(null);require(medallion.getClass()==legacyconnector.vb1_7_3.net.minecraft.item.Item.class,"actual medallion uses default legacy item API without entity override");
        var stack=StacksB173.toModern(medallion.id,2,0);c.player.setItemInHand(InteractionHand.MAIN_HAND,stack);result=stack.getItem().interactLivingEntity(stack,c.player,horse,InteractionHand.MAIN_HAND);EntityViewsB173.flush();require(result==net.minecraft.world.InteractionResult.PASS&&!saddled(horse)&&stack.getCount()==2,"default legacy item entity interaction remains PASS without new global registration");
    }

}
