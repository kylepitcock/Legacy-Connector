package legacyconnector.v1_7_10.net.minecraft.client.particle;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.slf4j.LoggerFactory;

/** Legacy particles: four layers ticked every client tick and drawn after the world (see LegacyParticles). */
public class EffectRenderer {
    /** The 1.7.10 particle sheet does not exist any more; layer-0 particles draw a vanilla particle sprite. */
    private static final ResourceLocation SHEET = new ResourceLocation("textures/particle/generic_2.png");
    private static final ResourceLocation BLOCKS = new ResourceLocation("textures/atlas/blocks.png");
    private static final int MAX_PER_LAYER = 4000;

    @SuppressWarnings("unchecked")
    public List<EntityFX>[] fxLayers = new List[4];
    protected World worldObj;
    public TextureManager renderer;

    public EffectRenderer(World world, TextureManager textures) {
        this.worldObj = world;
        this.renderer = textures;
        for (int i = 0; i < fxLayers.length; i++) fxLayers[i] = new ArrayList<>();
    }

    public void addEffect(EntityFX fx) {
        if (fx == null) return;
        int layer = Math.max(0, Math.min(3, fx.getFXLayer()));
        List<EntityFX> list = fxLayers[layer];
        if (list.size() >= MAX_PER_LAYER) list.remove(0);
        list.add(fx);
    }

    public void updateEffects() {
        for (List<EntityFX> list : fxLayers) {
            for (int i = 0; i < list.size(); i++) {
                EntityFX fx = list.get(i);
                try {
                    fx.onUpdate();
                } catch (RuntimeException | LinkageError e) {
                    LoggerFactory.getLogger("LegacyConnector/Particles").debug("Legacy particle {} failed: {}", fx.getClass().getName(), e.toString());
                    fx.setDead();
                }
                if (!fx.isEntityAlive()) list.remove(i--);
            }
        }
    }

    /** Draws all particles; the camera basis arguments follow the legacy ActiveRenderInfo convention. */
    public void mcc$render(float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        Tessellator t = Tessellator.instance;
        for (int layer = 0; layer < fxLayers.length; layer++) {
            List<EntityFX> list = fxLayers[layer];
            if (list.isEmpty()) continue;
            if (layer == 0) RenderContext.bindTexture(org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(SHEET.getResourceDomain(), SHEET.getResourcePath()));
            if (layer == 1 || layer == 2) RenderContext.bindTexture(org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations.modern(BLOCKS.getResourceDomain(), BLOCKS.getResourcePath()));
            for (EntityFX fx : new ArrayList<>(list)) {
                try {
                    t.startDrawingQuads();
                    t.setBrightness(fx.getBrightnessForRender(partialTicks));
                    fx.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
                    t.draw();
                } catch (RuntimeException | LinkageError e) {
                    if (t.isDrawing) t.draw();
                    fx.setDead();
                }
            }
        }
    }

    public void renderParticles(Entity viewer, float partialTicks) {
    }

    public void renderLitParticles(Entity viewer, float partialTicks) {
    }

    public void clearEffects(World world) {
        this.worldObj = world;
        for (List<EntityFX> list : fxLayers) list.clear();
    }

    public void addBlockDestroyEffects(int x, int y, int z, Block block, int meta) {
    }

    public void addBlockHitEffects(int x, int y, int z, int side) {
    }

    public String getStatistics() {
        int n = 0;
        for (List<EntityFX> list : fxLayers) n += list.size();
        return String.valueOf(n);
    }
}
