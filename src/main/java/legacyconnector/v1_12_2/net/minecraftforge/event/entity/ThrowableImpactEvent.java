package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** The pre-ProjectileImpactEvent form of a throwable hit (cancel to let it fly on); posted next to {@link ProjectileImpactEvent.Throwable}. */
@Cancelable
public class ThrowableImpactEvent extends EntityEvent {
    private final EntityThrowable throwable;
    private final RayTraceResult ray;

    public ThrowableImpactEvent(EntityThrowable throwable, RayTraceResult ray) {
        super(throwable);
        this.throwable = throwable;
        this.ray = ray;
    }

    public EntityThrowable getEntityThrowable() {
        return throwable;
    }

    public RayTraceResult getRayTraceResult() {
        return ray;
    }
}
