package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.PathAwareEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Stable entity identities within callbacks, with weakly held ordinary views between callbacks. */
public final class EntityViewsB173 {
    record Placement(double x,double y,double z,float yaw,float pitch) {
        static Placement of(Entity view){return new Placement(view.x,view.y,view.z,view.yaw,view.pitch);}
        void apply(Entity view,net.minecraft.world.entity.Entity modern){
            boolean px=view.x!=x,py=view.y!=y,pz=view.z!=z,ry=view.yaw!=yaw,rp=view.pitch!=pitch;
            if(!px&&!py&&!pz&&!ry&&!rp)return;
            double nx=px?view.x:modern.getX(),ny=py?view.y-view.standingEyeHeight:modern.getY(),nz=pz?view.z:modern.getZ();
            float nextYaw=ry?view.yaw:modern.getYRot(),nextPitch=rp?view.pitch:modern.getXRot();
            if(nx==modern.getX()&&ny==modern.getY()&&nz==modern.getZ()&&nextYaw==modern.getYRot()&&nextPitch==modern.getXRot())return;
            if(modern instanceof net.minecraft.server.level.ServerPlayer player&&!player.isPassenger()&&player.connection!=null)
                player.connection.teleport(nx,ny,nz,nextYaw,nextPitch);
            else{modern.setPos(nx,ny,nz);modern.setYRot(nextYaw);modern.setXRot(nextPitch);}
        }
    }
    private record HostState(double x,double y,double z,double vx,double vy,double vz,float yaw,float pitch,
            int health,int air,int fire,boolean ground,boolean dead,float width,float height,String texture,
            float sideways,float forward,boolean jumping,int hurt,int death,int age,float eye,float speed,float fall,Entity vehicle,Entity passenger) {
        static HostState of(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity v) {
            return new HostState(v.x,v.y,v.z,v.velocityX,v.velocityY,v.velocityZ,v.yaw,v.pitch,v.health,v.air,v.fireTicks,
                    v.onGround,v.dead,v.width,v.height,v.mcc$texture(),v.mcc$sidewaysSpeed(),v.mcc$forwardSpeed(),v.mcc$jumping(),
                    v.hurtTime,v.deathTime,v.age,v.standingEyeHeight,v.mcc$movementSpeed(),v.mcc$fallDistance(),v.vehicle,v.passenger);
        }
    }
    private record Extra(int itemAge,int pickupDelay,net.minecraft.world.item.ItemStack stack,int fuse,Entity owner) {}
    private record NeutralState(double x,double y,double z,double vx,double vy,double vz,float yaw,float pitch,
            int air,int fire,boolean ground,boolean dead,float width,float height,float eye,float fall,
            boolean noClip,float step,boolean blocks,Entity vehicle,Entity passenger) {
        static NeutralState of(Entity v){
            return new NeutralState(v.x,v.y,v.z,v.velocityX,v.velocityY,v.velocityZ,v.yaw,v.pitch,v.air,v.fireTicks,
                    v.onGround,v.dead,v.width,v.height,v.standingEyeHeight,v.mcc$fallDistance(),v.noClip,v.stepHeight,
                    v.blocksSameBlockSpawning,v.vehicle,v.passenger);
        }
        void merge(Entity v,NeutralState before){
            if(x!=before.x)v.x=x;if(y!=before.y)v.y=y;if(z!=before.z)v.z=z;
            if(vx!=before.vx)v.velocityX=vx;if(vy!=before.vy)v.velocityY=vy;if(vz!=before.vz)v.velocityZ=vz;
            if(yaw!=before.yaw)v.yaw=yaw;if(pitch!=before.pitch)v.pitch=pitch;
            if(air!=before.air)v.air=air;if(fire!=before.fire)v.fireTicks=fire;
            if(ground!=before.ground)v.onGround=ground;if(dead!=before.dead)v.dead=dead;
            if(width!=before.width)v.width=width;if(height!=before.height)v.height=height;
            if(eye!=before.eye)v.standingEyeHeight=eye;if(fall!=before.fall)v.mcc$fallDistance(fall);
            if(noClip!=before.noClip)v.noClip=noClip;if(step!=before.step)v.stepHeight=step;
            if(blocks!=before.blocks)v.blocksSameBlockSpawning=blocks;
            if(vehicle!=before.vehicle)v.vehicle=vehicle;if(passenger!=before.passenger)v.passenger=passenger;
        }
    }
    private static Extra extra(Entity view){
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item)return new Extra(item.itemAge,item.pickupDelay,item.stack==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(item.stack).copy(),0,null);
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity creeper)return new Extra(0,0,net.minecraft.world.item.ItemStack.EMPTY,creeper.fuseTime,null);
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity arrow)return new Extra(0,0,net.minecraft.world.item.ItemStack.EMPTY,0,arrow.owner);
        return null;
    }
    private record Touched(Entity view,double vx,double vy,double vz,boolean ground,Entity target,HostState hostState,NeutralState neutralState,Extra extra,Placement placement) {}
    private static final java.util.Map<net.minecraft.world.entity.Entity,Entity> VIEWS =
            new com.google.common.collect.MapMaker().weakKeys().weakValues().makeMap();
    private static final ThreadLocal<java.util.Map<net.minecraft.world.entity.Entity,Touched>> TOUCHED =
            ThreadLocal.withInitial(java.util.IdentityHashMap::new);
    private static final ThreadLocal<Integer> CALLBACK_DEPTH=ThreadLocal.withInitial(()->0);
    /** Nested native callbacks share one original-code transaction; later writes retain the same entity identities. */
    public static final class CallbackScope implements AutoCloseable {
        private boolean closed;
        private CallbackScope(){CALLBACK_DEPTH.set(CALLBACK_DEPTH.get()+1);}
        @Override public void close(){if(closed)return;closed=true;int depth=CALLBACK_DEPTH.get()-1;if(depth==0){CALLBACK_DEPTH.remove();flush();}else CALLBACK_DEPTH.set(depth);}
    }
    public static CallbackScope callbackScope(){return new CallbackScope();}
    /** Retained original references are live entity fields, even when no World query runs this tick. */
    private static final ClassValue<java.util.List<java.lang.reflect.Field>> ENTITY_REFERENCES=new ClassValue<>(){
        @Override protected java.util.List<java.lang.reflect.Field> computeValue(Class<?> type){
            var fields=new java.util.ArrayList<java.lang.reflect.Field>();
            try{
                for(Class<?> c=type;c!=null&&Entity.class.isAssignableFrom(c);c=c.getSuperclass()){
                    for(var field:c.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&Entity.class.isAssignableFrom(field.getType())){field.setAccessible(true);fields.add(field);}
                }
                return java.util.List.copyOf(fields);
            }catch(RuntimeException|LinkageError error){throw new IllegalStateException("Cannot inspect retained Beta entity references of "+type.getName(),error);}
        }
    };
    public static void refreshReferences(Entity original){
        try{
            for(var field:ENTITY_REFERENCES.get(original.getClass())){
                Entity previous=(Entity)field.get(original);if(previous==null||previous.mcc$modern==null)continue;
                Entity canonical=wrap(previous.mcc$modern);
                if(canonical!=previous&&field.getType().isInstance(canonical))field.set(original,canonical);
            }
        }catch(IllegalAccessException error){throw new IllegalStateException("Cannot refresh retained Beta entity references",error);}
    }
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
        if (modern instanceof B173EntityHost host && host.legacy()!=null) {
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
        view.width=modern.getBbWidth();view.height=modern.getBbHeight();
        if(!(modern instanceof net.minecraft.world.entity.player.Player))view.setPosition(modern.getX(),modern.getY()+view.standingEyeHeight,modern.getZ());
        var b=modern.getBoundingBox();
        view.boundingBox=legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
        view.fireTicks=modern.getRemainingFireTicks();
        if(!(modern instanceof net.minecraft.world.entity.player.Player)){
            view.velocityX=modern.getDeltaMovement().x;view.velocityY=modern.getDeltaMovement().y;view.velocityZ=modern.getDeltaMovement().z;
        }
        view.onGround=modern.onGround();view.dead=modern.isRemoved();view.mcc$environment(modern.isInWater());
        view.blocksSameBlockSpawning=modern.blocksBuilding;
        view.yaw=modern.getYRot();view.pitch=modern.getXRot();
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living && modern instanceof net.minecraft.world.entity.LivingEntity nativeLiving)
            living.health=Math.round(nativeLiving.getHealth());
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item&&modern instanceof net.minecraft.world.entity.item.ItemEntity nativeItem){
            item.stack=StacksB173.toLegacy(nativeItem.getItem());item.itemAge=EntityFieldsB173.itemAge(nativeItem);item.pickupDelay=EntityFieldsB173.pickupDelay(nativeItem);
        }
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity creeper&&modern instanceof net.minecraft.world.entity.monster.Creeper nativeCreeper)creeper.fuseTime=EntityFieldsB173.creeperFuse(nativeCreeper);
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.passive.SquidEntity squid&&modern instanceof net.minecraft.world.entity.animal.Squid nativeSquid){
            // Read snapshots only: native animation and swimming remain authoritative on ordinary native entities.
            squid.tiltAngle=nativeSquid.xBodyRot;squid.lastTiltAngle=nativeSquid.xBodyRotO;
            squid.rollAngle=nativeSquid.zBodyRot;squid.lastRollAngle=nativeSquid.zBodyRotO;
            squid.thrustTimer=nativeSquid.tentacleMovement;squid.lastThrustTimer=nativeSquid.oldTentacleMovement;
            squid.tentacleAngle=nativeSquid.tentacleAngle;squid.lastTentacleAngle=nativeSquid.oldTentacleAngle;
        }
        // Register before resolving relations/targets/owners, which may point back to this entity.
        touch(modern,view);
        view.vehicle=wrap(modern.getVehicle());view.passenger=wrap(modern.getFirstPassenger());
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity arrow&&modern instanceof net.minecraft.world.entity.projectile.AbstractArrow nativeArrow){
            Entity owner=wrap(nativeArrow.getOwner());arrow.owner=owner instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living?living:null;
            TOUCHED.get().put(modern,new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround,null,null,null,extra(view),Placement.of(view)));
        }
        if(view instanceof PathAwareEntity creature && modern instanceof net.minecraft.world.entity.Mob mob) {
            creature.target=wrap(mob.getTarget());
            TOUCHED.get().put(modern,new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround,creature.target,null,null,extra(view),Placement.of(view)));
        }
        return view;
    }
    private static Entity create(net.minecraft.world.entity.Entity modern) {
        World world=World.mcc$wrap(modern.level());
        if(modern instanceof LegacyB173Cow cow && cow.legacy()!=null)return cow.legacy();
        if(modern instanceof net.minecraft.world.entity.player.Player p)return PlayerViewB173.of(p);
        if(modern instanceof net.minecraft.world.entity.item.ItemEntity item)
            return new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,modern.getX(),modern.getY(),modern.getZ(),StacksB173.toLegacy(item.getItem()));
        if(modern instanceof net.minecraft.world.entity.projectile.AbstractArrow)return new legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity(world);
        if(modern instanceof net.minecraft.world.entity.monster.Creeper)return new legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity(world);
        if(modern instanceof net.minecraft.world.entity.monster.Monster)return new legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.animal.Squid)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.SquidEntity(world);
        if(modern instanceof net.minecraft.world.entity.animal.Sheep)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.SheepEntity(world);
        if(modern instanceof net.minecraft.world.entity.animal.Cow)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity(world);
        if(modern instanceof net.minecraft.world.entity.animal.Wolf)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.WolfEntity(world);
        if(modern instanceof net.minecraft.world.entity.animal.Animal)return new legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.PathfinderMob)return new PathAwareEntity(world) {};
        if(modern instanceof net.minecraft.world.entity.LivingEntity)return new legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity(world) {};
        return new Entity(world) {};
    }
    private static Entity touch(net.minecraft.world.entity.Entity modern,Entity view) {
        TOUCHED.get().putIfAbsent(modern,new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround,view instanceof PathAwareEntity p?p.target:null,
                modern instanceof B173LivingHost host?HostState.of(host.legacy()):null,
                modern instanceof B173EntityHost&&!(modern instanceof B173LivingHost)?NeutralState.of(view):null,extra(view),
                modern instanceof B173EntityHost||view instanceof PlayerViewB173?null:Placement.of(view)));
        return view;
    }
    /** Capture identity and a baseline before original code continues mutating a freshly spawned entity. */
    public static void spawned(net.minecraft.world.entity.Entity modern,Entity original){
        VIEWS.put(modern,original);
        if(modern instanceof net.minecraft.world.entity.item.ItemEntity item&&original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity legacyItem){EntityFieldsB173.itemAge(item,legacyItem.itemAge);item.setPickUpDelay(legacyItem.pickupDelay);}
        touch(modern,original);
    }
    /** Commit pending cross-entity writes before the host refreshes its own callback view. */
    public static boolean prepareHostCallback(B173EntityHost host) {
        if(host.callbackActive()||host.legacy()==null)return false;
        Touched touched=TOUCHED.get().remove(host.legacy().mcc$modern);
        if(touched!=null)flushHost(host,touched);
        return touched!=null;
    }
    public static void resumeHostCallback(B173EntityHost host,boolean referenced){if((referenced||CALLBACK_DEPTH.get()>1)&&host.legacy()!=null)touch(host.legacy().mcc$modern,host.legacy());}
    private static void flushHost(B173EntityHost host,Touched touched) {
        if(!(host instanceof B173LivingHost living)){flushNeutralHost(host,touched);return;}
        flushLivingHost(living,touched);
    }
    private static void flushNeutralHost(B173EntityHost host,Touched touched){
        NeutralState before=touched.neutralState;if(before==null)return;
        Entity v=host.legacy();NeutralState wanted=NeutralState.of(v);
        Extra oldExtra=touched.extra,wantedExtra=extra(v);
        // A retained reference may outlive nested native damage or movement. Only original writes win.
        host.syncView();wanted.merge(v,before);
        if(v instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item&&oldExtra!=null&&wantedExtra!=null){
            if(wantedExtra.itemAge!=oldExtra.itemAge)item.itemAge=wantedExtra.itemAge;
            if(wantedExtra.pickupDelay!=oldExtra.pickupDelay)item.pickupDelay=wantedExtra.pickupDelay;
            if(!net.minecraft.world.item.ItemStack.matches(wantedExtra.stack,oldExtra.stack))item.stack=StacksB173.toLegacy(wantedExtra.stack);
        }
        host.flushView();host.publishRenderState();
    }
    private static void flushLivingHost(B173LivingHost host,Touched touched) {
        HostState before=touched.hostState;
        if(before==null)return;
        var v=host.legacy();HostState wanted=HostState.of(v);
        if(wanted.equals(before)){if(host instanceof LegacyB173Animal animal)animal.publishRenderState();return;}
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
        if(wanted.fall!=before.fall)v.mcc$fallDistance(wanted.fall);
        if(wanted.vehicle!=before.vehicle)v.vehicle=wanted.vehicle;if(wanted.passenger!=before.passenger)v.passenger=wanted.passenger;
        v.mcc$movementInputs(wanted.sideways!=before.sideways?wanted.sideways:v.mcc$sidewaysSpeed(),
                wanted.forward!=before.forward?wanted.forward:v.mcc$forwardSpeed(),wanted.jumping!=before.jumping?wanted.jumping:v.mcc$jumping());
        host.flushView();
        if(host instanceof LegacyB173Animal animal)animal.publishRenderState();
    }
    public static void flush() {
        if(CALLBACK_DEPTH.get()>0)return;
        var touchedViews=TOUCHED.get();
        try {
            for(var entry:java.util.List.copyOf(touchedViews.entrySet())) {
                var modern=entry.getKey();var touched=entry.getValue();Entity view=touched.view;
                if(modern instanceof B173EntityHost host) {if(!host.callbackActive())flushHost(host,touched);continue;}
                if(view instanceof PlayerViewB173 player)player.writeBack();
                if(view.dead&&!modern.isRemoved()){modern.discard();continue;}
                if(touched.placement!=null)touched.placement.apply(view,modern);
                if(view.fireTicks>modern.getRemainingFireTicks())modern.setRemainingFireTicks(view.fireTicks);
                Extra before=touched.extra;
                if(before!=null){
                    if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item&&modern instanceof net.minecraft.world.entity.item.ItemEntity nativeItem){
                        if(item.itemAge!=before.itemAge)EntityFieldsB173.itemAge(nativeItem,item.itemAge);
                        if(item.pickupDelay!=before.pickupDelay)nativeItem.setPickUpDelay(item.pickupDelay);
                        var wanted=item.stack==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(item.stack);
                        if(!net.minecraft.world.item.ItemStack.matches(before.stack,wanted)){if(wanted.isEmpty()){nativeItem.discard();continue;}nativeItem.setItem(wanted);}
                    }
                    if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity creeper&&modern instanceof net.minecraft.world.entity.monster.Creeper nativeCreeper&&creeper.fuseTime!=before.fuse)EntityFieldsB173.creeperFuse(nativeCreeper,creeper.fuseTime);
                    if(view instanceof legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity arrow&&modern instanceof net.minecraft.world.entity.projectile.AbstractArrow nativeArrow&&arrow.owner!=before.owner)nativeArrow.setOwner(arrow.owner==null?null:arrow.owner.mcc$modern);
                }
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
