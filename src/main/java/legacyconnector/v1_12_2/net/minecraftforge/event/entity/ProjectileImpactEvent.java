package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A projectile hits something (cancel to let it fly on). */
@Cancelable
public class ProjectileImpactEvent extends EntityEvent {
    private final legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray;

    public ProjectileImpactEvent(Entity entity, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
        super(entity);
        this.ray = ray;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult getRayTraceResult() {
        return ray;
    }

    public static class Arrow extends ProjectileImpactEvent {
        private final legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow arrow;

        public Arrow(legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow arrow, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
            super(arrow, ray);
            this.arrow = arrow;
        }

        public legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow getArrow() {
            return arrow;
        }
    }

    public static class Throwable extends ProjectileImpactEvent {
        private final legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable throwable;

        public Throwable(legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable throwable, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
            super(throwable, ray);
            this.throwable = throwable;
        }

        public legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable getThrowable() {
            return throwable;
        }
    }
}
