package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelSpider;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Spider renderer (also the base of mod spiders). */
public class RenderSpider extends RenderLiving {
    private static final ResourceLocation SPIDER = new ResourceLocation("textures/entity/spider/spider.png");

    public RenderSpider() {
        super(new ModelSpider(), 1.0F);
        setRenderPassModel(new ModelSpider());
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SPIDER;
    }
}
