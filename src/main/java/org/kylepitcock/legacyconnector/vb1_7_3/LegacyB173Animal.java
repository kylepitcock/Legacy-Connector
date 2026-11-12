package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/** Neutral physics/navigation host. All legacy simulation phases enter through original virtual callbacks. */
public final class LegacyB173Animal extends PathfinderMob implements B173LivingHost, Saddleable {
    @Override public boolean isSaddleable(){return isAlive()&&legacy instanceof legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity;}
    @Override public boolean isSaddled(){return legacy instanceof legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity pig&&pig.isSaddled();}
    @Override public net.minecraft.sounds.SoundEvent getSaddleSoundEvent(){return net.minecraft.sounds.SoundEvents.PIG_SADDLE;}
    @Override public void equipSaddle(net.minecraft.world.item.ItemStack stack,net.minecraft.sounds.SoundSource soundSource){
        if(!isSaddleable()||level().isClientSide)return;
        callback(()->((legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity)legacy).setSaddled(true));
        if(soundSource!=null)level().playSound(null,this,getSaddleSoundEvent(),soundSource,.5F,1F);
    }
    private static final EntityDataAccessor<String> TEXTURE=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_OFFSET=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> RENDER_STATE=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.COMPOUND_TAG);
    private CompoundTag receivedRenderState;
    private LivingEntity legacy;
    private legacyconnector.vb1_7_3.net.minecraft.entity.Entity syncedVehicle,syncedPassenger;
    private int callbacks,ticks,movements,ais;
    private boolean inTick,inMovement,inAI,inTravel,inMove,inJump;
    private boolean nativeBoundsMovement;
    private MoverType nativeMoveType=MoverType.SELF;
    private int travels,moves;
    public int baseTravelCount(){return travels;}
    public int baseMoveCount(){return moves;}
    private float syncedHeadYaw=Float.NaN,syncedBodyYaw=Float.NaN;
    private boolean syncingView;
    private float appliedMovementSpeed=Float.NaN;
    private DamageSource damageSource;
    private net.minecraft.world.level.block.state.BlockState fallingState;
    private BlockPos fallingPos;
    private boolean inFall;
    private boolean inPlacement;
    private boolean inDeathCompletion;
    private net.minecraft.world.phys.Vec3 preTravelVelocity=net.minecraft.world.phys.Vec3.ZERO;
    public net.minecraft.world.phys.Vec3 lastPreTravelVelocity(){return preTravelVelocity;}
    public LegacyB173Animal(EntityType<? extends PathfinderMob> type,Level level,LivingEntity original){
        super(type,level);
        try{legacy=original!=null?original:(LivingEntity)AnimalsB173.info(type).registration().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Beta animal constructor failed",e);}
        legacy.mcc$modern=this;legacy.world=World.mcc$wrap(level);
        setYHeadRot(legacy.yaw);yHeadRotO=legacy.prevYaw;
        yBodyRot=legacy.bodyYaw;yBodyRotO=legacy.lastBodyYaw;yRotO=legacy.prevYaw;xRotO=legacy.prevPitch;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(legacy.maxHealth,legacy.health));
        if(!level.isClientSide){flushView();publishRenderState();}
    }
    public LivingEntity legacy(){return legacy;}
    public boolean callbackActive(){return callbacks>0||syncingView;}
    public int baseTickCount(){return ticks;}
    public int baseMovementCount(){return movements;}
    public int baseAICount(){return ais;}
    public String texturePath(){return entityData.get(TEXTURE);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TEXTURE,"/mob/char.png");b.define(WIDTH,.6F);b.define(HEIGHT,1.8F);b.define(EYE_OFFSET,0F);b.define(RENDER_STATE,new CompoundTag());}
    @Override protected void registerGoals(){}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){return legacy==null?super.getDefaultDimensions(pose):EntityDimensions.scalable(entityData.get(WIDTH),entityData.get(HEIGHT)).withEyeHeight(legacy.getEyeHeight());}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);if(legacy==null)return;
        if(level().isClientSide){
            if(key==RENDER_STATE)applyRenderState(entityData.get(RENDER_STATE));
            if(key==WIDTH)legacy.width=entityData.get(WIDTH);if(key==HEIGHT)legacy.height=entityData.get(HEIGHT);
            if(key==EYE_OFFSET)legacy.standingEyeHeight=entityData.get(EYE_OFFSET);
            if(key==TEXTURE)legacy.mcc$setTexture(texturePath());
        }
        if(key==WIDTH||key==HEIGHT)refreshDimensions();
    }
    @Override public void publishRenderState(){if(legacy!=null&&!level().isClientSide)entityData.set(RENDER_STATE,AnimalRenderStateB173.capture(legacy));}
    public CompoundTag renderState(){return entityData.get(RENDER_STATE).copy();}
    public void applyRenderState(CompoundTag incoming){AnimalRenderStateB173.apply(legacy,receivedRenderState,incoming);receivedRenderState=incoming.copy();}
    /** Apply a complete native tracking snapshot, also used by the packet round-trip fixture. */
    public void applyTrackedRenderData(){
        legacy.width=entityData.get(WIDTH);legacy.height=entityData.get(HEIGHT);legacy.standingEyeHeight=entityData.get(EYE_OFFSET);
        legacy.mcc$setTexture(texturePath());applyRenderState(entityData.get(RENDER_STATE));refreshDimensions();
    }
    /** Native tracking supplies pose/animation; rendering never writes this view back into simulation. */
    public LivingEntity prepareRenderView(float partial){
        syncView();if(level().isClientSide)AnimalRenderStateB173.retryReferences(legacy);legacy.bodyYaw=yBodyRot;legacy.lastBodyYaw=yBodyRotO;
        legacy.yaw=getYHeadRot();legacy.prevYaw=yHeadRotO;
        legacy.lastWalkAnimationSpeed=walkAnimation.speed(0);legacy.walkAnimationSpeed=walkAnimation.speed(1);
        legacy.walkAnimationProgress=walkAnimation.position(1);return legacy;
    }
    private static legacyconnector.vb1_7_3.net.minecraft.entity.Entity relationView(net.minecraft.world.entity.Entity entity){
        return entity instanceof B173LivingHost host?host.legacy():entity!=null&&entity.level().isClientSide?EntityViewsB173.wrapReadOnly(entity):EntityViewsB173.wrap(entity);
    }
    @Override public boolean startRiding(Entity vehicle,boolean force){
        Entity previous=getVehicle();
        boolean mounted=super.startRiding(vehicle,force);
        if(mounted&&getVehicle()==vehicle&&previous!=vehicle)syncCarrierPassengers(vehicle);
        return mounted;
    }
    @Override public void removeVehicle(){
        Entity previous=getVehicle();
        super.removeVehicle();
        if(previous!=null&&getVehicle()!=previous)syncCarrierPassengers(previous);
    }
    /** Native tracking excludes the player being tracked, including a player carrying a Beta entity. */
    private void syncCarrierPassengers(Entity carrier){
        if(legacy!=null&&carrier instanceof net.minecraft.server.level.ServerPlayer player&&player.connection!=null)
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(carrier));
    }
    public void syncView(){
        if(legacy==null||syncingView)return;
        syncingView=true;
        try{
        legacy.world=World.mcc$wrap(level());
        legacy.prevX=xo;legacy.prevY=yo+legacy.standingEyeHeight;legacy.prevZ=zo;legacy.prevYaw=yRotO;legacy.prevPitch=xRotO;
        legacy.setPosition(getX(),getY()+legacy.standingEyeHeight,getZ());legacy.yaw=getYRot();legacy.pitch=getXRot();
        // Beta move clips the existing box, then derives position from it. Raw position writes
        // (for example animation offsets) do not relocate that collision box.
        var bounds=getBoundingBox();
        legacy.boundingBox.set(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ);
        var v=getDeltaMovement();legacy.velocityX=v.x;legacy.velocityY=v.y;legacy.velocityZ=v.z;
        legacy.onGround=onGround();EntityViewsB173.syncCollisions(legacy,this);legacy.mcc$environment(isInWater());
        legacy.health=(int)Math.ceil(getHealth());legacy.hurtTime=hurtTime;legacy.deathTime=deathTime;legacy.dead=isRemoved();
        legacy.mcc$fallDistance(fallDistance);
        legacy.fireTicks=getRemainingFireTicks();legacy.air=getAirSupply();legacy.age=tickCount;legacy.despawnCounter=getNoActionTime();
        legacy.mcc$movementInputs(xxa,zza,jumping);
        var previousVehicle=legacy.vehicle;var previousPassenger=legacy.passenger;
        legacy.vehicle=relationView(getVehicle());legacy.passenger=relationView(getFirstPassenger());
        if(previousVehicle!=null&&previousVehicle!=legacy.vehicle&&previousVehicle.passenger==legacy)previousVehicle.passenger=null;
        if(previousPassenger!=null&&previousPassenger!=legacy.passenger&&previousPassenger.vehicle==legacy)previousPassenger.vehicle=null;
        if(legacy.vehicle!=null&&getVehicle().getFirstPassenger()==this)legacy.vehicle.passenger=legacy;
        if(legacy.passenger!=null)legacy.passenger.vehicle=legacy;
        syncedVehicle=legacy.vehicle;syncedPassenger=legacy.passenger;
        EntityViewsB173.refreshReferences(legacy);
        }finally{syncingView=false;}
    }
    /** Apply original direct relationship writes, leaving unchanged native relationships alone. */
    private void reconcileRelations(){
        if(legacy.vehicle!=syncedVehicle){
            var desired=legacy.vehicle==null?null:legacy.vehicle.mcc$modern;
            if(desired==null){
                if(syncedVehicle!=null&&getVehicle()==syncedVehicle.mcc$modern)stopRiding();
                if(syncedVehicle!=null&&syncedVehicle.passenger==legacy)syncedVehicle.passenger=null;
            }else if(getVehicle()!=desired)startRiding(desired,true);
            syncedVehicle=legacy.vehicle;
        }
        if(legacy.passenger!=syncedPassenger){
            var previous=syncedPassenger;
            if(previous!=null&&previous.mcc$modern!=null&&previous.mcc$modern.getVehicle()==this)previous.mcc$modern.stopRiding();
            if(previous!=null&&previous.vehicle==legacy)previous.vehicle=null;
            var desired=legacy.passenger;
            if(desired!=null&&desired.mcc$modern!=null){
                if(desired.mcc$modern.getVehicle()!=this)desired.mcc$modern.startRiding(this,true);
                desired.vehicle=legacy;
            }
            syncedPassenger=legacy.passenger;
        }
    }
    public void flushView(){
        if(legacy==null||level().isClientSide)return;
        reconcileRelations();
        if(!Float.isFinite(legacy.width)||!Float.isFinite(legacy.height)||legacy.width<0||legacy.height<0)throw new IllegalStateException("Invalid Beta animal dimensions");
        var bounds=legacy.boundingBox.mcc$box();
        double rawX=legacy.x,rawY=legacy.y-legacy.standingEyeHeight,rawZ=legacy.z;
        setPos(legacy.x,legacy.y-legacy.standingEyeHeight,legacy.z);setYRot(legacy.yaw);setXRot(legacy.pitch);
        // The original code owns head (yaw) and body (bodyYaw) facing; the native rotations are what clients and the renderer read, so a
        // change made by Beta code (a placement orientation, an aim) must reach them. Unchanged values leave native turning alone.
        if(Float.compare(syncedHeadYaw,legacy.yaw)!=0){syncedHeadYaw=legacy.yaw;setYHeadRot(legacy.yaw);}
        if(Float.compare(syncedBodyYaw,legacy.bodyYaw)!=0){syncedBodyYaw=legacy.bodyYaw;yBodyRot=legacy.bodyYaw;}
        setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);
        if(legacy.health>getMaxHealth())getAttribute(Attributes.MAX_HEALTH).setBaseValue(legacy.health);
        noPhysics=legacy.noClip;fallDistance=legacy.mcc$fallDistance();
        var step=getAttribute(Attributes.STEP_HEIGHT);if(step!=null&&Float.isFinite(legacy.stepHeight))step.setBaseValue(Math.max(0,legacy.stepHeight));
        NativeGroundB173.commit(this,legacy.onGround);setHealth(legacy.health);setRemainingFireTicks(legacy.fireTicks);setAirSupply(legacy.air);setNoActionTime(legacy.despawnCounter);
        if(Float.compare(appliedMovementSpeed,legacy.mcc$movementSpeed())!=0){appliedMovementSpeed=legacy.mcc$movementSpeed();setSpeed(appliedMovementSpeed);}
        xxa=legacy.mcc$sidewaysSpeed();zza=legacy.mcc$forwardSpeed();jumping=legacy.mcc$jumping();
        var held=legacy.getHeldItem();var nativeHeld=held==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(held);
        if(!net.minecraft.world.item.ItemStack.matches(getMainHandItem(),nativeHeld))setItemSlot(EquipmentSlot.MAINHAND,nativeHeld);
        setDropChance(EquipmentSlot.MAINHAND,0); // Original getHeldItem is a render attachment; original dropItems owns loot.
        entityData.set(WIDTH,legacy.width);entityData.set(HEIGHT,legacy.height);entityData.set(TEXTURE,legacy.mcc$texture()==null?"/mob/char.png":legacy.mcc$texture());
        entityData.set(EYE_OFFSET,legacy.standingEyeHeight);
        refreshDimensions();
        // Native setPos/refreshDimensions reconstruct a box from position. Preserve both
        // independently, including original direct box edits and explicit setPosition calls.
        setPos(rawX,rawY,rawZ);setBoundingBox(bounds);
        if(legacy.dead&&!isRemoved())discard();
    }
    private void callback(Runnable action){callback(false,action);}
    private void autonomous(Runnable action){callback(true,action);}
    private void physical(Runnable action){callback(false,true,action);}
    private void callback(boolean autonomous,Runnable action){callback(autonomous,false,action);}
    private void callback(boolean autonomous,boolean physical,Runnable action){try(var minecraftContext=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(level()),legacy);var combat=physical?CombatTargetsB173.continuation(legacy):CombatTargetsB173.enter(autonomous?legacy:null);var views=EntityViewsB173.callbackScope()){boolean outer=callbacks==0;boolean referenced=false;if(outer){referenced=EntityViewsB173.prepareHostCallback(this);syncView();if(autonomous)CombatTargetsB173.beforeAI(legacy);}callbacks++;try{action.run();flushView();if(outer)publishRenderState();}finally{callbacks--;if(outer)EntityViewsB173.resumeHostCallback(this,referenced);}}}
    @Override public void tick(){if(legacy==null||level().isClientSide){super.tick();if(legacy!=null)syncView();return;}autonomous(()->{
        legacy.tick();
        if(!isRemoved()&&RendererBlockEffectsB173.hasBlockEffects(this))RendererBlockEffectsB173.render(this);
    });}
    @Override public void rideTick(){
        if(legacy==null||level().isClientSide){super.rideTick();return;}
        autonomous(legacy::tickRiding);
    }
    @Override public void push(double x,double y,double z){
        if(legacy==null||level().isClientSide){super.push(x,y,z);return;}
        var previous=getDeltaMovement();callback(()->legacy.addVelocity(x,y,z));
        if(!previous.equals(getDeltaMovement()))hasImpulse=true;
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance){
        if(legacy==null)return super.shouldRenderAtSqrDistance(distance);
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(level()),legacy)){
            syncView();return legacy.shouldRender(distance);
        }finally{minecraft.world=previousWorld;}
    }
    public void mcc$baseTick(){if(inTick)throw new IllegalStateException("Recursive Beta base tick");inTick=true;ticks++;try{legacy.lastSwingAnimationProgress=legacy.swingAnimationProgress;if(legacy.attackCooldown>0)legacy.attackCooldown--;flushView();super.tick();syncView();}finally{inTick=false;}}
    @Override public void aiStep(){if(legacy==null||level().isClientSide){super.aiStep();return;}syncView();autonomous(()->legacy.tickMovement());}
    public void baseMovement(){if(inMovement)throw new IllegalStateException("Recursive Beta base movement");inMovement=true;movements++;try{flushView();super.aiStep();syncView();}finally{inMovement=false;}}
    @Override public void jumpFromGround(){
        if(legacy==null||level().isClientSide){super.jumpFromGround();return;}
        syncView();physical(legacy::mcc$jump);
    }
    @Override public void baseJump(){
        if(inJump)throw new IllegalStateException("Recursive Beta base jump");
        inJump=true;try{flushView();super.jumpFromGround();syncView();}finally{inJump=false;}
    }
    @Override public int getAmbientSoundInterval(){
        if(legacy==null)return super.getAmbientSoundInterval();
        int[] interval={80};callback(()->interval[0]=legacy.getMinAmbientSoundDelay());return interval[0];
    }
    @Override protected MovementEmission getMovementEmission(){
        return legacy!=null&&!legacy.mcc$steppingEffects()?MovementEmission.NONE:super.getMovementEmission();
    }
    public void serverAIPhase(){if(legacy==null)return;syncView();autonomous(()->legacy.mcc$tickLiving());}
    public void baseAI(){
        if(inAI)throw new IllegalStateException("Recursive Beta base AI");inAI=true;ais++;
        try{
            legacy.despawnCounter++;
            legacy.mcc$tryDespawn();
            flushView();
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity aimingTarget=null;
            if(legacy instanceof MobEntity creature){
                creature.mcc$setMovementBlocked(creature.mcc$isMovementBlocked());
                boolean acquired=false;
                if(creature.target==null){creature.target=creature.mcc$targetInRange();acquired=creature.target!=null;}
                else if(!creature.target.isAlive())creature.target=null;
                else if(creature.target.mcc$modern!=null){
                    var target=creature.target;
                    if(legacy.canSee(target))creature.mcc$attack(target,legacy.getDistance(target));else creature.mcc$resetAttack(target,legacy.getDistance(target));
                }
                // Attack/reset may clear or replace the target and set the shared attack flag.
                var target=creature.target;
                if(target!=null&&target.mcc$modern!=null){
                    if(acquired||!creature.mcc$movementBlocked()){getNavigation().moveTo(target.mcc$modern,Math.max(0,legacy.mcc$movementSpeed()));CombatTargetsB173.navigation(legacy,target);}
                    getLookControl().setLookAt(target.mcc$modern,30F,30F);
                    if(creature.mcc$movementBlocked()&&!getNavigation().isDone())aimingTarget=target;
                }else if(!creature.mcc$movementBlocked()&&getNavigation().isDone()&&creature.mcc$wanderRoll()){
                    creature.mcc$pathingUpdate();
                }
            }
            if(!(legacy instanceof MobEntity creature)||creature.target==null&&getNavigation().isDone())legacy.mcc$idleLook();
            float speed=Math.max(0,legacy.mcc$movementSpeed());
            getNavigation().setSpeedModifier(speed);
            if(speed==0)legacy.mcc$movementInputs(0,0,legacy.mcc$jumping());
            flushView();
            if(speed==0){
                // Keep the route while original code pauses movement, without stale native control or timeout progress.
                getMoveControl().setWantedPosition(getX(),getY(),getZ(),0);setSpeed(0);
                EntityFieldsB173.pauseNavigationTimeout(getNavigation(),level().getGameTime());
            }else getNavigation().tick();
            getMoveControl().tick();getLookControl().tick();getJumpControl().tick();
            if(aimingTarget!=null){
                // Beta follows its existing route while facing the target, rotating movement inputs
                // into that facing rather than stopping the route or letting controls overwrite aim.
                float aim=(float)(Math.atan2(aimingTarget.z-legacy.z,aimingTarget.x-legacy.x)*180/Math.PI)-90F;
                float delta=(getYRot()-aim)*net.minecraft.util.Mth.DEG_TO_RAD;
                float forward=zza,sideways=xxa,cos=net.minecraft.util.Mth.cos(delta),sin=net.minecraft.util.Mth.sin(delta);
                xxa=sideways*cos-forward*sin;zza=forward*cos+sideways*sin;setYRot(aim);
            }
            syncView();
        }finally{inAI=false;}
    }
    @Override public void moveTo(double x,double y,double z,float yaw,float pitch){
        if(legacy==null||level().isClientSide||inPlacement){super.moveTo(x,y,z,yaw,pitch);return;}
        callback(()->legacy.setPositionAndAngles(x,y+legacy.standingEyeHeight,z,yaw,pitch));
    }
    @Override protected void setRot(float yaw,float pitch){
        if(legacy==null||level().isClientSide||inPlacement){super.setRot(yaw,pitch);return;}
        callback(()->legacy.setRotation(yaw,pitch));
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){
        if(legacy==null||level().isClientSide||inPlacement){super.lerpTo(x,y,z,yaw,pitch,steps);return;}
        callback(()->legacy.setPositionAndAnglesAvoidEntities(x,y+legacy.standingEyeHeight,z,yaw,pitch,steps));
    }
    public void baseSetPositionAndAngles(double x,double y,double z,float yaw,float pitch){
        if(inPlacement)throw new IllegalStateException("Recursive Beta base placement");inPlacement=true;
        try{flushView();super.moveTo(x,y-legacy.standingEyeHeight,z,yaw,pitch);syncView();}finally{inPlacement=false;}
    }
    public void baseSetRotation(float yaw,float pitch){
        if(inPlacement){super.setRot(yaw,pitch);return;}inPlacement=true;
        try{flushView();super.setRot(yaw,pitch);syncView();}finally{inPlacement=false;}
    }
    public void baseInterpolate(double x,double y,double z,float yaw,float pitch,int steps){
        if(inPlacement)throw new IllegalStateException("Recursive Beta base interpolation");inPlacement=true;
        try{flushView();super.lerpTo(x,y-legacy.standingEyeHeight,z,yaw,pitch,steps);syncView();}finally{inPlacement=false;}
    }
    @Override public void travel(net.minecraft.world.phys.Vec3 input){
        preTravelVelocity=getDeltaMovement();
        if(legacy==null||level().isClientSide){super.travel(input);return;}
        syncView();physical(()->legacy.travel((float)input.x,(float)input.z));
    }
    public void baseTravel(float sideways,float forward){
        if(inTravel)throw new IllegalStateException("Recursive Beta base travel");
        if(!Float.isFinite(sideways)||!Float.isFinite(forward))throw new IllegalStateException("Non-finite Beta travel inputs");
        inTravel=true;travels++;try{flushView();super.travel(new net.minecraft.world.phys.Vec3(sideways,0,forward));syncView();}finally{inTravel=false;}
    }
    @Override public void move(MoverType type,net.minecraft.world.phys.Vec3 motion){
        if(legacy==null||level().isClientSide){super.move(type,motion);return;}
        MoverType previous=nativeMoveType;nativeMoveType=type;
        try{syncView();physical(()->legacy.move(motion.x,motion.y,motion.z));}finally{nativeMoveType=previous;}
    }
    public void baseMove(double x,double y,double z){
        if(inMove)throw new IllegalStateException("Recursive Beta base move");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalStateException("Non-finite Beta movement");
        inMove=true;moves++;
        try{
            flushView();
            // Beta move derives coordinates from the collision box, even for a zero move.
            // Native move instead adds its resolved delta to raw coordinates.
            var bounds=getBoundingBox();
            setPosRaw((bounds.minX+bounds.maxX)/2,bounds.minY,(bounds.minZ+bounds.maxZ)/2);
            nativeBoundsMovement=true;
            try{super.move(nativeMoveType,new net.minecraft.world.phys.Vec3(x,y,z));}
            finally{nativeBoundsMovement=false;}
            syncView();
        }finally{inMove=false;}
    }
    @Override public void setPos(double x,double y,double z){
        if(!nativeBoundsMovement){super.setPos(x,y,z);return;}
        // Reuse native collision resolution while translating its actual starting box.
        // Dimension fields may have changed independently since that box was placed.
        var bounds=getBoundingBox().move(x-getX(),y-getY(),z-getZ());
        setPosRaw(x,y,z);setBoundingBox(bounds);
    }
    public void baseSetPath(legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path path){
        flushView();if(path==null)getNavigation().stop();else if(path.mcc$modern()!=null)getNavigation().moveTo(path.mcc$modern(),Math.max(0,legacy.mcc$movementSpeed()));
    }
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return legacy instanceof MobEntity c?c.mcc$pathFavor(pos.getX(),pos.getY(),pos.getZ()):0;}
    @Override public void thunderHit(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.LightningBolt bolt){
        if(legacy==null){super.thunderHit(level,bolt);return;}
        callback(()->legacy.onStruckByLightning((legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity)EntityViewsB173.wrap(bolt)));
    }
    @Override public boolean hurt(DamageSource source,float amount){if(legacy==null||level().isClientSide)return super.hurt(source,amount);DamageSource previous=damageSource;damageSource=source;boolean[] result={false};try{autonomous(()->{
        var direct=source.getDirectEntity();
        var attacker=direct instanceof LegacyB173VanillaArrow?source.getEntity():direct!=null?direct:source.getEntity();
        result[0]=legacy.damage(EntityViewsB173.wrap(attacker),(int)Math.ceil(amount));
    });return result[0];}finally{damageSource=previous;}}
    public boolean baseDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount){if(level().isClientSide)return false;flushView();DamageSource source=damageSource!=null?damageSource:attacker!=null&&attacker.mcc$modern instanceof net.minecraft.world.entity.LivingEntity living?damageSources().mobAttack(living):damageSources().generic();boolean result=super.hurt(source,amount);syncView();return result;}
    @Override public void knockback(double strength,double x,double z){if(legacy==null||level().isClientSide){super.knockback(strength,x,z);return;}syncView();callback(()->legacy.applyKnockback(EntityViewsB173.wrap(damageSource==null?null:damageSource.getEntity()),0,x,z));}
    public void baseKnockback(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount,double x,double z){flushView();super.knockback(.4,x,z);syncView();}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){if(legacy==null||level().isClientSide)return super.causeFallDamage(distance,multiplier,source);callback(()->legacy.mcc$onLanding(distance));return false;}
    public void baseOnLanding(float distance){flushView();super.causeFallDamage(distance,1F,damageSources().fall());syncView();}
    public boolean baseCanSpawn(){flushView();var world=World.mcc$wrap(level());world.mcc$requireSpawnCollisionArea(getBoundingBox());var access=world.mcc$access();return access.noCollision(this)&&!access.containsAnyLiquid(getBoundingBox());}
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType reason){
        if(legacy==null)return false;
        try(var generation=World.mcc$generation(level)){boolean[] allowed={false};callback(()->allowed[0]=legacy.canSpawn());return allowed[0];}catch(World.SpawnRegionUnavailable unavailable){return false;}
    }
    @Override public boolean checkSpawnObstruction(LevelReader level){
        // Match the native water-animal host: liquid is its habitat, not an obstruction.
        if(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity)return level.isUnobstructed(this);
        return super.checkSpawnObstruction(level);
    }
    @Override public int getMaxSpawnClusterSize(){return legacy==null?4:Math.max(1,legacy.getLimitPerChunk());}
    public void baseMarkDead(){if(!isRemoved())discard();}
    @Override public void remove(net.minecraft.world.entity.Entity.RemovalReason reason){
        if(inDeathCompletion){
            if(!isRemoved())super.remove(reason==net.minecraft.world.entity.Entity.RemovalReason.DISCARDED?net.minecraft.world.entity.Entity.RemovalReason.KILLED:reason);
            return;
        }
        if(legacy!=null&&!level().isClientSide&&reason==net.minecraft.world.entity.Entity.RemovalReason.KILLED&&getHealth()<=0&&!isRemoved()){
            inDeathCompletion=true;try{callback(()->legacy.markDead());}finally{inDeathCompletion=false;}
            return;
        }
        super.remove(reason);
    }
    @Override public net.minecraft.world.phys.Vec3 getVehicleAttachmentPoint(net.minecraft.world.entity.Entity vehicle){
        if(legacy==null||!(vehicle instanceof net.minecraft.world.entity.player.Player)||vehicle!=getVehicle())return super.getVehicleAttachmentPoint(vehicle);
        net.minecraft.world.phys.Vec3[] attachment={null};
        callback(()->{
            var carrier=EntityViewsB173.wrap(vehicle);
            // Native positionRider subtracts the passenger attachment from its native seat.
            // Reconstruct Beta's seat in legacy coordinates, then normalize its eye origin once.
            var desired=new net.minecraft.world.phys.Vec3(carrier.x,
                    carrier.y+carrier.getPassengerRidingHeight()+legacy.getStandingEyeHeight()-legacy.standingEyeHeight,carrier.z);
            attachment[0]=vehicle.getPassengerRidingPosition(this).subtract(desired);
        });
        return attachment[0];
    }
    @Override protected void positionRider(net.minecraft.world.entity.Entity passenger,net.minecraft.world.entity.Entity.MoveFunction move){
        if(legacy==null||passenger!=getFirstPassenger()||!hasPassenger(passenger)){super.positionRider(passenger,move);return;}
        callback(()->{
            if(!RiderPositionB173.attached(legacy,passenger))return;
            legacy.updatePassengerPosition();
            if(!RiderPositionB173.attached(legacy,passenger))return;
            var view=EntityViewsB173.wrap(passenger);
            move.accept(passenger,view.x,view.y-view.standingEyeHeight,view.z);
        });
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){
        if(legacy==null||level().isClientSide){super.playerTouch(player);return;}
        var view=(PlayerViewB173)EntityViewsB173.wrap(player);callback(()->{legacy.onPlayerInteraction(view);view.writeBack();});
    }
    @Override public void handleEntityEvent(byte status){if(legacy==null){super.handleEntityEvent(status);return;}callback(()->legacy.processServerEntityStatus(status));}
    public void baseEntityStatus(byte status){super.handleEntityEvent(status);syncView();}
    @Override protected boolean isImmobile(){return legacy==null?super.isImmobile():legacy.mcc$isImmobile();}
    @Override public boolean isPushable(){return legacy==null?super.isPushable():legacy.isPushable();}
    @Override public boolean isPickable(){return legacy==null?super.isPickable():legacy.isCollidable();}
    // Beta interactions may toggle a carried entity off its current player vehicle.
    // Native picking otherwise excludes candidates sharing the camera entity's root vehicle.
    @Override public boolean canRiderInteract(){return legacy==null?super.canRiderInteract():isPickable();}
    @Override public boolean fireImmune(){return legacy==null?super.fireImmune():legacy.mcc$fireImmune();}
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type){
        return legacy!=null&&type==net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()&&legacy.canBreatheInWater()?false:super.canDrownInFluidType(type);
    }
    @Override public boolean onClimbable(){return legacy==null?super.onClimbable():legacy.isOnLadder();}
    public boolean baseIsOnLadder(){return super.onClimbable();}
    @Override public boolean isInWall(){return legacy==null?super.isInWall():legacy.isInsideWall();}
    public boolean baseInsideWall(){return super.isInWall();}
    @Override protected void checkFallDamage(double deltaY,boolean landed,net.minecraft.world.level.block.state.BlockState state,BlockPos pos){
        if(legacy==null||level().isClientSide){super.checkFallDamage(deltaY,landed,state,pos);return;}
        var previousState=fallingState;var previousPos=fallingPos;fallingState=state;fallingPos=pos;
        try{syncView();callback(()->legacy.mcc$fall(deltaY,landed));}finally{fallingState=previousState;fallingPos=previousPos;}
    }
    public void baseFall(double deltaY,boolean landed){
        if(inFall)throw new IllegalStateException("Recursive Beta base fall");inFall=true;
        try{flushView();var pos=fallingPos==null?blockPosition().below():fallingPos;super.checkFallDamage(deltaY,landed,fallingState==null?level().getBlockState(pos):fallingState,pos);syncView();}finally{inFall=false;}
    }
    @Override protected void doPush(net.minecraft.world.entity.Entity other){if(legacy==null||level().isClientSide){super.doPush(other);return;}autonomous(()->legacy.onCollision(EntityViewsB173.wrap(other)));}
    @Override protected float getSoundVolume(){return legacy==null?super.getSoundVolume():legacy.mcc$soundVolume();}
    private boolean despawnVeto;
    @Override public void checkDespawn(){
        // The original AI owns its age, lottery and canDespawn dispatch. Retain native event priority.
        if(!level().isClientSide)despawnVeto=net.neoforged.neoforge.event.EventHooks.checkMobDespawn(this);
    }
    @Override public boolean baseCanDespawn(){
        return !despawnVeto&&!isPersistenceRequired()&&!requiresCustomPersistence()&&!hasCustomName()
                &&!isLeashed()&&!isPassenger();
    }
    @Override public boolean removeWhenFarAway(double distance){return legacy!=null&&legacy.mcc$canDespawn();}
    @Override public void playAmbientSound(){if(legacy==null||level().isClientSide)return;callback(()->playLegacySound(legacy.mcc$randomSound()));}
    private void playLegacySound(String name){if(name!=null)World.mcc$wrap(level()).playSound(legacy,name,getSoundVolume(),getVoicePitch());}
    @Override protected void playHurtSound(DamageSource source){if(legacy!=null&&!level().isClientSide){syncView();callback(()->playLegacySound(legacy.mcc$hurtSound()));}}
    @Override public void die(DamageSource source){super.die(source);if(legacy!=null&&!level().isClientSide){syncView();callback(()->{playLegacySound(legacy.mcc$deathSound());legacy.killedByOtherEntity=source.getEntity()!=null;legacy.onKilledBy(EntityViewsB173.wrap(source.getEntity()));});}}
    @Override public InteractionResult mobInteract(net.minecraft.world.entity.player.Player p,InteractionHand hand){if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(level().isClientSide)return InteractionResult.SUCCESS;boolean[] result={false};var view=(PlayerViewB173)EntityViewsB173.wrap(p);callback(()->{Runnable action=()->{result[0]=legacy.interact(view);view.writeBack();};if(p instanceof net.minecraft.server.level.ServerPlayer serverPlayer)MenusB173.withActor(serverPlayer,action);else action.run();});return result[0]?InteractionResult.CONSUME:InteractionResult.PASS;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(legacy==null)return;callback(()->{var n=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();legacy.writeNbt(n);tag.put("beta",NbtB173.toModern(n));});tag.putFloat("betaWidth",legacy.width);tag.putFloat("betaHeight",legacy.height);tag.putFloat("betaEyeOffset",legacy.standingEyeHeight);tag.putString("betaTexture",texturePath());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(legacy==null)return;if(tag.contains("betaEyeOffset"))legacy.standingEyeHeight=tag.getFloat("betaEyeOffset");callback(()->{if(tag.contains("betaWidth"))legacy.width=tag.getFloat("betaWidth");if(tag.contains("betaHeight"))legacy.height=tag.getFloat("betaHeight");legacy.readNbt(NbtB173.fromModern(tag.getCompound("beta")));});if(tag.contains("betaTexture")){legacy.mcc$setTexture(tag.getString("betaTexture"));entityData.set(TEXTURE,legacy.mcc$texture());}}
}
