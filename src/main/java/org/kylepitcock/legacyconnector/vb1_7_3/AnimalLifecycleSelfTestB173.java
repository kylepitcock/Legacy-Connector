package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual archived callbacks, deliberately named only in this regression fixture. */
public final class AnimalLifecycleSelfTestB173 {
    private AnimalLifecycleSelfTestB173() {}
    public static void register(IEventBus bus) {
        if(Boolean.getBoolean("legacyconnector.entitySelfTest"))
            bus.addListener((ServerStartedEvent e)->run(e.getServer().overworld()));
    }
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("B1.7.3 ANIMAL LIFECYCLE TEST: "+message);
    }
    private static Field field(Object value,String name)throws ReflectiveOperationException {
        for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try {
            var f=c.getDeclaredField(name);f.setAccessible(true);return f;
        }catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static void put(Object value,String name,Object data)throws ReflectiveOperationException {field(value,name).set(value,data);}
    private static Object get(Object value,String name)throws ReflectiveOperationException {return field(value,name).get(value);}
    private static Object call(Object value,String name,Class<?>[] types,Object... args)throws ReflectiveOperationException {
        for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try {
            Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(value,args);
        }catch(NoSuchMethodException ignored){}
        throw new NoSuchMethodException(name);
    }
    private static final class ZeroRandom extends Random {
        @Override public int nextInt(int bound){return 0;}
        @Override public float nextFloat(){return .5F;}
    }
    private static LivingEntity spawn(Class<?> type,World world,double x,double y,double z,ArrayList<net.minecraft.world.entity.Entity> cleanup)throws ReflectiveOperationException {
        var original=(LivingEntity)type.getConstructor(World.class).newInstance(world);
        original.setPosition(x,y,z);
        require(world.spawnEntity(original),type.getSimpleName()+" original spawn");
        require(original.mcc$modern instanceof Mob&&original.mcc$modern instanceof B173LivingHost,"neutral living host");
        cleanup.add(original.mcc$modern);
        require(EntityViewsB173.wrap(original.mcc$modern)==original,"host view preserves original identity");EntityViewsB173.flush();
        put(original,"random",new ZeroRandom());return original;
    }
    private static Mob reload(Mob host,ArrayList<net.minecraft.world.entity.Entity> cleanup) {
        var tag=new net.minecraft.nbt.CompoundTag();require(host.save(tag),"registered host saves");
        var copy=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,host.level(),e->e);
        require(copy instanceof Mob&&copy instanceof B173LivingHost&&copy.getType()==host.getType(),"registry reload creates same native type");
        cleanup.add(copy);return (Mob)copy;
    }
    private static void run(net.minecraft.server.level.ServerLevel level) {
        for(var registration:ContentB173.entities()) {
            String name=registration.type().getSimpleName();
            if(!name.equals("EntityBunny")&&!name.equals("CREEPSEntityHippo"))continue;
            var cleanup=new ArrayList<net.minecraft.world.entity.Entity>();
            try {
                var world=World.mcc$wrap(level);var pos=level.getSharedSpawnPos().above(12);
                var original=spawn(registration.type(),world,pos.getX()+128.5,pos.getY(),pos.getZ()+128.5,cleanup);
                if(name.equals("EntityBunny"))bunny(original,world,level,cleanup);
                else hippo(original,world,level,cleanup);
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173AnimalTest").info("B1.7.3 ANIMAL LIFECYCLE TEST: original {} callbacks passed",name);
            }catch(ReflectiveOperationException e){throw new IllegalStateException("B1.7.3 ANIMAL LIFECYCLE TEST: "+name,e);}
            finally {for(var e:cleanup)e.discard();EntityViewsB173.flush();}
        }
    }
    private static void bunny(LivingEntity original,World world,net.minecraft.server.level.ServerLevel level,ArrayList<net.minecraft.world.entity.Entity> cleanup)throws ReflectiveOperationException {
        var mob=(LegacyB173Animal)original.mcc$modern;var host=(B173LivingHost)mob;
        put(original,"adult",false);put(original,"edad",.5F);put(original,"h",false);
        mob.setOnGround(true);mob.setDeltaMovement(.1,0,0);
        double x=mob.getX(),y=mob.getY();int ticks=host.baseTickCount(),movement=host.baseMovementCount(),ai=host.baseAICount();
        mob.tick();
        require(Math.abs((Float)get(original,"edad")-.51F)<.0001,"original juvenile growth callback");
        require(host.baseTickCount()==ticks+1&&host.baseMovementCount()==movement+1&&host.baseAICount()==ai+1,"one superclass tick/movement/AI phase");
        var preTravel=(net.minecraft.world.phys.Vec3)call(mob,"lastPreTravelVelocity",new Class<?>[]{});
        require(Math.abs(preTravel.y-.45)<.00001,"original hop .45 is present before native travel");
        require(mob.getY()>y+.25&&mob.getY()<y+.6,"original .45 hop reaches physics");
        require(Math.abs(mob.getX()-x)<.19,"movement is not applied twice");
        put(original,"h",true);mob.setOnGround(false);mob.setDeltaMovement(.1,.2,0);
        ticks=host.baseTickCount();movement=host.baseMovementCount();ai=host.baseAICount();y=mob.getY();
        mob.tick();
        require(host.baseTickCount()==ticks+1&&host.baseMovementCount()==movement+1&&host.baseAICount()==ai,"airborne original h branch skips superclass AI only");
        require(mob.getY()>y+.1&&mob.getY()<y+.3,"skipped AI retains exactly one physics displacement");
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous=player.getMainHandItem().copy();var previousMotion=player.getDeltaMovement();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);player.setDeltaMovement(.12,0,.08);
            require(mob.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction(),"native interaction executes original callback");
            require(mob.getVehicle()==player&&original.vehicle!=null&&original.vehicle.mcc$modern==player&&(Boolean)get(original,"tamed"),"original mounting and taming synchronize");
            var playerView=original.vehicle;
            mob.stopRiding();host.syncView();
            require(original.vehicle==null&&playerView.passenger==null,"native dismount clears original vehicle/passenger identity");
            require(mob.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction()&&mob.getVehicle()==player&&original.vehicle==playerView,
                    "original interaction remounts after native dismount");
            mob.mobInteract(player,InteractionHand.MAIN_HAND);
            require(mob.getVehicle()==null&&original.vehicle==null&&(Boolean)get(original,"h"),"original second interaction dismounts");
            require(Math.abs(mob.getDeltaMovement().x-.6)<.0001&&Math.abs(mob.getDeltaMovement().y-.5)<.0001,"original thrown velocity synchronizes");
        }finally {mob.stopRiding();player.setItemInHand(InteractionHand.MAIN_HAND,previous);player.setDeltaMovement(previousMotion);}
        call(original,"setType",new Class<?>[]{int.class},3);put(original,"adult",false);put(original,"edad",.73F);
        var copy=reload(mob,cleanup);var restored=((B173LivingHost)copy).legacy();
        require((Integer)get(restored,"typeint")==3&&(Boolean)get(restored,"tamed")&&!(Boolean)get(restored,"adult")&&Math.abs((Float)get(restored,"edad")-.73F)<.0001,"original NBT type/tame/adult/growth survives registry reload");
        float health=mob.getHealth();require(mob.hurt(level.damageSources().generic(),1)&&mob.getHealth()==health-1,"native damage synchronizes");
        mob.invulnerableTime=0;require(original.damage(null,1)&&mob.getHealth()==health-2,"legacy superclass damage reaches native base once");
        // Keep both query boxes identical in membership: the original bytecode indexes
        // the narrow list using the broad list's size (an unrelated original bug).
        mob.setPos(original.x,original.y,original.z);mob.setDeltaMovement(0,0,0);
        var mate=spawn(original.getClass(),world,mob.getX()+.5,mob.getY(),mob.getZ(),cleanup);
        var mateMob=(LegacyB173Animal)mate.mcc$modern;put(mate,"h",true);mateMob.setOnGround(false);
        var pending=EntityViewsB173.wrap(mateMob);pending.velocityX=.2;((LivingEntity)pending).health=3;
        double mateX=mateMob.getX();mateMob.tick();
        require(mateMob.getHealth()==3&&mate.health==3,"pending other-host health survives its callback synchronization");
        require(mateMob.getX()>mateX+.1&&mateMob.getX()<mateX+.3,"pending other-host velocity reaches its next physics phase");
        for(var parent:new LivingEntity[]{original,mate}) {put(parent,"adult",true);put(parent,"tamed",true);put(parent,"h",false);put(parent,"j",1023);put(parent,"i",127);}
        require(world.getEntities(original,original.boundingBox.expand(1,1,1)).contains(mate)&&world.entities.contains(mate),"nearby and live-list original identity");
        var before=level.getEntities(mob,mob.getBoundingBox().inflate(2));mob.tick();
        var babies=level.getEntities(mob,mob.getBoundingBox().inflate(2)).stream().filter(e->!before.contains(e)&&e instanceof B173LivingHost h&&h.legacy().getClass()==original.getClass()).toList();
        require(babies.size()==1,"original breeding callback spawns one offspring");cleanup.addAll(babies);
        require(!(Boolean)get(((B173LivingHost)babies.getFirst()).legacy(),"adult"),"original offspring juvenile state");
    }
    private static void hippo(LivingEntity original,World world,net.minecraft.server.level.ServerLevel level,ArrayList<net.minecraft.world.entity.Entity> cleanup)throws ReflectiveOperationException {
        var mob=(LegacyB173Animal)original.mcc$modern;var host=(B173LivingHost)mob;
        int ticks=host.baseTickCount();mob.tick();require(host.baseTickCount()==ticks+1,"Hippo original tick delegates once");
        var victim=new net.minecraft.world.entity.npc.Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);victim.setPos(mob.getX()+1,mob.getY(),mob.getZ());require(level.addFreshEntity(victim),"target spawn");cleanup.add(victim);
        var target=EntityViewsB173.wrap(victim);EntityViewsB173.flush();
        require(call(original,"func_21018_getClosestTarget",new Class<?>[]{Entity.class,double.class},original,15D)==target,"original target query preserves visible identity");
        var wall=new java.util.HashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
        victim.setPos(mob.getX()+6,mob.getY(),mob.getZ());
        var middle=mob.blockPosition().offset(3,0,0);
        try {
            for(int dy=-1;dy<=5;dy++)for(int dz=-2;dz<=2;dz++) {
                var block=middle.offset(0,dy,dz);wall.put(block,level.getBlockState(block));
                level.setBlock(block,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            }
            require(call(original,"func_21018_getClosestTarget",new Class<?>[]{Entity.class,double.class},original,15D)==null,"original target query rejects occluded victim");
        }finally {wall.forEach((block,state)->level.setBlock(block,state,3));victim.setPos(mob.getX()+1,mob.getY(),mob.getZ());}
        float hp=mob.getHealth();put(original,"hungry",true);
        require(mob.hurt(level.damageSources().mobAttack(victim),2)&&mob.getHealth()==hp-2&&!(Boolean)get(original,"hungry"),"original native damage callback and superclass damage once");
        require(get(original,"target")==target,"original retaliation retains attacker identity");
        mob.invulnerableTime=0;require(original.damage(target,1)&&mob.getHealth()==hp-3,"Hippo super.damage avoids reentry");
        host.syncView();original.onGround=true;float victimHealth=victim.getHealth();
        call(original,"attack",new Class<?>[]{Entity.class,float.class},target,1F);host.flushView();EntityViewsB173.flush();
        require(victim.getHealth()<victimHealth&&original.velocityY>.09,"original attack damages and lunges");
        put(original,"hungry",true);var copy=reload(mob,cleanup);
        require((Boolean)get(((B173LivingHost)copy).legacy(),"hungry"),"original Hungry NBT survives registry factory");
        var waterPos=mob.blockPosition();var previous=level.getBlockState(waterPos);
        try {
            level.setBlock(waterPos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);
            host.syncView();call(original,"tickLiving",new Class<?>[]{});
            require(Math.abs(original.standingEyeHeight+.76F)<.0001,"original water eye-offset callback");
            require((Boolean)call(world,"isBoxSubmergedInFluid",new Class<?>[]{legacyconnector.vb1_7_3.net.minecraft.util.math.Box.class},original.boundingBox),"water volume API");
        }finally {level.setBlock(waterPos,previous,3);}
        put(original,"random",new ZeroRandom());mob.invulnerableTime=0;
        var beforeDrops=level.getEntities(mob,mob.getBoundingBox().inflate(4));
        require(mob.hurt(level.damageSources().mobAttack(victim),10000)&&mob.isDeadOrDying(),"native lethal damage executes original death callback");
        var drops=level.getEntities(mob,mob.getBoundingBox().inflate(4)).stream()
                .filter(e->!beforeDrops.contains(e)&&e instanceof net.minecraft.world.entity.item.ItemEntity)
                .map(e->(net.minecraft.world.entity.item.ItemEntity)e).toList();
        cleanup.addAll(drops);
        require(drops.size()==2&&drops.stream().filter(e->e.getItem().is(net.minecraft.world.item.Items.PORKCHOP)&&e.getItem().getCount()==1).count()==1
                &&drops.stream().filter(e->e.getItem().is(net.minecraft.world.item.Items.SUGAR_CANE)&&e.getItem().getCount()==1).count()==1,
                "original deterministic death callback spawns one porkchop and one sugar cane");
    }
}
