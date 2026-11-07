package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Views touched by one projectile tick; direct legacy fire-field writes are applied afterwards. */
public final class EntityViewsB173 {
    private static final ThreadLocal<java.util.List<Entity>> TOUCHED = ThreadLocal.withInitial(java.util.ArrayList::new);
    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof LegacyB173Arrow arrow && arrow.legacy != null) return arrow.legacy;
        Entity view = modern instanceof net.minecraft.world.entity.player.Player p ? PlayerViewB173.of(p)
                : new legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity(World.mcc$wrap(modern.level())) {};
        view.mcc$modern = modern;
        if (!(modern instanceof net.minecraft.world.entity.player.Player)) view.setPosition(modern.getX(), modern.getY(), modern.getZ());
        var b = modern.getBoundingBox();
        view.boundingBox = legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
        view.fireTicks = modern.getRemainingFireTicks();
        TOUCHED.get().add(view);
        return view;
    }
    public static void flush() {
        for (Entity view : TOUCHED.get()) {
            if (view.mcc$modern != null && view.fireTicks > view.mcc$modern.getRemainingFireTicks()) view.mcc$modern.setRemainingFireTicks(view.fireTicks);
        }
        TOUCHED.remove();
    }
}
