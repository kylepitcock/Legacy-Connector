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
    private boolean inTick,inMovement,inAI,inTravel,inMove;
    private MoverType nativeMoveType=MoverType.SELF;
    private int travels,moves;
    public int baseTravelCount(){return travels;}
    public int baseMoveCount(){return moves;}
    private boolean syncingView;
    private float appliedMovementSpeed=Float.NaN;
    private DamageSource damageSource;
    private net.minecraft.world.level.block.state.BlockState fallingState;
    private BlockPos fallingPos;
    private boolean inFall;
    private boolean inPlacement;
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
        syncView();legacy.bodyYaw=yBodyRot;legacy.lastBodyYaw=yBodyRotO;
        legacy.yaw=getYHeadRot();legacy.prevYaw=yHeadRotO;
        legacy.lastWalkAnimationSpeed=walkAnimation.speed(0);legacy.walkAnimationSpeed=walkAnimation.speed(1);
        legacy.walkAnimationProgress=walkAnimation.position(1);return legacy;
    }
    private static legacyconnector.vb1_7_3.net.minecraft.entity.Entity relationView(net.minecraft.world.entity.Entity entity){
        return entity instanceof B173LivingHost host?host.legacy():entity!=null&&entity.level().isClientSide?EntityViewsB173.wrapReadOnly(entity):EntityViewsB173.wrap(entity);
    }
    public void syncView(){
        if(legacy==null||syncingView)return;
        syncingView=true;
        try{
        legacy.world=World.mcc$wrap(level());
        legacy.prevX=xo;legacy.prevY=yo+legacy.standingEyeHeight;legacy.prevZ=zo;legacy.prevYaw=yRotO;legacy.prevPitch=xRotO;
        legacy.setPosition(getX(),getY()+legacy.standingEyeHeight,getZ());legacy.yaw=getYRot();legacy.pitch=getXRot();
        var v=getDeltaMovement();legacy.velocityX=v.x;legacy.velocityY=v.y;legacy.velocityZ=v.z;
        legacy.onGround=onGround();legacy.horizontalCollision=horizontalCollision;legacy.verticalCollision=verticalCollision;legacy.mcc$environment(isInWater());
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
        if(!Float.isFinite(legacy.width)||!Float.isFinite(legacy.height)||legacy.width<=0||legacy.height<=0)throw new IllegalStateException("Invalid Beta animal dimensions");
        setPos(legacy.x,legacy.y-legacy.standingEyeHeight,legacy.z);setYRot(legacy.yaw);setXRot(legacy.pitch);setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);
        if(legacy.health>getMaxHealth())getAttribute(Attributes.MAX_HEALTH).setBaseValue(legacy.health);
        noPhysics=legacy.noClip;fallDistance=legacy.mcc$fallDistance();
        var step=getAttribute(Attributes.STEP_HEIGHT);if(step!=null&&Float.isFinite(legacy.stepHeight))step.setBaseValue(Math.max(0,legacy.stepHeight));
        setOnGround(legacy.onGround);setHealth(legacy.health);setRemainingFireTicks(legacy.fireTicks);setAirSupply(legacy.air);setNoActionTime(legacy.despawnCounter);
        if(Float.compare(appliedMovementSpeed,legacy.mcc$movementSpeed())!=0){appliedMovementSpeed=legacy.mcc$movementSpeed();setSpeed(appliedMovementSpeed);}
        xxa=legacy.mcc$sidewaysSpeed();zza=legacy.mcc$forwardSpeed();jumping=legacy.mcc$jumping();
        var held=legacy.getHeldItem();var nativeHeld=held==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(held);
        if(!net.minecraft.world.item.ItemStack.matches(getMainHandItem(),nativeHeld))setItemSlot(EquipmentSlot.MAINHAND,nativeHeld);
        setDropChance(EquipmentSlot.MAINHAND,0); // Original getHeldItem is a render attachment; original dropItems owns loot.
        entityData.set(WIDTH,legacy.width);entityData.set(HEIGHT,legacy.height);entityData.set(TEXTURE,legacy.mcc$texture()==null?"/mob/char.png":legacy.mcc$texture());
        entityData.set(EYE_OFFSET,legacy.standingEyeHeight);
        refreshDimensions();if(legacy.dead)discard();
    }
    private void callback(Runnable action){try(var minecraftContext=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(level()),legacy);var views=EntityViewsB173.callbackScope()){boolean outer=callbacks==0;boolean referenced=false;if(outer){referenced=EntityViewsB173.prepareHostCallback(this);syncView();}callbacks++;try{action.run();flushView();if(outer)publishRenderState();}finally{callbacks--;if(outer)EntityViewsB173.resumeHostCallback(this,referenced);}}}
    @Override public void tick(){if(legacy==null||level().isClientSide){super.tick();if(legacy!=null)syncView();return;}callback(()->legacy.tick());}
    public void mcc$baseTick(){if(inTick)throw new IllegalStateException("Recursive Beta base tick");inTick=true;ticks++;try{if(legacy.attackCooldown>0)legacy.attackCooldown--;flushView();super.tick();syncView();}finally{inTick=false;}}
    @Override public void aiStep(){if(legacy==null||level().isClientSide){super.aiStep();return;}syncView();callback(()->legacy.tickMovement());}
    public void baseMovement(){if(inMovement)throw new IllegalStateException("Recursive Beta base movement");inMovement=true;movements++;try{flushView();super.aiStep();syncView();}finally{inMovement=false;}}
    public void serverAIPhase(){if(legacy==null)return;syncView();callback(()->legacy.mcc$tickLiving());}
    public void baseAI(){
        if(inAI)throw new IllegalStateException("Recursive Beta base AI");inAI=true;ais++;
        try{
            legacy.despawnCounter++;
            flushView();
            if(legacy instanceof MobEntity creature){
                creature.mcc$setMovementBlocked(creature.mcc$isMovementBlocked());
                if(creature.target!=null&&!creature.target.isAlive())creature.target=null;
                if(creature.target==null)creature.target=creature.mcc$targetInRange();
                var target=creature.target;
                if(target!=null&&target.mcc$modern!=null){
                    if(legacy.canSee(target))creature.mcc$attack(target,legacy.getDistance(target));else creature.mcc$resetAttack(target,legacy.getDistance(target));
                    if(!creature.mcc$isMovementBlocked())getNavigation().moveTo(target.mcc$modern,Math.max(.01,legacy.mcc$movementSpeed()));
                    getLookControl().setLookAt(target.mcc$modern,30F,30F);
                }else if(!creature.mcc$isMovementBlocked()&&getNavigation().isDone()&&random.nextInt(80)==0){
                    creature.mcc$pathingUpdate();
                }
            }
            if(!(legacy instanceof MobEntity creature)||creature.target==null&&getNavigation().isDone())legacy.mcc$idleLook();
            flushView();getNavigation().tick();getMoveControl().tick();getLookControl().tick();getJumpControl().tick();syncView();
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
        syncView();callback(()->legacy.travel((float)input.x,(float)input.z));
    }
    public void baseTravel(float sideways,float forward){
        if(inTravel)throw new IllegalStateException("Recursive Beta base travel");
        if(!Float.isFinite(sideways)||!Float.isFinite(forward))throw new IllegalStateException("Non-finite Beta travel inputs");
        inTravel=true;travels++;try{flushView();super.travel(new net.minecraft.world.phys.Vec3(sideways,0,forward));syncView();}finally{inTravel=false;}
    }
    @Override public void move(MoverType type,net.minecraft.world.phys.Vec3 motion){
        if(legacy==null||level().isClientSide){super.move(type,motion);return;}
        MoverType previous=nativeMoveType;nativeMoveType=type;
        try{syncView();callback(()->legacy.move(motion.x,motion.y,motion.z));}finally{nativeMoveType=previous;}
    }
    public void baseMove(double x,double y,double z){
        if(inMove)throw new IllegalStateException("Recursive Beta base move");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalStateException("Non-finite Beta movement");
        inMove=true;moves++;try{flushView();super.move(nativeMoveType,new net.minecraft.world.phys.Vec3(x,y,z));syncView();}finally{inMove=false;}
    }
    public void baseSetPath(legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path path){
        flushView();if(path==null)getNavigation().stop();else if(path.mcc$modern()!=null)getNavigation().moveTo(path.mcc$modern(),Math.max(.01,legacy.mcc$movementSpeed()));
    }
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return legacy instanceof MobEntity c?c.mcc$pathFavor(pos.getX(),pos.getY(),pos.getZ()):0;}
    @Override public boolean hurt(DamageSource source,float amount){if(legacy==null||level().isClientSide)return super.hurt(source,amount);DamageSource previous=damageSource;damageSource=source;boolean[] result={false};try{callback(()->result[0]=legacy.damage(EntityViewsB173.wrap(source.getDirectEntity()!=null?source.getDirectEntity():source.getEntity()),(int)Math.ceil(amount)));return result[0];}finally{damageSource=previous;}}
    public boolean baseDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount){if(level().isClientSide)return false;flushView();DamageSource source=damageSource!=null?damageSource:attacker!=null&&attacker.mcc$modern instanceof net.minecraft.world.entity.LivingEntity living?damageSources().mobAttack(living):damageSources().generic();boolean result=super.hurt(source,amount);syncView();return result;}
    @Override public void knockback(double strength,double x,double z){if(legacy==null||level().isClientSide){super.knockback(strength,x,z);return;}syncView();callback(()->legacy.applyKnockback(EntityViewsB173.wrap(damageSource==null?null:damageSource.getEntity()),0,x,z));}
    public void baseKnockback(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount,double x,double z){flushView();super.knockback(.4,x,z);syncView();}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){if(legacy==null||level().isClientSide)return super.causeFallDamage(distance,multiplier,source);callback(()->legacy.mcc$onLanding(distance));return false;}
    public void baseOnLanding(float distance){flushView();super.causeFallDamage(distance,1F,damageSources().fall());syncView();}
    public boolean baseCanSpawn(){flushView();return level().noCollision(this)&&!level().containsAnyLiquid(getBoundingBox());}
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType reason){
        if(legacy==null)return false;
        boolean[] allowed={false};callback(()->allowed[0]=legacy.canSpawn());return allowed[0];
    }
    @Override public int getMaxSpawnClusterSize(){return legacy==null?4:Math.max(1,legacy.getLimitPerChunk());}
    public void baseMarkDead(){discard();}
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){
        if(legacy==null||level().isClientSide){super.playerTouch(player);return;}
        var view=(PlayerViewB173)EntityViewsB173.wrap(player);callback(()->{legacy.onPlayerInteraction(view);view.writeBack();});
    }
    @Override public void handleEntityEvent(byte status){if(legacy==null){super.handleEntityEvent(status);return;}callback(()->legacy.processServerEntityStatus(status));}
    public void baseEntityStatus(byte status){super.handleEntityEvent(status);syncView();}
    @Override protected boolean isImmobile(){return legacy==null?super.isImmobile():legacy.mcc$isImmobile();}
    @Override public boolean isPushable(){return legacy==null?super.isPushable():legacy.isPushable();}
    @Override public boolean isPickable(){return legacy==null?super.isPickable():legacy.isCollidable();}
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
    @Override protected void doPush(net.minecraft.world.entity.Entity other){if(legacy==null||level().isClientSide){super.doPush(other);return;}callback(()->legacy.onCollision(EntityViewsB173.wrap(other)));}
    @Override protected float getSoundVolume(){return legacy==null?super.getSoundVolume():legacy.mcc$soundVolume();}
    @Override public boolean removeWhenFarAway(double distance){return legacy!=null&&legacy.mcc$canDespawn();}
    @Override public void playAmbientSound(){if(legacy==null||level().isClientSide)return;callback(()->playLegacySound(legacy.mcc$randomSound()));}
    private void playLegacySound(String name){if(name!=null)World.mcc$wrap(level()).playSound(legacy,name,getSoundVolume(),getVoicePitch());}
    @Override protected void playHurtSound(DamageSource source){if(legacy!=null&&!level().isClientSide){syncView();callback(()->playLegacySound(legacy.mcc$hurtSound()));}}
    @Override public void die(DamageSource source){super.die(source);if(legacy!=null&&!level().isClientSide){syncView();callback(()->{playLegacySound(legacy.mcc$deathSound());legacy.killedByOtherEntity=source.getEntity()!=null;legacy.onKilledBy(EntityViewsB173.wrap(source.getEntity()));});}}
    @Override public InteractionResult mobInteract(net.minecraft.world.entity.player.Player p,InteractionHand hand){if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(level().isClientSide)return InteractionResult.SUCCESS;boolean[] result={false};var view=(PlayerViewB173)EntityViewsB173.wrap(p);callback(()->{Runnable action=()->{result[0]=legacy.interact(view);view.writeBack();};if(p instanceof net.minecraft.server.level.ServerPlayer serverPlayer)MenusB173.withActor(serverPlayer,action);else action.run();});return result[0]?InteractionResult.CONSUME:InteractionResult.PASS;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(legacy==null)return;callback(()->{var n=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();legacy.writeNbt(n);tag.put("beta",NbtB173.toModern(n));});tag.putFloat("betaWidth",legacy.width);tag.putFloat("betaHeight",legacy.height);tag.putFloat("betaEyeOffset",legacy.standingEyeHeight);tag.putString("betaTexture",texturePath());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(legacy==null)return;if(tag.contains("betaEyeOffset"))legacy.standingEyeHeight=tag.getFloat("betaEyeOffset");callback(()->{if(tag.contains("betaWidth"))legacy.width=tag.getFloat("betaWidth");if(tag.contains("betaHeight"))legacy.height=tag.getFloat("betaHeight");legacy.readNbt(NbtB173.fromModern(tag.getCompound("beta")));});if(tag.contains("betaTexture")){legacy.mcc$setTexture(tag.getString("betaTexture"));entityData.set(TEXTURE,legacy.mcc$texture());}}
}
