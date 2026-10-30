package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelSpider;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerSpiderEyes;
import legacyconnector.v1_12_2.net.minecraft.entity.monster.EntitySpider;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 spider renderer: the spider model with its glowing eyes layer, rolling fully over when it dies. */
public class RenderSpider<T extends EntitySpider> extends RenderLiving<T> {
    private static final ResourceLocation SPIDER_TEXTURES = new ResourceLocation("textures/entity/spider/spider.png");

    public RenderSpider(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelSpider(), 1.0F);
        addLayer(new LayerSpiderEyes<>(this));
    }

    @Override
    protected float getDeathMaxRotation(T entityLivingBaseIn) {
        return 180.0F;
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return SPIDER_TEXTURES;
    }
}
