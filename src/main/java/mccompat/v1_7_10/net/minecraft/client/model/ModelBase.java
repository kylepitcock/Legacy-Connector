package mccompat.v1_7_10.net.minecraft.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;

public abstract class ModelBase {
    public float onGround;
    public boolean isRiding;
    public List boxList = new ArrayList();
    public boolean isChild = true;
    private final Map<String, int[]> modelTextureMap = new HashMap<>();
    public int textureWidth = 64;
    public int textureHeight = 32;

    public void render(Entity entity, float a, float b, float c, float d, float e, float scale) {
    }

    public void setRotationAngles(float a, float b, float c, float d, float e, float scale, Entity entity) {
    }

    public void setLivingAnimations(EntityLivingBase entity, float a, float b, float partial) {
    }

    public ModelRenderer getRandomModelBox(Random random) {
        return (ModelRenderer) boxList.get(random.nextInt(boxList.size()));
    }

    protected void setTextureOffset(String name, int u, int v) {
        modelTextureMap.put(name, new int[] {u, v});
    }

    public int[] mcc$textureOffset(String name) {
        return modelTextureMap.get(name);
    }
}
