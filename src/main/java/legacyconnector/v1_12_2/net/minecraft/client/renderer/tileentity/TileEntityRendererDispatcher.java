package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 tile entity renderer registry. The modern block entity renderer of the hosting block entity asks it for the renderer of a
 * legacy tile entity's class (superclasses included, as vanilla looked them up) and draws through it.
 */
public class TileEntityRendererDispatcher {
    public final Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<? extends TileEntity>> renderers = new HashMap<>();
    public static TileEntityRendererDispatcher instance = new TileEntityRendererDispatcher();
    public static double staticPlayerX;
    public static double staticPlayerY;
    public static double staticPlayerZ;
    public TextureManager renderEngine;
    public World world;
    public Entity entity;
    public float entityYaw;
    public float entityPitch;
    public double entityX;
    public double entityY;
    public double entityZ;

    private TileEntityRendererDispatcher() {
    }

    public synchronized void mcc$register(Class<? extends TileEntity> cls, TileEntitySpecialRenderer<? extends TileEntity> renderer) {
        renderers.put(cls, renderer);
        renderer.setRendererDispatcher(this);
    }

    @SuppressWarnings("unchecked")
    public <T extends TileEntity> TileEntitySpecialRenderer<T> getRenderer(Class<? extends TileEntity> teClass) {
        TileEntitySpecialRenderer<? extends TileEntity> renderer;
        synchronized (this) {
            renderer = renderers.get(teClass);
            if (renderer == null && teClass != TileEntity.class && teClass != null) {
                Class<?> sup = teClass.getSuperclass();
                if (sup != null && TileEntity.class.isAssignableFrom(sup)) {
                    renderer = getRenderer((Class<? extends TileEntity>) sup);
                    if (renderer != null) renderers.put(teClass, renderer);
                }
            }
        }
        return (TileEntitySpecialRenderer<T>) renderer;
    }

    public <T extends TileEntity> TileEntitySpecialRenderer<T> getRenderer(TileEntity tileEntityIn) {
        return tileEntityIn == null || tileEntityIn.isInvalid() ? null : getRenderer(tileEntityIn.getClass());
    }

    public void setWorld(World worldIn) {
        world = worldIn;
        if (worldIn == null) entity = null;
    }

    /** Draws a tile entity at the current matrix (the legacy offsets are relative to it). */
    public void render(TileEntity tileentityIn, float partialTicks, int destroyStage) {
        render(tileentityIn, 0.0D, 0.0D, 0.0D, partialTicks, destroyStage, 1.0F);
    }

    public void render(TileEntity tileEntityIn, double x, double y, double z, float partialTicks) {
        render(tileEntityIn, x, y, z, partialTicks, -1, 1.0F);
    }

    public void render(TileEntity tileEntityIn, double x, double y, double z, float partialTicks, float alpha) {
        render(tileEntityIn, x, y, z, partialTicks, -1, alpha);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(TileEntity tileEntityIn, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        TileEntitySpecialRenderer renderer = getRenderer(tileEntityIn);
        if (renderer == null) return;
        try {
            if (renderer.hasFastRenderer()) {
                var tess = legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator.getInstance();
                var buffer = tess.getBuffer();
                if (renderEngine != null) renderEngine.bindTexture(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
                buffer.begin(7, legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK);
                renderer.renderTileEntityFast(tileEntityIn, x, y, z, partialTicks, destroyStage, alpha, buffer);
                tess.draw();
            } else {
                renderer.render(tileEntityIn, x, y, z, partialTicks, destroyStage, alpha);
            }
        } catch (RuntimeException | LinkageError e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("rendering tile entity " + tileEntityIn.getClass().getName(), e);
        }
    }

    public void renderTileEntity(TileEntity tileentityIn, float partialTicks, int destroyStage) {
        render(tileentityIn, partialTicks, destroyStage);
    }

    /** Forge's fast-renderer batch: each renderer draws its own batch here. */
    public void preDrawBatch() {
    }

    public void drawBatch(int pass) {
    }

    /** The frame's view for tile entity renderers: world, textures, viewer and its interpolated position and angles. */
    public void prepare(World worldIn, TextureManager renderEngineIn, legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer fontRendererIn,
            Entity entityIn, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult cameraHitResultIn, float p_190056_6_) {
        if (world != worldIn) setWorld(worldIn);
        renderEngine = renderEngineIn;
        entity = entityIn;
        if (entityIn == null) return;
        entityYaw = Entity.mcc$get$prevRotationYaw(entityIn) + (Entity.mcc$get$rotationYaw(entityIn) - Entity.mcc$get$prevRotationYaw(entityIn)) * p_190056_6_;
        entityPitch = Entity.mcc$get$prevRotationPitch(entityIn) + (Entity.mcc$get$rotationPitch(entityIn) - Entity.mcc$get$prevRotationPitch(entityIn)) * p_190056_6_;
        entityX = Entity.mcc$get$lastTickPosX(entityIn) + (Entity.mcc$get$posX(entityIn) - Entity.mcc$get$lastTickPosX(entityIn)) * (double) p_190056_6_;
        entityY = Entity.mcc$get$lastTickPosY(entityIn) + (Entity.mcc$get$posY(entityIn) - Entity.mcc$get$lastTickPosY(entityIn)) * (double) p_190056_6_;
        entityZ = Entity.mcc$get$lastTickPosZ(entityIn) + (Entity.mcc$get$posZ(entityIn) - Entity.mcc$get$lastTickPosZ(entityIn)) * (double) p_190056_6_;
    }
}
