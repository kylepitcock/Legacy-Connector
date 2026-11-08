package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controlled actual MoreCreeps death, resurrection and dismemberment callbacks. No production behavior is selected by these names. */
public final class MoreCreepsLifecycleExtraSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsLifecycleExtraTest");
    private MoreCreepsLifecycleExtraSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS LIFECYCLE EXTRA TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private static Object call(Object object,String name,Class<?>[] signature,Object... args)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(object,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
    private static final class ZeroRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    /** FakePlayer deliberately disables riding, menus and advancements; these proofs need normal native behavior. */
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level,int index){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("legacyconnector:creeps-lifecycle:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Feature"+index);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }
    private static final class Context implements AutoCloseable {
        final net.minecraft.server.level.ServerLevel level;final World world;final net.minecraft.server.level.ServerPlayer player;
        final ContentB173.Registration registration;final net.minecraft.core.BlockPos origin;final ArrayList<net.minecraft.world.entity.Entity> entities=new ArrayList<>();
        final Set<Integer> baseline=new HashSet<>();
        final net.minecraft.nbt.ListTag inventory=new net.minecraft.nbt.ListTag();final net.minecraft.world.phys.Vec3 previousPosition,previousMotion;final int selected;final Random random;final boolean shifted;
        Context(net.minecraft.server.level.ServerLevel level,ContentB173.Registration registration,int index){this.level=level;for(var entity:level.getAllEntities())baseline.add(entity.getId());this.registration=registration;world=World.mcc$wrap(level);player=actor(level,index);previousPosition=player.position();previousMotion=player.getDeltaMovement();selected=player.getInventory().selected;shifted=player.isShiftKeyDown();player.setShiftKeyDown(false);player.getInventory().save(inventory);player.getInventory().clearContent();origin=level.getSharedSpawnPos().offset(8,24,8);level.getChunk(origin);player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setDeltaMovement(0,0,0);random=world.random;world.random=new ZeroRandom();}
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LegacyB173Animal spawn(String name)throws ReflectiveOperationException {var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);require(world.spawnEntity(original),"original spawn rejected: "+name);require(original.mcc$modern instanceof LegacyB173Animal,"generic living host required");var host=(LegacyB173Animal)original.mcc$modern;entities.add(host);require(level.getEntities((net.minecraft.world.entity.Entity)null,host.getBoundingBox().inflate(.1)).contains(host),"fixture original host must enter loaded native entity query sections");put(original,"random",new ZeroRandom());EntityViewsB173.flush();return host;}
        @Override public void close(){MenusB173.withActor(player,()->legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().setScreen(null));player.stopRiding();player.closeContainer();EntityViewsB173.flush();var descendants=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!baseline.contains(entity.getId()))descendants.add(entity);for(var entity:descendants)entity.discard();for(var entity:entities)entity.discard();player.getInventory().clearContent();player.getInventory().load(inventory);player.getInventory().selected=selected;player.setPos(previousPosition.x,previousPosition.y,previousPosition.z);player.setDeltaMovement(previousMotion);world.random=random;player.setShiftKeyDown(shifted);EntityViewsB173.flush();player.discard();}
    }
    @FunctionalInterface private interface Proof {void run(Context c)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var failures=new ArrayList<String>();int index=0;
        for(var proof:List.of(new Case("native pet death and LifeGem resurrection",MoreCreepsLifecycleExtraSelfTestB173::resurrection),new Case("ArmyGuy limbs and body-part expiration",MoreCreepsLifecycleExtraSelfTestB173::limbs)))try(var c=new Context(level,registration.get(),index++)){proof.proof.run(c);LOG.info("B1.7.3 CREEPS LIFECYCLE EXTRA TEST: {} passed using original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 CREEPS LIFECYCLE EXTRA TEST failed",error);}
        require(failures.isEmpty(),"B1.7.3 CREEPS LIFECYCLE EXTRA TEST: "+failures);LOG.info("B1.7.3 CREEPS LIFECYCLE EXTRA TEST: all 2 original lifecycle proofs passed");
    }
    private static List<LegacyB173Animal> named(Context c,String name){var result=new ArrayList<LegacyB173Animal>();for(var entity:c.level.getAllEntities())if(!entity.isRemoved()&&entity instanceof LegacyB173Animal host&&host.legacy().getClass().getSimpleName().equals(name))result.add(host);return result;}
    private static void resurrection(Context c)throws ReflectiveOperationException {
        var dog=c.spawn("CREEPSEntityHotdog");dog.setNoAi(true);put(dog.legacy(),"tamed",true);put(dog.legacy(),"interest",100);put(dog.legacy(),"name","Lifecycle Proof");put(dog.legacy(),"level",7);put(dog.legacy(),"wanderstate",1);put(dog.legacy(),"speedboost",800);put(dog.legacy(),"totalexperience",123);put(dog.legacy(),"baseSpeed",.45F);put(dog.legacy(),"skillattack",2);put(dog.legacy(),"skilldefend",3);put(dog.legacy(),"skillhealing",1);put(dog.legacy(),"skillspeed",4);put(dog.legacy(),"firepower",2);put(dog.legacy(),"dogsize",.8F);
        var fields=List.of("interest","tamed","name","basehealth","level","basetexture","totaldamage","wanderstate","totalexperience","baseSpeed","skillattack","skilldefend","skillhealing","skillspeed","firepower","dogsize");var expected=new LinkedHashMap<String,Object>();for(String name:fields)expected.put(name,get(dog.legacy(),name));var oldStones=named(c,"CREEPSEntityTombstone");
        MenusB173.withActor(c.player,()->require(dog.hurt(c.level.damageSources().playerAttack(c.player),1000),"actual native lethal attack accepted"));require(dog.getHealth()==0,"original damage callback reaches native fatal health");for(String name:fields)expected.put(name,get(dog.legacy(),name));for(int tick=0;tick<25&&!dog.isRemoved();tick++)MenusB173.withActor(c.player,dog::tick);
        var stones=named(c,"CREEPSEntityTombstone").stream().filter(s->!oldStones.contains(s)).toList();require(dog.isRemoved()&&stones.size()==1,"native death completion invokes original markDead and creates exactly one tombstone: removed="+dog.isRemoved()+" stones="+stones.size());var stone=stones.getFirst();require(get(stone.legacy(),"deathtype").equals("Hotdog"),"original tombstone records exact pet family");for(String name:fields)require(Objects.equals(expected.get(name),get(stone.legacy(),name)),"original death transfers tombstone field "+name);
        EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT,2));require(!stone.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&!stone.isRemoved(),"wrong resurrection item leaves tombstone intact");
        var gem=(legacyconnector.vb1_7_3.net.minecraft.item.Item)c.registration.mod().instance.getClass().getField("lifegem").get(null);EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(gem.id,2,0));var prior=named(c,"CREEPSEntityHotdog");require(stone.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction()&&stone.isRemoved()&&c.player.getMainHandItem().getCount()==1,"actual LifeGem consumes one and removes tombstone");var raised=named(c,"CREEPSEntityHotdog").stream().filter(h->!prior.contains(h)).toList();require(raised.size()==1,"original LifeGem creates exactly one new native pet");var restored=raised.getFirst();require(restored.getHealth()==5&&restored.legacy().mcc$movementSpeed()==0,"original raised pet has five health and retained stay command");for(String name:fields)require(Objects.equals(expected.get(name),get(restored.legacy(),name)),"original LifeGem transfers restored field "+name);
        // Original Tombstone.interact does not assign an owner on the new pet.
        require(get(restored.legacy(),"owner")==null,"resurrection retains original unassigned-owner policy");
    }
    private static void limbs(Context c)throws ReflectiveOperationException {
        var soldier=c.spawn("CREEPSEntityArmyGuy");soldier.setNoAi(true);put(soldier.legacy(),"modelsize",.8F);var blood=c.registration.mod().instance.getClass().getField("Blood");boolean priorBlood=blood.getBoolean(null);blood.setBoolean(null,false);
        try{int[] health={45,35,25,15};String[] flags={"armleft","legright","legleft","armright"};String[] textures={"armyguy.png","armyguyleg.png","armyguyleg.png","armyguy.png"};double[] velocityY={-.25,.25,-.25,.25};
            for(int index=0;index<health.length;index++){EntityViewsB173.flush();soldier.setHealth(health[index]);var before=named(c,"CREEPSEntityArmyGuyArm");MenusB173.withActor(c.player,soldier::aiStep);var added=named(c,"CREEPSEntityArmyGuyArm").stream().filter(a->!before.contains(a)).toList();require(added.size()==1&&(Boolean)get(soldier.legacy(),flags[index]),"actual health threshold detaches exactly one original limb: "+health[index]);var limb=added.getFirst();require(limb.legacy().mcc$texture().endsWith(textures[index])&&(Float)get(limb.legacy(),"modelsize")==.8F&&limb.getDeltaMovement().x==.25&&limb.getDeltaMovement().y==velocityY[index],"original detached limb texture, scale and launch impulse survive hosting");int count=named(c,"CREEPSEntityArmyGuyArm").size();MenusB173.withActor(c.player,soldier::aiStep);require(named(c,"CREEPSEntityArmyGuyArm").size()==count,"already-detached original limb does not duplicate");
                put(limb.legacy(),"lifetime",0);MenusB173.withActor(c.player,limb::tick);require(!limb.isRemoved()&&(Integer)get(limb.legacy(),"lifetime")==-1,"original postfix lifetime survives zero tick");MenusB173.withActor(c.player,limb::tick);require(limb.isRemoved(),"next original lifetime tick removes exact detached limb");
            }
            require(get(soldier.legacy(),"defaultHeldItem")==null,"original right-arm loss removes held weapon");
        }finally{blood.setBoolean(null,priorBlood);}
    }
}
