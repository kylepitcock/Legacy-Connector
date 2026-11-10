package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Server original lifecycle; client runs native presentation only, never original damage/fire. */
public class LegacyB173Lightning extends net.minecraft.world.entity.LightningBolt implements B173EntityHost {
    /** Private to this hosted entity type; native packet special cases 21/35/63 are intentionally avoided. */
    public static final byte FIRST_BASELINE_EVENT=120;
    private static final EntityDataAccessor<String> ORIGINAL_CLASS=SynchedEntityData.defineId(LegacyB173Lightning.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> ORIGINAL_STATE=SynchedEntityData.defineId(LegacyB173Lightning.class,EntityDataSerializers.COMPOUND_TAG);
    private final NeutralEntityStateB173 state=new NeutralEntityStateB173(this,this);
    private int presentedBaseTicks;
    public LegacyB173Lightning(EntityType<? extends LegacyB173Lightning> type,Level level){super(type,level);}
    public void bind(LightningEntity original){state.attach(original);seed=original.seed;state.flush();publishRenderState();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ORIGINAL_CLASS,"");builder.define(ORIGINAL_STATE,new CompoundTag());}
    @Override public Entity legacy(){return state==null?null:state.original;}
    @Override public boolean callbackActive(){return state.callbacks>0||state.syncing;}
    @Override public void syncView(){state.sync();if(legacy() instanceof LightningEntity bolt)bolt.seed=seed;}
    @Override public void flushView(){state.flush();if(legacy() instanceof LightningEntity bolt)seed=bolt.seed;}
    @Override public void publishRenderState(){if(legacy()!=null){entityData.set(ORIGINAL_CLASS,legacy().getClass().getName());var snapshot=state.snapshot();snapshot.putLong("$seed",seed);snapshot.putInt("$baseTicks",state.baseTicks);entityData.set(ORIGINAL_STATE,snapshot);}}
    @Override public Entity prepareRenderView(float partial){var snapshot=entityData.get(ORIGINAL_STATE);var view=state.render(entityData.get(ORIGINAL_CLASS),snapshot,partial);if(view instanceof LightningEntity bolt){seed=snapshot.getLong("$seed");bolt.seed=seed;}return view;}
    @Override public void tick(){
        if(level().isClientSide){presentUntil(entityData.get(ORIGINAL_STATE).getInt("$baseTicks"));return;}
        if(legacy()==null){discard();return;}
        state.tick();if(legacy() instanceof LightningEntity bolt)seed=bolt.seed;publishRenderState();
    }
    @Override public void mcc$baseTick(){state.beforeBaseTick();state.flush();if(legacy() instanceof LightningEntity bolt)seed=bolt.seed;state.baseTicks++;super.tick();if(state.baseTicks==1&&level() instanceof net.minecraft.server.level.ServerLevel server)server.broadcastEntityEvent(this,FIRST_BASELINE_EVENT);state.sync();if(legacy() instanceof LightningEntity bolt)bolt.seed=seed;}
    @Override public void handleEntityEvent(byte event){
        if(event==FIRST_BASELINE_EVENT&&level().isClientSide){
            // A short-lived original can disappear before its dirty metadata is sent. The ordered
            // native event follows spawn and precedes removal, and enters only native client effects.
            presentUntil(1);
        }else super.handleEntityEvent(event);
    }
    private void presentUntil(int target){presentedBaseTicks=advancePresentation(presentedBaseTicks,target,this::isRemoved,super::tick);}
    static int advancePresentation(int presented,int target,java.util.function.BooleanSupplier removed,Runnable baseline){
        while(presented<target&&!removed.getAsBoolean()){baseline.run();presented++;}
        return presented;
    }
    @Override public int originalTickCount(){return state.originalTicks;}
    @Override public int baseTickCount(){return state.baseTicks;}
    @Override public void baseMove(double x,double y,double z){super.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(x,y,z));}
    @Override public boolean baseDamage(Entity attacker,int amount){return false;}
    @Override public void baseMarkDead(){discard();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);state.save(tag);tag.putLong("betaSeed",seed);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);state.load(tag);seed=tag.getLong("betaSeed");if(legacy() instanceof LightningEntity bolt)bolt.seed=seed;publishRenderState();}
}
