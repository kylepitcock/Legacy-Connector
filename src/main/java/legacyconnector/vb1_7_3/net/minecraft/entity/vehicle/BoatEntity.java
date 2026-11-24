package legacyconnector.vb1_7_3.net.minecraft.entity.vehicle;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta's boat. The game's own boats are shown to mods as this type (the wobble fields as of the moment the view was made); a plain
 * {@code new BoatEntity(world, x, y, z)} spawned into a world becomes a native boat, a mod's own subclass is hosted as a neutral entity.
 */
public class BoatEntity extends Entity {
    public int damageWobbleTicks, damageWobbleStrength, damageWobbleSide = 1;

    public BoatEntity(World world) {
        super(world);
        setBoundingBoxSpacing(1.5F, 0.6F);
        standingEyeHeight = height / 2.0F;
    }

    public BoatEntity(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y + standingEyeHeight, z);
        prevX = x;
        prevY = y;
        prevZ = z;
    }
}
