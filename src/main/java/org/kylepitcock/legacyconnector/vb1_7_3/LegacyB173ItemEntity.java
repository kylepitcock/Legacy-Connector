package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Original nonliving lifecycle; native superclass behavior only follows original super calls. */
public final class LegacyB173ItemEntity extends net.minecraft.world.entity.item.ItemEntity implements B173EntityHost {
    private static final EntityDataAccessor<String> ORIGINAL_CLASS=SynchedEntityData.defineId(LegacyB173ItemEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> ORIGINAL_STATE=SynchedEntityData.defineId(LegacyB173ItemEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private final NeutralEntityStateB173 state=new NeutralEntityStateB173(this,this);
    private DamageSource retainedDamage;
    private MoverType retainedMover=MoverType.SELF;
    public LegacyB173ItemEntity(EntityType<? extends LegacyB173ItemEntity> type,Level level){super(type,level);}
    public void bind(Entity original){state.attach(original);state.flush();publishRenderState();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ORIGINAL_CLASS,"");builder.define(ORIGINAL_STATE,new CompoundTag());}
    @Override public void publishRenderState(){if(state.original!=null){entityData.set(ORIGINAL_CLASS,state.original.getClass().getName());entityData.set(ORIGINAL_STATE,state.snapshot());}}
    @Override public Entity legacy(){return state==null?null:state.original;}
    @Override public Entity prepareRenderView(float partial){return state.render(entityData.get(ORIGINAL_CLASS),entityData.get(ORIGINAL_STATE),partial);}
    public void applyTrackedRenderData(){state.render(entityData.get(ORIGINAL_CLASS),entityData.get(ORIGINAL_STATE),0);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(state!=null&&level().isClientSide&&(key==ORIGINAL_CLASS||key==ORIGINAL_STATE))applyTrackedRenderData();}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return legacy()==null?super.shouldRenderAtSqrDistance(distance):legacy().shouldRender(distance);}
    @Override public float getPickRadius(){return legacy()==null?super.getPickRadius():legacy().getTargetingMargin();}
    @Override public boolean isPickable(){return legacy()!=null&&legacy().isCollidable();}
    @Override public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose){return legacy()==null?super.getDimensions(pose):net.minecraft.world.entity.EntityDimensions.scalable(legacy().width,legacy().height);}
    @Override public float maxUpStep(){return legacy()==null?super.maxUpStep():legacy().stepHeight;}
    @Override public void lerpMotion(double x,double y,double z){if(legacy()==null){super.lerpMotion(x,y,z);return;}state.callback(()->legacy().setVelocityClient(x,y,z));}
    @Override public boolean callbackActive(){return state.callbacks>0||state.syncing;}
    @Override public void syncView(){state.sync();}
    @Override public void flushView(){state.flush();}
    @Override public int originalTickCount(){return state.originalTicks;}
    @Override public int baseTickCount(){return state.baseTicks;}
    @Override public void tick(){
        if(level().isClientSide)return;
        if(legacy()==null){discard();return;}
        xo=getX();yo=getY();zo=getZ();yRotO=getYRot();xRotO=getXRot();
        state.tick();publishRenderState();
    }
    @Override public void mcc$baseTick(){state.beforeBaseTick();state.flush();state.baseTicks++;super.baseTick();state.sync();}
    @Override public void move(MoverType mover,Vec3 movement){
        if(legacy()==null){super.move(mover,movement);return;}
        MoverType previous=retainedMover;retainedMover=mover;
        try{state.callback(()->legacy().move(movement.x,movement.y,movement.z));}finally{retainedMover=previous;}
    }
    @Override public void baseMove(double x,double y,double z){state.flush();super.move(retainedMover,new Vec3(x,y,z));state.sync();}
    @Override public boolean hurt(DamageSource source,float amount){
        if(legacy()==null||level().isClientSide)return false;
        DamageSource previous=retainedDamage;retainedDamage=source;boolean[] result={false};
        try{state.callback(()->result[0]=legacy().damage(EntityViewsB173.wrap(source.getEntity()),(int)Math.ceil(amount)));return result[0];}
        finally{retainedDamage=previous;}
    }
    @Override public boolean baseDamage(Entity attacker,int amount){
        state.flush();boolean result=super.hurt(retainedDamage!=null?retainedDamage:damageSources().generic(),amount);state.sync();return result;
    }
    @Override public void baseMarkDead(){discard();}
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){if(!level().isClientSide)state.pickup(player);}
    @Override public void addAdditionalSaveData(CompoundTag tag){state.save(tag);}
    @Override public void readAdditionalSaveData(CompoundTag tag){state.load(tag);publishRenderState();}
}
