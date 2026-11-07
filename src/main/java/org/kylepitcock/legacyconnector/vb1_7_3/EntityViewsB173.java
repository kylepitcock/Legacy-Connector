package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Entity views touched by callbacks; apply changed velocity, ground and fire fields afterwards. */
public final class EntityViewsB173 {
    private record Touched(Entity view,double vx,double vy,double vz,boolean ground) {}
    private static final ThreadLocal<java.util.List<Touched>> TOUCHED = ThreadLocal.withInitial(java.util.ArrayList::new);
    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof LegacyB173Arrow arrow && arrow.legacy != null) return arrow.legacy;
        Entity view = modern instanceof LegacyB173Cow cow && cow.legacy()!=null ? cow.legacy()
                : modern instanceof net.minecraft.world.entity.player.Player p ? PlayerViewB173.of(p)
                : modern instanceof net.minecraft.world.entity.item.ItemEntity item
                ? new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(World.mcc$wrap(modern.level()),modern.getX(),modern.getY(),modern.getZ(),StacksB173.toLegacy(item.getItem()))
                : modern instanceof net.minecraft.world.entity.LivingEntity ? new legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity(World.mcc$wrap(modern.level())) {}
                : new Entity(World.mcc$wrap(modern.level())) {};
        view.mcc$modern = modern;
        if (!(modern instanceof net.minecraft.world.entity.player.Player)) view.setPosition(modern.getX(), modern.getY(), modern.getZ());
        var b = modern.getBoundingBox();
        view.boundingBox = legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
        view.fireTicks = modern.getRemainingFireTicks();
        view.velocityX=modern.getDeltaMovement().x;view.velocityY=modern.getDeltaMovement().y;view.velocityZ=modern.getDeltaMovement().z;
        view.onGround=modern.onGround();
        TOUCHED.get().add(new Touched(view,view.velocityX,view.velocityY,view.velocityZ,view.onGround));
        return view;
    }
    public static void flush() {
        for (var touched : TOUCHED.get()) {
            Entity view=touched.view;
            if (view.mcc$modern != null && view.fireTicks > view.mcc$modern.getRemainingFireTicks()) view.mcc$modern.setRemainingFireTicks(view.fireTicks);
            if(view.mcc$modern!=null) {
                var motion=new net.minecraft.world.phys.Vec3(view.velocityX,view.velocityY,view.velocityZ);
                if(view.velocityX!=touched.vx || view.velocityY!=touched.vy || view.velocityZ!=touched.vz) { view.mcc$modern.setDeltaMovement(motion);view.mcc$modern.hurtMarked=true; }
                if(view.onGround!=touched.ground)view.mcc$modern.setOnGround(view.onGround);
            }
        }
        TOUCHED.remove();
    }
}
