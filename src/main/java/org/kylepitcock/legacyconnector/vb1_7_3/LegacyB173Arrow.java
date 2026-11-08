package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/** Shared tracked host for Beta arrows and thrown projectiles; the mod owns server physics and impacts. */
public final class LegacyB173Arrow extends Arrow {
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> TEXTURE = net.minecraft.network.syncher.SynchedEntityData.defineId(
            LegacyB173Arrow.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<net.minecraft.world.item.ItemStack> SPRITE = net.minecraft.network.syncher.SynchedEntityData.defineId(
            LegacyB173Arrow.class, net.minecraft.network.syncher.EntityDataSerializers.ITEM_STACK);
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);builder.define(TEXTURE,ProjectileTexturesB173.FALLBACK.toString());
        builder.define(SPRITE,net.minecraft.world.item.ItemStack.EMPTY);
    }
    public net.minecraft.resources.ResourceLocation texture() { return net.minecraft.resources.ResourceLocation.parse(entityData.get(TEXTURE)); }
    public Entity legacy;
    public net.minecraft.world.item.ItemStack sprite() { return entityData.get(SPRITE); }
    public boolean isThrown() { return getType()==org.kylepitcock.legacyconnector.LegacyConnector.BETA_THROWN.get(); }
    private static boolean heading(Class<?> type,String name) {
        try { type.getMethod(name,double.class,double.class,double.class,float.class,float.class);return true; }
        catch(NoSuchMethodException e) { return false; }
    }
    private static boolean supported(Class<?> type) {
        return heading(type,"setArrowHeading") || heading(type,"setSnowballHeading") || heading(type,"setEggHeading");
    }
    private static java.lang.reflect.Field shooterField(Class<?> type) {
        java.lang.reflect.Field candidate=null;
        for(Class<?> c=type;c!=null && c!=Entity.class;c=c.getSuperclass())for(var field:c.getDeclaredFields()) {
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !Entity.class.isAssignableFrom(field.getType()))continue;
            if(field.getName().equals("owner") || field.getName().equals("thrower")) { field.setAccessible(true);return field; }
            if(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity.class.isAssignableFrom(field.getType())) {
                if(candidate!=null)return null;
                candidate=field;
            }
        }
        if(candidate!=null)candidate.setAccessible(true);
        return candidate;
    }
    public LegacyB173Arrow(EntityType<? extends Arrow> type, Level level) { super(type, level); }
    public static boolean spawn(Level level, Entity entity) {
        if (level.isClientSide) return false;
        if(!supported(entity.getClass()))return false;
        var type=heading(entity.getClass(),"setArrowHeading") ? org.kylepitcock.legacyconnector.LegacyConnector.BETA_ARROW.get()
                : org.kylepitcock.legacyconnector.LegacyConnector.BETA_THROWN.get();
        var host = new LegacyB173Arrow(type, level);
        host.legacy = entity;
        entity.mcc$modern = host;
        try {
            var field=shooterField(entity.getClass());
            Object owner=field==null?null:field.get(entity);
            if (owner instanceof Entity view && view.mcc$modern != null) host.setOwner(view.mcc$modern);
        } catch (ReflectiveOperationException ignored) { }
        host.sync();
        host.entityData.set(SPRITE,ProjectileTexturesB173.sprite(entity.getClass()));
        return level.addFreshEntity(host);
    }
    private void sync() {
        entityData.set(TEXTURE,ProjectileTexturesB173.of(legacy.getClass()).toString());
        setPos(legacy.x,legacy.y,legacy.z);
        setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);
        setYRot(legacy.yaw); setXRot(legacy.pitch);
        setRemainingFireTicks(legacy.fireTicks);
    }
    @Override public void tick() {
        if (level().isClientSide) {
            if(!isThrown())super.tick();
            else { xo=getX();yo=getY();zo=getZ();setPos(position().add(getDeltaMovement())); }
            return;
        }
        if (legacy == null) { discard(); return; }
        xo=getX(); yo=getY(); zo=getZ(); yRotO=getYRot(); xRotO=getXRot();
        try (var views=EntityViewsB173.callbackScope()) {
            if (getOwner() != null) {
                try {
                    var field=shooterField(legacy.getClass());
                    if (field!=null && field.get(legacy)==null) field.set(legacy,EntityViewsB173.wrap(getOwner()));
                } catch (ReflectiveOperationException ignored) { }
            }
            legacy.tick();
            sync();
            if (legacy.dead) discard();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("Legacy projectile {} failed", legacy.getClass().getName(), e);
            discard();
        } finally { EntityViewsB173.flush(); }
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player) {
        if (level().isClientSide || legacy == null) return;
        var view=PlayerViewB173.of(player);
        try (var views=EntityViewsB173.callbackScope()) {
            legacy.onPlayerInteraction(view);
            view.writeBack();
            if (legacy.dead) discard();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("Legacy projectile pickup failed",e);
            discard();
        }
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (legacy == null) return;
        tag.putString("betaClass",legacy.getClass().getName());
        var data=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();
        legacy.writeNbt(data); tag.put("beta",NbtB173.toModern(data));
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            Class<?> type=savedType(tag.getString("betaClass"));
            if (type != null) {
                legacy=(Entity)type.getConstructor(World.class).newInstance(World.mcc$wrap(level()));
                legacy.mcc$modern=this;
                legacy.setPositionAndAngles(getX(),getY(),getZ(),getYRot(),getXRot());
                legacy.velocityX=getDeltaMovement().x;legacy.velocityY=getDeltaMovement().y;legacy.velocityZ=getDeltaMovement().z;
                legacy.readNbt(NbtB173.fromModern(tag.getCompound("beta")));
                entityData.set(TEXTURE,ProjectileTexturesB173.of(legacy.getClass()).toString());
                entityData.set(SPRITE,ProjectileTexturesB173.sprite(legacy.getClass()));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Could not restore legacy projectile {}",tag.getString("betaClass"),e);
        }
        if (legacy==null) discard();
    }

    /** Some mods omit entity ids. Only restore supported projectile classes from a currently loaded Beta mod's module. */
    private static Class<?> savedType(String name) {
        if (!name.startsWith("legacyconnector_b173.")) return null;
        for (var mod : ContentB173.loadedMods()) {
            try {
                Class<?> type=Class.forName(name,false,mod.getClass().getClassLoader());
                if (type.getModule()!=mod.getClass().getModule() || !Entity.class.isAssignableFrom(type)) continue;
                if(!supported(type))continue;
                return type;
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
        return null;
    }
}
