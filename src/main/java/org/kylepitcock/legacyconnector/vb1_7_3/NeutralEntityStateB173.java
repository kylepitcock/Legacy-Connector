package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.CompoundTag;

/** Shared transaction and original-state ownership for the two finite native hosts. */
final class NeutralEntityStateB173 {
    final net.minecraft.world.entity.Entity nativeEntity;
    final B173EntityHost host;
    Entity original;
    int callbacks,originalTicks,baseTicks;
    boolean syncing,rendering,moving;
    CompoundTag previousSnapshot;
    NeutralEntityStateB173(net.minecraft.world.entity.Entity nativeEntity,B173EntityHost host){this.nativeEntity=nativeEntity;this.host=host;}
    void attach(Entity value){
        if(value!=null&&!NeutralEntitiesB173.supports(value.getClass()))throw new IllegalArgumentException(NeutralEntitiesB173.unsupportedReason(value.getClass()));
        original=value;if(value!=null){value.mcc$modern=nativeEntity;value.world=World.mcc$wrap(nativeEntity.level());}
    }
    void sync(){
        if(original==null||syncing)return;syncing=true;
        try{
            original.world=World.mcc$wrap(nativeEntity.level());
            original.setPosition(nativeEntity.getX(),nativeEntity.getY()+original.standingEyeHeight,nativeEntity.getZ());
            if(!rendering&&!nativeEntity.level().isClientSide){var bounds=nativeEntity.getBoundingBox();original.boundingBox=Box.create(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ);}
            var velocity=nativeEntity.getDeltaMovement();original.velocityX=velocity.x;original.velocityY=velocity.y;original.velocityZ=velocity.z;
            original.yaw=nativeEntity.getYRot();original.pitch=nativeEntity.getXRot();
            original.onGround=nativeEntity.onGround();original.dead=nativeEntity.isRemoved();original.fireTicks=nativeEntity.getRemainingFireTicks();
            original.air=nativeEntity.getAirSupply();original.mcc$fallDistance(nativeEntity.fallDistance);original.mcc$environment(nativeEntity.isInWater());
            original.horizontalCollision=nativeEntity.horizontalCollision;original.verticalCollision=nativeEntity.verticalCollision;
            original.hasCollided=original.horizontalCollision||original.verticalCollision;if(!rendering&&!nativeEntity.level().isClientSide)original.noClip=nativeEntity.noPhysics;original.blocksSameBlockSpawning=nativeEntity.blocksBuilding;
            original.vehicle=relation(nativeEntity.getVehicle());original.passenger=relation(nativeEntity.getFirstPassenger());
            if(original instanceof ItemEntity item&&nativeEntity instanceof net.minecraft.world.entity.item.ItemEntity nativeItem){
                item.stack=StacksB173.toLegacy(nativeItem.getItem());if(!rendering&&!nativeEntity.level().isClientSide){item.itemAge=EntityFieldsB173.itemAge(nativeItem);item.pickupDelay=EntityFieldsB173.pickupDelay(nativeItem);}
            }
            EntityViewsB173.refreshReferences(original);
        }finally{syncing=false;}
    }
    private Entity relation(net.minecraft.world.entity.Entity entity){return entity instanceof B173EntityHost other&&other.legacy()!=null?other.legacy():EntityViewsB173.wrap(entity);}
    void flush(){
        if(original==null)return;
        var bounds=original.boundingBox.mcc$box();
        double rawX=original.x,rawY=original.y-original.standingEyeHeight,rawZ=original.z;
        nativeEntity.setPos(original.x,original.y-original.standingEyeHeight,original.z);
        nativeEntity.setYRot(original.yaw);nativeEntity.setXRot(original.pitch);
        nativeEntity.setDeltaMovement(original.velocityX,original.velocityY,original.velocityZ);
        nativeEntity.setOnGround(original.onGround);nativeEntity.setRemainingFireTicks(original.fireTicks);
        nativeEntity.setAirSupply(original.air);nativeEntity.fallDistance=original.mcc$fallDistance();nativeEntity.noPhysics=original.noClip;nativeEntity.blocksBuilding=original.blocksSameBlockSpawning;
        if(original instanceof ItemEntity item&&nativeEntity instanceof net.minecraft.world.entity.item.ItemEntity nativeItem){
            nativeItem.setItem(item.stack==null?net.minecraft.world.item.ItemStack.EMPTY:StacksB173.toModern(item.stack));
            EntityFieldsB173.itemAge(nativeItem,item.itemAge);nativeItem.setPickUpDelay(item.pickupDelay);
        }
        net.minecraft.world.entity.Entity vehicle=original.vehicle==null?null:original.vehicle.mcc$modern;
        if(nativeEntity.getVehicle()!=vehicle){if(vehicle==null)nativeEntity.stopRiding();else nativeEntity.startRiding(vehicle,true);}
        // Native position/relationship setters may rebuild bounds. Beta owns raw
        // coordinates and its six box coordinates independently until movement.
        nativeEntity.setPos(rawX,rawY,rawZ);nativeEntity.setBoundingBox(bounds);
        if(original.dead)nativeEntity.discard();
    }
    /** Beta movement begins with the existing box, including after raw coordinate or size writes. */
    void move(Runnable nativeMove){
        if(moving)throw new IllegalStateException("Recursive Beta neutral movement");
        flush();var box=nativeEntity.getBoundingBox();
        nativeEntity.setPosRaw((box.minX+box.maxX)/2,box.minY,(box.minZ+box.maxZ)/2);
        moving=true;
        try{nativeMove.run();}finally{moving=false;sync();}
    }
    /** Native move calls setPos; translate the original box instead of rebuilding it from size fields. */
    boolean movePosition(double x,double y,double z){
        if(!moving)return false;
        var box=nativeEntity.getBoundingBox().move(x-nativeEntity.getX(),y-nativeEntity.getY(),z-nativeEntity.getZ());
        nativeEntity.setPosRaw(x,y,z);nativeEntity.setBoundingBox(box);return true;
    }
    void callback(Runnable action){
        if(original==null)return;
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(nativeEntity.level()),original);
            var views=EntityViewsB173.callbackScope()){
            boolean outer=callbacks==0,referenced=false;
            if(outer){referenced=EntityViewsB173.prepareHostCallback(host);sync();}callbacks++;
            try{action.run();flush();if(outer)host.publishRenderState();}
            finally{callbacks--;if(outer)EntityViewsB173.resumeHostCallback(host,referenced);}
        }
    }
    void tick(){callback(()->{originalTicks++;original.tick();});}
    void beforeBaseTick(){
        original.age++;original.prevHorizontalSpeed=original.horizontalSpeed;
        original.prevX=original.x;original.prevY=original.y;original.prevZ=original.z;
        original.prevYaw=original.yaw;original.prevPitch=original.pitch;
        nativeEntity.tickCount=original.age;
    }
    void pickup(net.minecraft.world.entity.player.Player player){callback(()->{
        var view=PlayerViewB173.of(player);original.onPlayerInteraction(view);view.writeBack();
    });}
    void save(CompoundTag tag){
        if(original==null)return;
        tag.putString("betaClass",original.getClass().getName());
        var custom=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();
        callback(()->original.writeNbt(custom));tag.put("beta",NbtB173.toModern(custom));
    }
    void load(CompoundTag tag){
        attach(NeutralEntitiesB173.restore(tag.getString("betaClass"),nativeEntity.level()));
        if(original==null){nativeEntity.discard();return;}
        sync();callback(()->original.readNbt(NbtB173.fromModern(tag.getCompound("beta"))));
    }
    Entity render(String className,CompoundTag snapshot,float partial){
        if(className.isEmpty()||!snapshot.getBoolean("$ready"))return null;
        if(original==null){attach(NeutralEntitiesB173.restore(className,nativeEntity.level()));if(original==null)return null;}
        AnimalRenderStateB173.apply(original,previousSnapshot,snapshot);
        applyCommon(snapshot.getCompound("$common"),previousSnapshot==null?null:previousSnapshot.getCompound("$common"));
        previousSnapshot=snapshot.copy();
        rendering=true;try{sync();}finally{rendering=false;}
        original.prevX=nativeEntity.xo;original.prevY=nativeEntity.yo+original.standingEyeHeight;original.prevZ=nativeEntity.zo;
        original.prevYaw=nativeEntity.yRotO;original.prevPitch=nativeEntity.xRotO;return original;
    }
    CompoundTag snapshot(){
        var snapshot=AnimalRenderStateB173.capture(original);var common=new CompoundTag();
        common.putFloat("width",original.width);common.putFloat("height",original.height);common.putFloat("eye",original.standingEyeHeight);
        common.putInt("age",original.age);common.putDouble("distance",original.renderDistanceMultiplier);common.putBoolean("noClip",original.noClip);
        if(original instanceof ItemEntity item){common.putInt("itemAge",item.itemAge);common.putInt("pickupDelay",item.pickupDelay);common.putFloat("rotation",item.initialRotationAngle);}
        snapshot.put("$common",common);snapshot.putBoolean("$ready",true);return snapshot;
    }
    private static boolean changed(CompoundTag incoming,CompoundTag previous,String key){return previous==null||!java.util.Objects.equals(incoming.get(key),previous.get(key));}
    private void applyCommon(CompoundTag common,CompoundTag previous){
        if(changed(common,previous,"width"))original.width=common.getFloat("width");
        if(changed(common,previous,"height"))original.height=common.getFloat("height");
        if(changed(common,previous,"eye"))original.standingEyeHeight=common.getFloat("eye");
        if(changed(common,previous,"age"))original.age=common.getInt("age");
        if(changed(common,previous,"distance"))original.renderDistanceMultiplier=common.getDouble("distance");
        if(changed(common,previous,"noClip"))original.noClip=common.getBoolean("noClip");
        if(original instanceof ItemEntity item){
            if(changed(common,previous,"itemAge"))item.itemAge=common.getInt("itemAge");
            if(changed(common,previous,"pickupDelay"))item.pickupDelay=common.getInt("pickupDelay");
            if(changed(common,previous,"rotation"))item.initialRotationAngle=common.getFloat("rotation");
        }
        double x=nativeEntity.getX(),y=nativeEntity.getY(),z=nativeEntity.getZ();
        nativeEntity.refreshDimensions();nativeEntity.setPos(x,y,z);
        nativeEntity.setBoundingBox(new net.minecraft.world.phys.AABB(x-original.width/2,y,z-original.width/2,x+original.width/2,y+original.height,z+original.width/2));
    }
}
