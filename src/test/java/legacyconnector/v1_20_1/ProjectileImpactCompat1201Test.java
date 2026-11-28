package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.junit.jupiter.api.Test;

class ProjectileImpactCompat1201Test {
    @Test
    void skipEntityCancelsOnlyAnEntityHit() {
        ProjectileImpactEvent entityHit = new ProjectileImpactEvent(null, new EntityHitResult(null, Vec3.ZERO));
        Compat1201.skipProjectileEntityImpact(entityHit);
        assertTrue(entityHit.isCanceled());

        ProjectileImpactEvent blockHit = new ProjectileImpactEvent(null,
                new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false));
        Compat1201.skipProjectileEntityImpact(blockHit);
        assertFalse(blockHit.isCanceled());
    }

    @Test
    void blockHitPreservesAnEarlierListenersCancellation() {
        ProjectileImpactEvent event = new ProjectileImpactEvent(null,
                new BlockHitResult(Vec3.ZERO, Direction.UP, BlockPos.ZERO, false));
        event.setCanceled(true);
        Compat1201.skipProjectileEntityImpact(event);
        assertTrue(event.isCanceled());
    }
}
