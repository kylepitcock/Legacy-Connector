package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original collision branches; private mod jars are runtime inputs only. */
public final class MoreCreepsRemainingSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsRemainingTest");
    private MoreCreepsRemainingSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS REMAINING TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static float number(Object object,String name)throws ReflectiveOperationException{return ((Number)field(object,name).get(object)).floatValue();}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:creepsremaining".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173CreepsRemain");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }

    private static Object get(Object object,String name)throws ReflectiveOperationException{return field(object,name).get(object);}
    private static final class FixedRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    private static final class Context implements AutoCloseable {
        final ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.server.level.ServerPlayer player;
        final Set<Integer> before=new HashSet<>();final net.minecraft.core.BlockPos origin;final List<Object> previousPlayers;
        Context(ServerLevel level,ContentB173.Registration registration)throws ReflectiveOperationException {
            this.level=level;this.registration=registration;world=World.mcc$wrap(level);for(var e:level.getAllEntities())before.add(e.getId());
            origin=level.getSharedSpawnPos().offset(8,36,8);level.getChunk(origin);player=actor(level);put(player,"spawnInvulnerableTime",0);player.setPos(origin.getX()+1.5,origin.getY(),origin.getZ()+.5);
            previousPlayers=world.players;world.players=new ArrayList<>();try(var scope=EntityViewsB173.callbackScope()){world.players.add(EntityViewsB173.wrap(player));}
        }
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LegacyB173Animal spawn(String name)throws ReflectiveOperationException {
            var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            require(world.spawnEntity(original)&&original.mcc$modern instanceof LegacyB173Animal,"original remaining-case spawn uses shared host: "+name);EntityViewsB173.flush();put(original,"random",new FixedRandom());
            require(level.getEntities((net.minecraft.world.entity.Entity)null,original.mcc$modern.getBoundingBox().inflate(.1)).contains(original.mcc$modern),"original enters loaded query sections");return (LegacyB173Animal)original.mcc$modern;
        }
        void movement(LegacyB173Animal host){try(var scope=EntityViewsB173.callbackScope()){EntityViewsB173.wrap(player);MenusB173.withActor(player,host::aiStep);}}
        @Override public void close(){EntityViewsB173.flush();world.players=previousPlayers;player.stopRiding();player.getInventory().clearContent();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var e:level.getAllEntities())if(!before.contains(e.getId()))added.add(e);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();}
    }
    private static void theft(Context c,int count,boolean damaged)throws ReflectiveOperationException {
        var item=damaged?net.minecraft.world.item.Items.IRON_SWORD:net.minecraft.world.item.Items.DIAMOND;
        var stack=new net.minecraft.world.item.ItemStack(item,count);if(damaged)stack.setDamageValue(7);c.player.getInventory().items.set(4,stack);
        var thief=c.spawn("CREEPSEntityThief");c.movement(thief);var original=thief.legacy();
        require((Boolean)get(original,"stolen")&&(Integer)get(original,"itemnumber")==4&&(Integer)get(original,"stolenamount")==1,"original thief steals one from actual nonselected inventory slot");
        require(c.player.getInventory().items.get(4).getCount()==count-1,"original thief slot null/count write reaches native inventory");
        var held=original.getHeldItem();require(held!=null&&held.itemId==StacksB173.toLegacy(stack).itemId,"original thief retains stolen item identity");
        require(held.count==(count==1?1:count-1),"original thief preserves shared-stack count alias quirk");
        var before=new HashSet<Integer>();for(var e:c.level.getAllEntities())before.add(e.getId());
        // Invoke native death, which must dispatch the actual onKilledBy restitution callback once.
        thief.invulnerableTime=0;thief.setHealth(1);MenusB173.withActor(c.player,()->thief.hurt(c.level.damageSources().playerAttack(c.player),100));
        var returned=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var e:c.level.getAllEntities())if(!before.contains(e.getId())&&e instanceof net.minecraft.world.entity.item.ItemEntity dropped&&dropped.getItem().is(item))returned.add(dropped);
        require(returned.size()==1&&returned.getFirst().getItem().getCount()==1,"actual thief death returns exactly stolen quantity through native item drop");
        require(returned.getFirst().getItem().getDamageValue()==(damaged?7:0),"actual thief restitution preserves original damage policy");
        require(c.player.getInventory().items.get(4).getCount()==count-1,"native death callback does not overwrite prior original inventory mutation");
    }
    private static void preacherContact(Context c)throws ReflectiveOperationException {
        var preacher=c.spawn("CREEPSEntityPreacher");put(preacher.legacy(),"getvictim",true);preacher.flushView();c.player.setDeltaMovement(0,0,0);float health=preacher.getHealth();
        require(preacher.hurt(c.level.damageSources().playerAttack(c.player),7),"actual captured-victim Preacher accepts contact damage");
        require(preacher.getHealth()==health-1,"original captured-victim contact subtracts exactly one health");
        var motion=c.player.getDeltaMovement();double expected=(double)(.5F*1.98F);require(Math.abs(motion.x-expected)<.000001&&Math.abs(motion.y-expected)<.000001&&Math.abs(motion.z-expected)<.000001,"original Preacher contact writes all three attacker velocity components through player view");
    }
    private static void preacherRelease(Context c)throws ReflectiveOperationException {
        var preacher=c.spawn("CREEPSEntityPreacher");
        try(var scope=EntityViewsB173.callbackScope()){put(preacher.legacy(),"victimEntity",EntityViewsB173.wrap(c.player));put(preacher.legacy(),"getvictim",true);put(preacher.legacy(),"raise",11);put(preacher.legacy(),"raiselevel",10);preacher.flushView();}
        c.player.setDeltaMovement(.3,.4,.5);c.movement(preacher);
        require(!(Boolean)get(preacher.legacy(),"getvictim")&&!(Boolean)get(preacher.legacy(),"ritual"),"original Preacher release ends ritual after raise limit");
        require(Math.abs(c.player.getDeltaMovement().y-(-.800000011920929))<.000001,"retained original victim reference receives Preacher release downward velocity");
    }
    private static void mountedBullet(Context c)throws ReflectiveOperationException {
        var mount=new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW,c.level);mount.setNoAi(true);mount.setPos(c.origin.getX()+.5,c.origin.getY(),c.origin.getZ()+.5);require(c.level.addFreshEntity(mount),"native bullet mount enters loaded sections");
        require(c.player.startRiding(mount,true),"controlled real player mounts native cow");float health=mount.getHealth();Entity projectile;
        try(var scope=EntityViewsB173.callbackScope()){
            var owner=(LivingEntity)EntityViewsB173.wrap(c.player);
            projectile=(Entity)c.type("CREEPSEntityBullet").getConstructor(World.class,LivingEntity.class,float.class).newInstance(c.world,owner,0F);
            projectile.setPosition(mount.getX(),mount.getY()+.5,mount.getZ()-1.5);projectile.velocityX=0;projectile.velocityY=0;projectile.velocityZ=2;
            require(c.world.spawnEntity(projectile)&&projectile.mcc$modern instanceof B173EntityHost,"actual Bullet uses generic neutral host");
        }
        MenusB173.withActor(c.player,projectile.mcc$modern::tick);
        require(mount.getHealth()==health&&!projectile.mcc$modern.isRemoved(),"actual first-tick Bullet skips original shooter's native vehicle");
        try(var scope=EntityViewsB173.callbackScope()){
            var owner=EntityViewsB173.wrap(c.player);var vehicle=EntityViewsB173.wrap(mount);
            require(owner.vehicle==vehicle&&vehicle.passenger==owner,"native rider/vehicle views retain exact reciprocal identities without recursion");
        }
        c.player.stopRiding();
        try(var scope=EntityViewsB173.callbackScope()){
            require(EntityViewsB173.wrap(c.player).vehicle==null&&EntityViewsB173.wrap(mount).passenger==null,"native dismount refresh clears both relation views");
        }
    }
    private static void run(ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getSimpleName().equals("CREEPSEntityThief")&&r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var failures=new ArrayList<String>();int passed=0;
        for(String name:List.of("partial theft","full theft","damaged restitution","Preacher contact","Preacher release","mounted Bullet"))try(var c=new Context(level,registration.get())){
            switch(name){case "partial theft"->theft(c,5,false);case "full theft"->theft(c,1,false);case "damaged restitution"->theft(c,1,true);case "Preacher contact"->preacherContact(c);case "Preacher release"->preacherRelease(c);default->mountedBullet(c);}
            passed++;LOG.info("B1.7.3 CREEPS REMAINING TEST: {} original specialized branch passed",name);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(name+": "+error);LOG.error("B1.7.3 CREEPS REMAINING TEST failed: {}",name,error);}
        require(failures.isEmpty(),"B1.7.3 CREEPS REMAINING TEST: "+failures);LOG.info("B1.7.3 CREEPS REMAINING TEST: all {} actual specialized branches passed",passed);
    }
}
