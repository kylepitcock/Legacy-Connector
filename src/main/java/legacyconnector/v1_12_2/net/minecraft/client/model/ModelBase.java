package legacyconnector.v1_12_2.net.minecraft.client.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;

/** 1.12.2 entity model: its parts ({@link ModelRenderer}) draw into the render context with the bound texture. */
public abstract class ModelBase {
    public float swingProgress;
    public boolean isRiding;
    public boolean isChild = true;
    public List<ModelRenderer> boxList = Lists.newArrayList();
    private final Map<String, TextureOffset> modelTextureMap = Maps.newHashMap();
    public int textureWidth = 64;
    public int textureHeight = 32;

    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
    }

    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
    }

    public ModelRenderer getRandomModelBox(Random rand) {
        return boxList.get(rand.nextInt(boxList.size()));
    }

    protected void setTextureOffset(String partName, int x, int y) {
        modelTextureMap.put(partName, new TextureOffset(x, y));
    }

    public TextureOffset getTextureOffset(String partName) {
        return modelTextureMap.get(partName);
    }

    public static void copyModelAngles(ModelRenderer source, ModelRenderer dest) {
        dest.rotateAngleX = source.rotateAngleX;
        dest.rotateAngleY = source.rotateAngleY;
        dest.rotateAngleZ = source.rotateAngleZ;
        dest.rotationPointX = source.rotationPointX;
        dest.rotationPointY = source.rotationPointY;
        dest.rotationPointZ = source.rotationPointZ;
    }

    public void setModelAttributes(ModelBase model) {
        swingProgress = model.swingProgress;
        isRiding = model.isRiding;
        isChild = model.isChild;
    }
}
