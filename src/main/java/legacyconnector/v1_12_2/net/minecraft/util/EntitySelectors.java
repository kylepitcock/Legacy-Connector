package legacyconnector.v1_12_2.net.minecraft.util;

import com.google.common.base.Predicate;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** Entity filters for the world's entity queries; they test the modern entity each legacy view stands for. */
public final class EntitySelectors {
    private EntitySelectors() {}

    private static net.minecraft.world.entity.Entity modern(Entity e) {
        return e == null ? null : e.mcc$modern();
    }

    public static final Predicate<Entity> IS_ALIVE = e -> {
        var m = modern(e);
        return m != null ? m.isAlive() : e != null && !e.isDead;
    };
    public static final Predicate<Entity> IS_STANDALONE = e -> {
        var m = modern(e);
        return m != null && m.isAlive() && !m.isVehicle() && !m.isPassenger();
    };
    public static final Predicate<Entity> HAS_INVENTORY = e -> {
        var m = modern(e);
        return m instanceof net.minecraft.world.Container && m.isAlive();
    };
    public static final Predicate<Entity> CAN_AI_TARGET = e -> {
        var m = modern(e);
        return !(m instanceof net.minecraft.world.entity.player.Player p) || !p.isSpectator() && !p.isCreative();
    };
    public static final Predicate<Entity> NOT_SPECTATING = e -> {
        var m = modern(e);
        return !(m instanceof net.minecraft.world.entity.player.Player p) || !p.isSpectator();
    };

    public static <T extends Entity> Predicate<T> withinRange(double x, double y, double z, double range) {
        double d = range * range;
        return e -> {
            var m = modern(e);
            return m != null && m.distanceToSqr(x, y, z) <= d;
        };
    }

    /** Who {@code entityIn} may collide with under the team collision rules. */
    public static <T extends Entity> Predicate<T> getTeamCollisionPredicate(Entity entityIn) {
        var self = modern(entityIn);
        return e -> {
            var m = modern(e);
            if (self == null || m == null) return true;
            return net.minecraft.world.entity.EntitySelector.pushableBy(self).test(m);
        };
    }

    public static Predicate<Entity> notRiding(Entity entity) {
        var self = modern(entity);
        return e -> {
            var m = modern(e);
            while (m != null && m.isPassenger()) {
                m = m.getVehicle();
                if (m == self) return false;
            }
            return true;
        };
    }
}
