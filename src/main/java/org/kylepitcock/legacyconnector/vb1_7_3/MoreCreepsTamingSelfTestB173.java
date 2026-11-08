package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controlled actual MoreCreeps domestication and progression callbacks. No production behavior is selected by these names. */
public final class MoreCreepsTamingSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsTamingTest");
    private MoreCreepsTamingSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS TAMING TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {return field(object,name).get(object);}
    private static Object call(Object object,String name,Class<?>[] signature,Object... args)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var method=type.getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(object,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
    private static final class ZeroRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    /** FakePlayer deliberately disables riding, menus and advancements; these proofs need normal native behavior. */
    private static net.minecraft.server.level.ServerPlayer actor(net.minecraft.server.level.ServerLevel level,int index){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("legacyconnector:creeps-taming:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173Feature"+index);
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
        net.minecraft.advancements.AdvancementHolder achievement(String name)throws ReflectiveOperationException {var achievement=(legacyconnector.vb1_7_3.net.minecraft.achievement.Achievement)registration.mod().instance.getClass().getField(name).get(null);var nativeAchievement=level.getServer().getAdvancements().get(AdvancementsB173.idOf(achievement));require(nativeAchievement!=null,"original achievement loaded from native pack: "+name);player.getAdvancements().revoke(nativeAchievement,AdvancementsB173.CRITERION);return nativeAchievement;}
        @Override public void close(){MenusB173.withActor(player,()->legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().setScreen(null));player.stopRiding();player.closeContainer();EntityViewsB173.flush();var descendants=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!baseline.contains(entity.getId()))descendants.add(entity);for(var entity:descendants)entity.discard();for(var entity:entities)entity.discard();player.getInventory().clearContent();player.getInventory().load(inventory);player.getInventory().selected=selected;player.setPos(previousPosition.x,previousPosition.y,previousPosition.z);player.setDeltaMovement(previousMotion);world.random=random;player.setShiftKeyDown(shifted);EntityViewsB173.flush();player.discard();}
    }
    @FunctionalInterface private interface Proof {void run(Context c)throws ReflectiveOperationException;}
    private record Case(String name,Proof proof){}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var cases=List.of(new Case("camel cookie domestication and riding",MoreCreepsTamingSelfTestB173::camel),new Case("guinea pig food carry and commands",MoreCreepsTamingSelfTestB173::guineaPig),new Case("guinea pig level-five progression",MoreCreepsTamingSelfTestB173::progression),new Case("hotdog pork domestication",MoreCreepsTamingSelfTestB173::hotdog));
        var failures=new ArrayList<String>();int index=0;
        for(var proof:cases)try(var c=new Context(level,registration.get(),index++)){proof.proof.run(c);LOG.info("B1.7.3 CREEPS TAMING TEST: {} passed using actual original callbacks",proof.name);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(proof.name+": "+error);LOG.error("B1.7.3 CREEPS TAMING TEST failed: {}",proof.name,error);}
        require(failures.isEmpty(),"B1.7.3 CREEPS TAMING TEST: "+failures);LOG.info("B1.7.3 CREEPS TAMING TEST: all {} specialized original-callback proofs passed",cases.size());
    }
    private static void hold(Context c,net.minecraft.world.item.Item item,int count){EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(item,count));}
    private static void interact(Context c,LegacyB173Animal host){require(host.mobInteract(c.player,InteractionHand.MAIN_HAND).consumesAction(),"actual original interaction handled: "+host.legacy().getClass().getSimpleName());}
    private static void awarded(Context c,net.minecraft.advancements.AdvancementHolder achievement){require(c.player.getAdvancements().getOrStartProgress(achievement).isDone(),"original callback awards native advancement "+achievement.id());}
    private static void camel(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("achievecamel");var camel=c.spawn("CREEPSEntityCamel");put(camel.legacy(),"tamedcookies",2);put(camel.legacy(),"tamed",false);camel.setHealth(1);hold(c,net.minecraft.world.item.Items.COOKIE,3);
        interact(c,camel);require(!(Boolean)get(camel.legacy(),"tamed")&&(Integer)get(camel.legacy(),"tamedcookies")==1&&camel.getHealth()==Math.min(11,(Integer)get(camel.legacy(),"basehealth")),"first cookie heals and advances original countdown without premature taming");
        interact(c,camel);require((Boolean)get(camel.legacy(),"tamed")&&(Integer)get(camel.legacy(),"tamedcookies")==0&&((LivingEntity)get(camel.legacy(),"owner")).mcc$modern==c.player&&!((String)get(camel.legacy(),"name")).isEmpty(),"last cookie tames, names and assigns exact native owner");
        // Actual Camel.interact never decrements the cookie stack; preserve that original policy.
        require(c.player.getMainHandItem().getCount()==3,"actual camel cookie interaction retains original unconsumed stack policy");awarded(c,achievement);EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);interact(c,camel);require(c.player.getVehicle()==camel&&camel.legacy().passenger.mcc$modern==c.player,"original empty-hand riding mounts exact native player");c.player.stopRiding();
    }
    private static void guineaPig(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("achievepigtaming");var pig=c.spawn("CREEPSEntityGuineaPig");put(pig.legacy(),"interest",0);put(pig.legacy(),"tamed",false);hold(c,net.minecraft.world.item.Items.GOLDEN_APPLE,2);interact(c,pig);
        require((Boolean)get(pig.legacy(),"tamed")&&(Integer)get(pig.legacy(),"interest")==100&&c.player.getMainHandItem().getCount()==1&&!((String)get(pig.legacy(),"name")).isEmpty(),"golden apple consumes one and crosses original interest threshold into named tame state");awarded(c,achievement);
        hold(c,net.minecraft.world.item.Items.DANDELION,2);put(pig.legacy(),"wanderstate",0);interact(c,pig);require((Integer)get(pig.legacy(),"wanderstate")==1&&pig.legacy().mcc$movementSpeed()==0&&c.player.getMainHandItem().getCount()==2,"original flower command enters stay without consuming flower");interact(c,pig);require((Integer)get(pig.legacy(),"wanderstate")==2,"second flower command enables follow");interact(c,pig);require((Integer)get(pig.legacy(),"wanderstate")==0,"third flower command restores fight/follow");
        put(pig.legacy(),"speedboost",-1);hold(c,net.minecraft.world.item.Items.SUGAR_CANE,2);interact(c,pig);require((Integer)get(pig.legacy(),"speedboost")==13000&&Math.abs(pig.legacy().mcc$movementSpeed()-((Float)get(pig.legacy(),"baseSpeed")+.5F))<.0001&&c.player.getMainHandItem().getCount()==1,"original sugar-cane boost resets expired timer, adds13000 and consumes one");
        EntityViewsB173.flush();c.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        // Empty-hand carrying mutates relations then falls through to the original false return.
        pig.mobInteract(c.player,InteractionHand.MAIN_HAND);require(pig.getVehicle()==c.player,"original tame pig carry mounts pig on native player");pig.mobInteract(c.player,InteractionHand.MAIN_HAND);require(pig.getVehicle()==null&&(Integer)get(pig.legacy(),"unmounttimer")==20,"original second carry interaction releases with cooldown");
        var top=c.spawn("CREEPSEntityGuineaPig");put(top.legacy(),"tamed",true);pig.mobInteract(c.player,InteractionHand.MAIN_HAND);top.mobInteract(c.player,InteractionHand.MAIN_HAND);
        require(pig.getVehicle()==c.player&&top.getVehicle()==pig&&c.player.getFirstPassenger()==pig&&pig.getFirstPassenger()==top,"actual original carry branches build native two-pig stack");
        top.fallDistance=7;EntityViewsB173.flush();pig.mobInteract(c.player,InteractionHand.MAIN_HAND);
        require(pig.getVehicle()==c.player&&top.getVehicle()==null&&pig.getFirstPassenger()==null&&(Integer)get(top.legacy(),"unmounttimer")==20,"actual lower-pig interaction releases top of stack only and applies cooldown");
        require(top.fallDistance==-25F,"original stack-release fallDistance=-25 reaches exact native top pig: got="+top.fallDistance);
    }
    private static void progression(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("achievepiglevel5");var pig=c.spawn("CREEPSEntityGuineaPig");put(pig.legacy(),"tamed",true);put(pig.legacy(),"level",4);var thresholds=pig.legacy().getClass().getDeclaredField("leveldamage");thresholds.setAccessible(true);var threshold=(int[])thresholds.get(null);put(pig.legacy(),"totaldamage",(float)threshold[4]);put(pig.legacy(),"totalexperience",0);float speed=(Float)get(pig.legacy(),"baseSpeed");int health=(Integer)get(pig.legacy(),"basehealth");
        var victim=new net.minecraft.world.entity.npc.Villager(net.minecraft.world.entity.EntityType.VILLAGER,c.level);victim.setNoAi(true);victim.setPos(pig.getX()+1,pig.getY(),pig.getZ());require(c.level.addFreshEntity(victim),"progression target enters native query sections");c.entities.add(victim);
        MenusB173.withActor(c.player,()->{try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,pig.legacy());var scope=EntityViewsB173.callbackScope()){try{call(pig.legacy(),"attack",new Class<?>[]{legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,float.class},EntityViewsB173.wrap(victim),1F);pig.flushView();}catch(ReflectiveOperationException error){throw new RuntimeException(error);}}});
        require((Integer)get(pig.legacy(),"level")==5&&(Float)get(pig.legacy(),"totaldamage")==0F&&(Integer)get(pig.legacy(),"totalexperience")==1&&Math.abs((Float)get(pig.legacy(),"baseSpeed")-speed-.025F)<.0001&&(Integer)get(pig.legacy(),"basehealth")==health+1,"actual attack callback crosses level threshold, resets damage and increases experience/speed/health");awarded(c,achievement);
    }
    private static void hotdog(Context c)throws ReflectiveOperationException {
        var achievement=c.achievement("achievehotdogtaming");var dog=c.spawn("CREEPSEntityHotdog");put(dog.legacy(),"interest",90);put(dog.legacy(),"tamed",false);hold(c,net.minecraft.world.item.Items.PORKCHOP,2);interact(c,dog);require((Boolean)get(dog.legacy(),"tamed")&&(Integer)get(dog.legacy(),"interest")==100&&((LivingEntity)get(dog.legacy(),"owner")).mcc$modern==c.player&&c.player.getMainHandItem().getCount()==1,"original pork threshold consumes one, tames and retains exact native owner");awarded(c,achievement);
    }
}
