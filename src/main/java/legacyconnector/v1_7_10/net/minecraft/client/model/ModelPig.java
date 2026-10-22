package legacyconnector.v1_7_10.net.minecraft.client.model;

public class ModelPig extends ModelQuadruped {
    public ModelPig() {
        this(0.0F);
    }

    public ModelPig(float grow) {
        super(6, grow);
        head.setTextureOffset(16, 16).addBox(-2.0F, 0.0F, -9.0F, 4, 3, 1, grow);
        field_78145_g = 4.0F;
    }
}
