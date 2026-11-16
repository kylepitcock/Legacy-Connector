package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code EntitySpider}: hunts only in the dark, leaps at its target from a few blocks away, climbs walls and drops string. Mods
 * extend it for their own spiders (Twilight Forest's hedge and swarm spiders).
 */
public class SpiderEntity extends MonsterEntity {
    public SpiderEntity(World world) {
        super(world);
        texture = "/mob/spider.png";
        setBoundingBoxSpacing(1.4F, 0.9F);
        movementSpeed = 0.8F;
    }

    @Override protected Entity getTargetInRange() {
        return getBrightnessAtEyes(1.0F) < 0.5F ? org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.monsterClosest(this, 16) : null;
    }

    @Override protected void attack(Entity victim, float distance) {
        if (getBrightnessAtEyes(1.0F) > 0.5F && random.nextInt(100) == 0) { target = null; return; }
        if (distance > 2.0F && distance < 6.0F && random.nextInt(10) == 0) {
            if (onGround) {
                double dx = victim.x - x, dz = victim.z - z;
                double length = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
                velocityX = dx / length * 0.5D * 0.8D + velocityX * 0.2D;
                velocityZ = dz / length * 0.5D * 0.8D + velocityZ * 0.2D;
                velocityY = 0.4D;
            }
        } else super.attack(victim, distance);
    }

    @Override protected int getDroppedItemId() { return 287; } // string

    @Override public boolean isOnLadder() { return horizontalCollision; }
}
