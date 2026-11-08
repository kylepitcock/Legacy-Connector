package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Native animal movement/collision/health, with a registered Beta identity and original interaction/save callbacks. */
public final class LegacyB173Cow extends Cow {
    private static final EntityDataAccessor<String> TEXTURE=SynchedEntityData.defineId(LegacyB173Cow.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(LegacyB173Cow.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(LegacyB173Cow.class,EntityDataSerializers.FLOAT);
    private LivingEntity legacy;
    public LivingEntity legacy(){return legacy;}
    public String texturePath(){return entityData.get(TEXTURE);}
    public LegacyB173Cow(EntityType<? extends Cow> type,Level level,LivingEntity original){
        super(type,level);
        try{legacy=original!=null?original:(LivingEntity)MobsB173.info(type).registration().type().getConstructor(World.class).newInstance(World.mcc$wrap(level));}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Beta mob constructor failed",e);}
        legacy.mcc$modern=this;legacy.world=World.mcc$wrap(level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(getMaxHealth(),legacy.health));
        push();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(TEXTURE,"/mob/cow.png");builder.define(WIDTH,.9F);builder.define(HEIGHT,1.4F);
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose){
        if(legacy==null)return super.getDefaultDimensions(pose);
        return EntityDimensions.scalable(entityData.get(WIDTH),entityData.get(HEIGHT)).scale(isBaby()?.5F:1F);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor){
        super.onSyncedDataUpdated(accessor);if(legacy!=null&&(accessor==WIDTH||accessor==HEIGHT))refreshDimensions();
    }
    private void pull(){
        if(legacy==null)return;
        legacy.setPositionAndAngles(getX(),getY(),getZ(),getYRot(),getXRot());
        legacy.velocityX=getDeltaMovement().x;legacy.velocityY=getDeltaMovement().y;legacy.velocityZ=getDeltaMovement().z;
        legacy.health=(int)Math.ceil(getHealth());legacy.hurtTime=hurtTime;legacy.deathTime=deathTime;
        legacy.onGround=onGround();legacy.fireTicks=getRemainingFireTicks();legacy.age=tickCount;
        legacy.despawnCounter=getNoActionTime();legacy.dead=isRemoved();
    }
    private void push(){
        if(!Float.isFinite(legacy.width)||!Float.isFinite(legacy.height)||legacy.width<=0||legacy.height<=0)throw new IllegalStateException("Invalid legacy mob dimensions");
        setPos(legacy.x,legacy.y,legacy.z);setYRot(legacy.yaw);setXRot(legacy.pitch);
        setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);setHealth(legacy.health);setRemainingFireTicks(legacy.fireTicks);
        setNoActionTime(legacy.despawnCounter);
        entityData.set(WIDTH,legacy.width);entityData.set(HEIGHT,legacy.height);entityData.set(TEXTURE,legacy.mcc$texture()==null?"/mob/cow.png":legacy.mcc$texture());
        refreshDimensions();if(legacy.dead)discard();
    }
    @Override public void tick(){
        if(legacy!=null&&legacy.dead){discard();return;}
        super.tick();pull();
    }
    private boolean despawnVeto;
    @Override public void checkDespawn(){
        if(!level().isClientSide)despawnVeto=net.neoforged.neoforge.event.EventHooks.checkMobDespawn(this);
    }
    public boolean baseCanDespawn(){
        return !despawnVeto&&!isPersistenceRequired()&&!requiresCustomPersistence()&&!hasCustomName()
                &&!isLeashed()&&!isPassenger();
    }
    @Override public int getMaxSpawnClusterSize(){return legacy==null?super.getMaxSpawnClusterSize():Math.max(1,legacy.getLimitPerChunk());}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();
        if(legacy==null||isRemoved())return;
        try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(level()),legacy);
            var views=EntityViewsB173.callbackScope()){
            pull();legacy.mcc$tryDespawn();push();
        }
    }
    public boolean baseCanSpawn(){
        push();var world=World.mcc$wrap(level());world.mcc$requireSpawnCollisionArea(getBoundingBox());
        var access=world.mcc$access();return access.noCollision(this)&&!access.containsAnyLiquid(getBoundingBox());
    }
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor access,MobSpawnType reason){
        if(legacy==null)return false;
        try(var generation=World.mcc$generation(access);
            var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(level()),legacy);
            var views=EntityViewsB173.callbackScope()){
            pull();boolean allowed=legacy.canSpawn();push();return allowed;
        }catch(World.SpawnRegionUnavailable unavailable){return false;}
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){boolean result=super.hurt(source,amount);pull();return result;}
    @Override public InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,InteractionHand hand){
        var info=MobsB173.info(getType());
        if(info.interact()==null){var result=super.mobInteract(player,hand);pull();return result;}
        // Beta has only a main hand; false from an original override must not fall through to native milking.
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        var view=PlayerViewB173.of(player);pull();
        try(var views=EntityViewsB173.callbackScope()){
            boolean result=(Boolean)info.interact().invoke(legacy,view);view.writeBack();push();
            return result?InteractionResult.sidedSuccess(level().isClientSide):InteractionResult.PASS;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Beta mob interaction failed",e);}
        finally{EntityViewsB173.flush();}
    }
    @Override public void playAmbientSound(){
        var info=MobsB173.info(getType());
        if(legacy==null||info.ambient()==null){super.playAmbientSound();return;}
        try{
            String name=(String)info.ambient().invoke(legacy);
            if(name!=null)World.mcc$wrap(level()).playSound(legacy,name,getSoundVolume(),getVoicePitch());
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Beta mob sound callback failed",e);}
    }
    @Override public Cow getBreedOffspring(net.minecraft.server.level.ServerLevel level,AgeableMob other){return (Cow)getType().create(level);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);if(legacy==null)return;pull();
        var data=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();legacy.writeNbt(data);tag.put("beta",NbtB173.toModern(data));
        tag.putFloat("betaWidth",legacy.width);tag.putFloat("betaHeight",legacy.height);tag.putString("betaTexture",texturePath());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);if(legacy==null)return;pull();
        if(tag.contains("betaWidth"))legacy.width=tag.getFloat("betaWidth");if(tag.contains("betaHeight"))legacy.height=tag.getFloat("betaHeight");
        legacy.readNbt(NbtB173.fromModern(tag.getCompound("beta")));push();
        if(tag.contains("betaTexture"))entityData.set(TEXTURE,tag.getString("betaTexture"));
    }
}
