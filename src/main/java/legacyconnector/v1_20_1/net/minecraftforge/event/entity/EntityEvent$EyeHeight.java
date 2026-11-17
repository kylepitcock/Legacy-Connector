package legacyconnector.v1_20_1.net.minecraftforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * Forge 1.20.1's eye height event ({@code EntityEvent.EyeHeight}, posted whenever the eye height of a pose was asked). In 1.21.1 the eye height is part of
 * {@code EntityDimensions}, and {@code EntityEvent.Size} is the one hook; the layer posts this event from it (Forge1201Events) and applies the new height
 * with {@code EntityDimensions.withEyeHeight}.
 */
public class EntityEvent$EyeHeight extends EntityEvent {
    private final Pose pose;
    private final EntityDimensions size;
    private final float oldHeight;
    private float newHeight;

    public EntityEvent$EyeHeight(Entity entity, Pose pose, EntityDimensions size, float eyeHeight) {
        super(entity);
        this.pose = pose;
        this.size = size;
        this.oldHeight = eyeHeight;
        this.newHeight = eyeHeight;
    }

    public Pose getPose() {
        return pose;
    }

    public EntityDimensions getSize() {
        return size;
    }

    public float getOldHeight() {
        return oldHeight;
    }

    public float getNewHeight() {
        return newHeight;
    }

    public void setNewHeight(float newHeight) {
        this.newHeight = newHeight;
    }
}
