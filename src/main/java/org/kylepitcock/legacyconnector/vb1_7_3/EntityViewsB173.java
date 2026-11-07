package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.PathAwareEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Stable entity identities within callbacks, with weakly held ordinary views between callbacks. */
public final class EntityViewsB173 {
    private record HostState(double x,double y,double z,double vx,double vy,double vz,float yaw,float pitch,
            int health,int air,int fire,boolean ground,boolean dead,float width,float height,String texture,
            float sideways,float forward,boolean jumping,int hurt,int death,int age,float eye,float speed) {
        static HostState of(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity v) {
            return new HostState(v.x,v.y,v.z,v.velocityX,v.velocityY,v.velocityZ,v.yaw,v.pitch,v.health,v.air,v.fireTicks,
                    v.onGround,v.dead,v.width,v.height,v.mcc$texture(),v.mcc$sidewaysSpeed(),v.mcc$forwardSpeed(),v.mcc$jumping(),
                    v.hurtTime,v.deathTime,v.age,v.standingEyeHeight,v.mcc$movementSpeed());
        }
    }
    private record Touched(Entity view,double vx,double vy,double vz,boolean ground,Entity target,HostState hostState) {}
    private static final java.util.Map<net.minecraft.world.entity.Entity,Entity> VIEWS =
            new com.google.common.collect.MapMaker().weakKeys().weakValues().makeMap();
    private static final ThreadLocal<java.util.Map<net.minecraft.world.entity.Entity,Touched>> TOUCHED =
            ThreadLocal.withInitial(java.util.IdentityHashMap::new);
    public static final class ReadOnlyScope implements AutoCloseable {
        private final java.util.Map<net.minecraft.world.entity.Entity,Touched> previous;
        private ReadOnlyScope(){previous=TOUCHED.get();TOUCHED.set(new java.util.IdentityHashMap<>());}
        @Override public void close(){TOUCHED.set(previous);}
    }
    /** Render-time relation queries cannot retain touched simulation views or write them back. */
    public static ReadOnlyScope readOnly(){return new ReadOnlyScope();}
    public static Entity wrapReadOnly(net.minecraft.world.entity.Entity modern){try(var scope=readOnly()){return wrap(modern);}}

    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern == null) return null;
        if (modern instanceof B173LivingHost host && host.legacy()!=null) {
            if (host.callbackActive()) return host.legacy();
            if (!TOUCHED.get().containsKey(modern)) host.syncView();
            return touch(modern,host.legacy());
        }
        Touched touched=TOUCHED.get().get(modern);
        if(touched!=null)return touched.view;
        if (modern instanceof LegacyB173Arrow arrow && arrow.legacy != null) return touch(modern,arrow.legacy);
        Entity view=modern instanceof net.minecraft.world.entity.player.Player player ? PlayerViewB173.of(player) : VIEWS.computeIfAbsent(modern, EntityViewsB173::create);
        view.mcc$modern=modern;
        view.world=World.mcc$wrap(modern.level());
        if(!(modern instanceof net.minecraft.world.entity.player.Player))view.setPosition(modern.getX(),modern.getY(),modern.getZ());
        var b=modern.getBoundingBox();
        view.boundingBox=legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
        view.fireTicks=modern.getRemainingFireTicks();
        view.velocityX=modern.getDeltaMovement().x;view.velocityY=modern.getDeltaMovement().y;view.velocityZ=modern.getDeltaMovement().z;
        view.onGround=modern.onGround();view.dead=modern.isRemoved();
        view.blocksSameBlockSpawning=modern.blocksBuilding;
        view.yaw=modern.getYRot();view.pitch=modern.getXRot();
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living && modern instanceof net.minecraft.world.entity.LivingEntity nativeLiving)
            living.health=Math.round(nativeLiving.getHealth());
        // Register before resolving targets, which may point back to this entity.
        touch(modern,view);
        if(view instanceof PathAwareEntity creature && modern instanceof net.minecraft.world.entity.Mob mob) {
            creature.target=wrap(mob.getTarget());
            TOUCHED.get().put(modern,new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround,creature.target,null));
        }
        return view;
    }
    private static Entity create(net.minecraft.world.entity.Entity modern) {
        World world=World.mcc$wrap(modern.level());
        if(modern instanceof LegacyB173Cow cow && cow.legacy()!=null)return cow.legacy();
        if(modern instanceof net.minecraft.world.entity.player.Player p)return PlayerViewB173.of(p);
        if(modern instanceof net.minecraft.world.entity.item.ItemEntity item)
            return new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,modern.getX(),modern.getY(),modern.getZ(),StacksB173.toLegacy(item.getItem()));
        if(modern instanceof net.minecraft.world.entity.monster.Monster)return new legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.animal.Animal)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.PathfinderMob)return new PathAwareEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.LivingEntity)return new legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity(world) {};
        return new Entity(world) {};
    }
    private static Entity touch(net.minecraft.world.entity.Entity modern,Entity view) {
        TOUCHED.get().putIfAbsent(modern,new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround,view instanceof PathAwareEntity p?p.target:null,modern instanceof B173LivingHost host?HostState.of(host.legacy()):null));
        return view;
    }
    /** Commit pending cross-entity writes before the host refreshes its own callback view. */
    public static void prepareHostCallback(B173LivingHost host) {
        if(host.callbackActive()||host.legacy()==null)return;
        Touched touched=TOUCHED.get().remove(host.legacy().mcc$modern);
        if(touched!=null)flushHost(host,touched);
    }
    private static void flushHost(B173LivingHost host,Touched touched) {
        HostState before=touched.hostState;
        if(before==null)return;
        var v=host.legacy();HostState wanted=HostState.of(v);
        if(wanted.equals(before))return;
        // Refresh unchanged native fields so a read earlier in this callback cannot undo later damage or movement.
        host.syncView();
        if(wanted.x!=before.x)v.x=wanted.x;if(wanted.y!=before.y)v.y=wanted.y;if(wanted.z!=before.z)v.z=wanted.z;
        if(wanted.vx!=before.vx)v.velocityX=wanted.vx;if(wanted.vy!=before.vy)v.velocityY=wanted.vy;if(wanted.vz!=before.vz)v.velocityZ=wanted.vz;
        if(wanted.yaw!=before.yaw)v.yaw=wanted.yaw;if(wanted.pitch!=before.pitch)v.pitch=wanted.pitch;
        if(wanted.health!=before.health)v.health=wanted.health;if(wanted.air!=before.air)v.air=wanted.air;
        if(wanted.fire!=before.fire)v.fireTicks=wanted.fire;if(wanted.ground!=before.ground)v.onGround=wanted.ground;
        if(wanted.dead!=before.dead)v.dead=wanted.dead;
        if(wanted.hurt!=before.hurt)v.hurtTime=wanted.hurt;if(wanted.death!=before.death)v.deathTime=wanted.death;
        if(wanted.age!=before.age)v.age=wanted.age;
        v.mcc$movementInputs(wanted.sideways!=before.sideways?wanted.sideways:v.mcc$sidewaysSpeed(),
                wanted.forward!=before.forward?wanted.forward:v.mcc$forwardSpeed(),wanted.jumping!=before.jumping?wanted.jumping:v.mcc$jumping());
        host.flushView();
    }
    public static void flush() {
        var touchedViews=TOUCHED.get();
        try {
            for(var entry:java.util.List.copyOf(touchedViews.entrySet())) {
                var modern=entry.getKey();var touched=entry.getValue();Entity view=touched.view;
                if(modern instanceof B173LivingHost host) {if(!host.callbackActive())flushHost(host,touched);continue;}
                if(view.fireTicks>modern.getRemainingFireTicks())modern.setRemainingFireTicks(view.fireTicks);
                if(view.velocityX!=touched.vx||view.velocityY!=touched.vy||view.velocityZ!=touched.vz) {
                    modern.setDeltaMovement(view.velocityX,view.velocityY,view.velocityZ);modern.hurtMarked=true;
                }
                if(view.onGround!=touched.ground)modern.setOnGround(view.onGround);
                if(view instanceof PathAwareEntity p && p.target!=touched.target && modern instanceof net.minecraft.world.entity.Mob mob)
                    mob.setTarget(p.target!=null && p.target.mcc$modern instanceof net.minecraft.world.entity.LivingEntity target?target:null);
            }
        } finally {TOUCHED.remove();}
    }
}
