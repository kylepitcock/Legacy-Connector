package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/** A tracked modern arrow whose server flight, collision, damage and pickup run the mod's original code. */
public final class LegacyB173Arrow extends Arrow {
    public Entity legacy;
    public LegacyB173Arrow(EntityType<? extends Arrow> type, Level level) { super(type, level); }
    public static boolean spawn(Level level, Entity entity) {
        if (level.isClientSide) return false;
        try {
            entity.getClass().getMethod("setArrowHeading", double.class,double.class,double.class,float.class,float.class);
        } catch (NoSuchMethodException e) { return false; }
        var host = new LegacyB173Arrow(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ARROW.get(), level);
        host.legacy = entity;
        entity.mcc$modern = host;
        try {
            Object owner=entity.getClass().getField("owner").get(entity);
            if (owner instanceof Entity view && view.mcc$modern != null) host.setOwner(view.mcc$modern);
        } catch (ReflectiveOperationException ignored) { }
        host.sync();
        return level.addFreshEntity(host);
    }
    private void sync() {
        setPos(legacy.x,legacy.y,legacy.z);
        setDeltaMovement(legacy.velocityX,legacy.velocityY,legacy.velocityZ);
        setYRot(legacy.yaw); setXRot(legacy.pitch);
        setRemainingFireTicks(legacy.fireTicks);
    }
    @Override public void tick() {
        if (level().isClientSide) { super.tick(); return; }
        if (legacy == null) { discard(); return; }
        xo=getX(); yo=getY(); zo=getZ(); yRotO=getYRot(); xRotO=getXRot();
        try {
            if (getOwner() != null) {
                try {
                    var field=legacy.getClass().getField("owner");
                    if (field.get(legacy)==null) field.set(legacy,EntityViewsB173.wrap(getOwner()));
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
        try {
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
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Could not restore legacy projectile {}",tag.getString("betaClass"),e);
        }
        if (legacy==null) discard();
    }

    /** Some mods create arrows without registering entity ids. Only restore arrow classes from a currently loaded Beta mod's module. */
    private static Class<?> savedType(String name) {
        if (!name.startsWith("legacyconnector_b173.")) return null;
        for (var mod : ContentB173.loadedMods()) {
            try {
                Class<?> type=Class.forName(name,false,mod.getClass().getClassLoader());
                if (type.getModule()!=mod.getClass().getModule() || !Entity.class.isAssignableFrom(type)) continue;
                type.getMethod("setArrowHeading",double.class,double.class,double.class,float.class,float.class);
                return type;
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
        return null;
    }
}
