package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Beta renderer entry point. Drawing state belongs to the surrounding common renderer context. */
public abstract class EntityRenderer {
    protected float shadowRadius;
    protected float shadowDarkness=1F;
    protected EntityModel bipedModel;
    protected legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager renderManager=new legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager();
    public EntityRenderer(){}
    public abstract void render(Entity entity,double x,double y,double z,float yaw,float partial);
    protected void bindTexture(String path){RendererContextB173.bind(path);}
    protected boolean bindDownloadedTexture(String url,String fallback){bindTexture(fallback);return true;}
    public void postRender(Entity entity,double x,double y,double z,float yaw,float partial){}
    public legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer getTextRenderer(){return legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer.mcc$instance();}
    public final float mcc$shadowRadius(){return shadowRadius;}
}
