package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

/** Beta model callbacks. Vanilla's base implementations intentionally do nothing. */
public abstract class EntityModel {
    public float handSwingProgress;
    public boolean riding;
    public void animateModel(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,float limb,float amount,float partial) {}
    public void render(float limb,float amount,float age,float yaw,float pitch,float scale) {}
    public void setAngles(float limb,float amount,float age,float yaw,float pitch,float scale) {}
}
