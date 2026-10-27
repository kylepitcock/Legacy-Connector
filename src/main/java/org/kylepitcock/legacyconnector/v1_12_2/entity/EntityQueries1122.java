package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/** Entity searches in legacy terms (by legacy class, over the modern level's entities). */
public final class EntityQueries1122 {
    private EntityQueries1122() {}

    /** The legacy views of the entities in a box that are instances of {@code cls} and pass the filter (except {@code except}). */
    @SuppressWarnings("unchecked")
    public static <T> List<T> within(net.minecraft.world.level.Level level, AxisAlignedBB box, Class<? extends T> cls, Entity except,
            java.util.function.Predicate<? super T> filter) {
        List<T> out = new ArrayList<>();
        if (level == null || box == null) return out;
        var exceptModern = except == null ? null : except.mcc$entity;
        for (var modern : level.getEntities(exceptModern, box.mcc$modern())) {
            Entity view = EntityBridge1122.wrap(modern);
            if (view == null || cls != null && !cls.isInstance(view)) continue;
            T t = (T) view;
            try {
                if (filter == null || filter.test(t)) out.add(t);
            } catch (RuntimeException | LinkageError e) {
                // a mod's filter that cannot handle this entity
            }
        }
        return out;
    }

    /** The nearest of {@link #within} to {@code to}, or null. */
    public static <T extends Entity> T nearest(net.minecraft.world.level.Level level, AxisAlignedBB box, Class<? extends T> cls, Entity to,
            java.util.function.Predicate<? super T> filter) {
        T best = null;
        double bestDist = Double.MAX_VALUE;
        for (T e : EntityQueries1122.<T>within(level, box, cls, to, filter)) {
            double d = to.getDistanceSq(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /** The nearest player within {@code distance} (any distance when negative) that passes the filter, or null. */
    public static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer nearestPlayer(net.minecraft.world.level.Level level, double x, double y,
            double z, double distance, java.util.function.Predicate<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> filter) {
        if (level == null) return null;
        legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (var p : level.players()) {
            if (p.isSpectator()) continue;
            double d = p.distanceToSqr(x, y, z);
            if (distance >= 0 && d >= distance * distance || d >= bestDist) continue;
            if (!(EntityBridge1122.wrap(p) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer lp)) continue;
            if (filter != null && !filter.test(lp)) continue;
            best = lp;
            bestDist = d;
        }
        return best;
    }
}
