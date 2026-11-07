package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.PathAwareEntity;
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
public final class LegacyB173Animal extends PathfinderMob implements B173LivingHost {
    private static final EntityDataAccessor<String> TEXTURE=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(LegacyB173Animal.class,EntityDataSerializers.FLOAT);
    private LivingEntity legacy;
    private int callbacks,ticks,movements,ais;
    private boolean inTick,inMovement,inAI;
    private boolean syncingView;
    private float appliedMovementSpeed=Float.NaN;
    private DamageSource damageSource;
    private net.minecraft.world.phys.Vec3 preTravelVelocity=net.minecraft.world.phys.Vec3.ZERO;
    public net.minecraft.world.phys.Vec3 lastPreTravelVelocity(){return preTravelVelocity;}
    public LegacyB173Animal(EntityType<? extends PathfinderMob> type,Level level,LivingEntity original){
        super(type,level);
        try{legacy=original!=null?original:(LivingEntity)AnimalsB173.info(type).registration().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Beta animal constructor failed",e);}
        legacy.mcc$modern=this;legacy.world=World.mcc$wrap(level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(legacy.maxHealth,legacy.health));
        flushView();
    }
    public LivingEntity legacy(){return legacy;}
    public boolean callbackActive(){return callbacks>0||syncingView;}
    public int baseTickCount(){return ticks;}
    public int baseMovementCount(){return movements;}
    public int baseAICount(){return ais;}
    public String texturePath(){return entityData.get(TEXTURE);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TEXTURE,"/mob/char.png");b.define(WIDTH,.6F);b.define(HEIGHT,1.8F);}
    @Override protected void registerGoals(){}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){return legacy==null?super.getDefaultDimensions(pose):EntityDimensions.scalable(entityData.get(WIDTH),entityData.get(HEIGHT));}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(legacy!=null&&(key==WIDTH||key==HEIGHT))refreshDimensions();}
    private static legacyconnector.vb1_7_3.net.minecraft.entity.Entity relationView(net.minecraft.world.entity.Entity entity){
        return entity instanceof B173LivingHost host?host.legacy():EntityViewsB173.wrap(entity);
    }
    public void syncView(){
        if(legacy==null||syncingView)return;
        syncingView=true;
        try{
        legacy.prevX=xo;legacy.prevY=yo;legacy.prevZ=zo;legacy.prevYaw=yRotO;legacy.prevPitch=xRotO;
        legacy.setPosition(getX(),getY()+legacy.standingEyeHeight,getZ());legacy.yaw=getYRot();legacy.pitch=getXRot();
        var v=getDeltaMovement();legacy.velocityX=v.x;legacy.velocityY=v.y;legacy.velocityZ=v.z;
        legacy.onGround=onGround();legacy.horizontalCollision=horizontalCollision;legacy.verticalCollision=verticalCollision;
        legacy.health=(int)Math.ceil(getHealth());legacy.hurtTime=hurtTime;legacy.deathTime=deathTime;
        legacy.fireTicks=getRemainingFireTicks();legacy.air=getAirSupply();legacy.age=tickCount;
        legacy.mcc$movementInputs(xxa,zza,jumping);
        var previousVehicle=legacy.vehicle;var previousPassenger=legacy.passenger;
        legacy.vehicle=relationView(getVehicle());legacy.passenger=relationView(getFirstPassenger());
        if(previousVehicle!=null&&previousVehicle!=legacy.vehicle&&previousVehicle.passenger==legacy)previousVehicle.passenger=null;
        if(previousPassenger!=null&&previousPassenger!=legacy.passenger&&previousPassenger.vehicle==legacy)previousPassenger.vehicle=null;
        if(legacy.vehicle!=null&&getVehicle().getFirstPassenger()==this)legacy.vehicle.passenger=legacy;
        if(legacy.passenger!=null)legacy.passenger.vehicle=legacy;
        }finally{syncingView=false;}
    }
    public void flushView(){
        if(legacy==null)return;
        if(!Float.isFinite(legacy.width)||!Float.isFinite(legacy.height)||legacy.width<=0||legacy.height<=0)throw new IllegalStateException("Invalid Beta animal dimensions");
        setPos(legacy.x,legacy.y-legacy.standingEyeHeight,legacy.z);setYRot(legacy.yaw);setXRot(legacy.pitch);setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);
        setOnGround(legacy.onGround);setHealth(legacy.health);setRemainingFireTicks(legacy.fireTicks);setAirSupply(legacy.air);
        if(Float.compare(appliedMovementSpeed,legacy.mcc$movementSpeed())!=0){appliedMovementSpeed=legacy.mcc$movementSpeed();setSpeed(appliedMovementSpeed);}
        xxa=legacy.mcc$sidewaysSpeed();zza=legacy.mcc$forwardSpeed();jumping=legacy.mcc$jumping();
        entityData.set(WIDTH,legacy.width);entityData.set(HEIGHT,legacy.height);entityData.set(TEXTURE,legacy.mcc$texture()==null?"/mob/char.png":legacy.mcc$texture());
        refreshDimensions();if(legacy.dead)discard();
    }
    private void callback(Runnable action){boolean outer=callbacks==0;if(outer){EntityViewsB173.prepareHostCallback(this);syncView();}callbacks++;try{action.run();flushView();}finally{callbacks--;if(outer)EntityViewsB173.flush();}}
    @Override public void tick(){if(legacy==null){super.tick();return;}callback(()->legacy.tick());}
    public void mcc$baseTick(){if(inTick)throw new IllegalStateException("Recursive Beta base tick");inTick=true;ticks++;try{flushView();super.tick();syncView();}finally{inTick=false;}}
    @Override public void aiStep(){if(legacy==null){super.aiStep();return;}syncView();callback(()->legacy.tickMovement());}
    public void baseMovement(){if(inMovement)throw new IllegalStateException("Recursive Beta base movement");inMovement=true;movements++;try{flushView();super.aiStep();syncView();}finally{inMovement=false;}}
    public void serverAIPhase(){if(legacy==null)return;syncView();callback(()->legacy.mcc$tickLiving());}
    public void baseAI(){
        if(inAI)throw new IllegalStateException("Recursive Beta base AI");inAI=true;ais++;
        try{
            flushView();if(legacy.attackCooldown>0)legacy.attackCooldown--;
            if(legacy instanceof PathAwareEntity creature){
                if(creature.target!=null&&!creature.target.isAlive())creature.target=null;
                if(creature.target==null)creature.target=creature.mcc$targetInRange();
                var target=creature.target;
                if(target!=null&&target.mcc$modern!=null){
                    if(legacy.canSee(target))creature.mcc$attack(target,legacy.getDistance(target));
                    getNavigation().moveTo(target.mcc$modern,Math.max(.01,legacy.mcc$movementSpeed()));
                    getLookControl().setLookAt(target.mcc$modern,30F,30F);
                }else if(getNavigation().isDone()&&random.nextInt(80)==0){
                    BlockPos best=null;float favor=-Float.MAX_VALUE;
                    for(int i=0;i<10;i++){BlockPos p=blockPosition().offset(random.nextInt(13)-6,random.nextInt(7)-3,random.nextInt(13)-6);float f=creature.mcc$pathFavor(p.getX(),p.getY(),p.getZ());if(f>favor){favor=f;best=p;}}
                    if(best!=null)getNavigation().moveTo(best.getX()+.5,best.getY(),best.getZ()+.5,Math.max(.01,legacy.mcc$movementSpeed()));
                }
            }
            flushView();getNavigation().tick();getMoveControl().tick();getLookControl().tick();getJumpControl().tick();syncView();
        }finally{inAI=false;}
    }
    @Override public void travel(net.minecraft.world.phys.Vec3 input){preTravelVelocity=getDeltaMovement();super.travel(input);}
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return legacy instanceof PathAwareEntity c?c.mcc$pathFavor(pos.getX(),pos.getY(),pos.getZ()):0;}
    @Override public boolean hurt(DamageSource source,float amount){if(legacy==null)return super.hurt(source,amount);DamageSource previous=damageSource;damageSource=source;boolean[] result={false};try{callback(()->result[0]=legacy.damage(EntityViewsB173.wrap(source.getEntity()),(int)Math.ceil(amount)));return result[0];}finally{damageSource=previous;}}
    public boolean baseDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount){flushView();DamageSource source=damageSource!=null?damageSource:attacker!=null&&attacker.mcc$modern instanceof net.minecraft.world.entity.LivingEntity living?damageSources().mobAttack(living):damageSources().generic();boolean result=super.hurt(source,amount);syncView();return result;}
    @Override public void knockback(double strength,double x,double z){if(legacy==null){super.knockback(strength,x,z);return;}syncView();callback(()->legacy.applyKnockback(EntityViewsB173.wrap(damageSource==null?null:damageSource.getEntity()),0,x,z));}
    public void baseKnockback(legacyconnector.vb1_7_3.net.minecraft.entity.Entity attacker,int amount,double x,double z){flushView();super.knockback(.4,x,z);syncView();}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){if(legacy==null)return super.causeFallDamage(distance,multiplier,source);callback(()->legacy.mcc$onLanding(distance));return false;}
    public void baseOnLanding(float distance){flushView();super.causeFallDamage(distance,1F,damageSources().fall());syncView();}
    public boolean baseCanSpawn(){flushView();return level().noCollision(this)&&!level().containsAnyLiquid(getBoundingBox());}
    public void baseMarkDead(){discard();}
    @Override public boolean removeWhenFarAway(double distance){return legacy!=null&&legacy.mcc$canDespawn();}
    @Override public void playAmbientSound(){if(legacy==null)return;callback(()->playLegacySound(legacy.mcc$randomSound()));}
    private void playLegacySound(String name){if(name!=null)World.mcc$wrap(level()).playSound(legacy,name,getSoundVolume(),getVoicePitch());}
    @Override protected void playHurtSound(DamageSource source){if(legacy!=null){syncView();callback(()->playLegacySound(legacy.mcc$hurtSound()));}}
    @Override public void die(DamageSource source){super.die(source);if(legacy!=null){syncView();callback(()->{playLegacySound(legacy.mcc$deathSound());legacy.killedByOtherEntity=source.getEntity()!=null;legacy.onKilledBy(EntityViewsB173.wrap(source.getEntity()));});}}
    @Override public InteractionResult mobInteract(net.minecraft.world.entity.player.Player p,InteractionHand hand){if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;boolean[] result={false};var view=PlayerViewB173.of(p);callback(()->{result[0]=legacy.interact(view);view.writeBack();});return result[0]?InteractionResult.sidedSuccess(level().isClientSide):InteractionResult.PASS;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(legacy==null)return;callback(()->{var n=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();legacy.writeNbt(n);tag.put("beta",NbtB173.toModern(n));});tag.putFloat("betaWidth",legacy.width);tag.putFloat("betaHeight",legacy.height);tag.putString("betaTexture",texturePath());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(legacy==null)return;callback(()->{if(tag.contains("betaWidth"))legacy.width=tag.getFloat("betaWidth");if(tag.contains("betaHeight"))legacy.height=tag.getFloat("betaHeight");legacy.readNbt(NbtB173.fromModern(tag.getCompound("beta")));});if(tag.contains("betaTexture"))entityData.set(TEXTURE,tag.getString("betaTexture"));}
}
