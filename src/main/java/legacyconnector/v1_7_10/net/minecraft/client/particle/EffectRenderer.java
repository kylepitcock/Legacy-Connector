package legacyconnector.v1_7_10.net.minecraft.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;
import org.slf4j.LoggerFactory;

/**
 * Legacy particles: four layers ticked every client tick and drawn after the world (see LegacyParticles). Layers 0-2 are
 * drawn in one Tessellator batch per particle with the legacy sheet, block atlas or item atlas bound; layer 3 particles
 * bind their textures and run the Tessellator themselves (1.7.10's renderLitParticles).
 */
public class EffectRenderer {
    /** The 1.7.10 particle sheet, composed from the modern sprites at the old cells (LegacyParticleSheets). */
    public static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");
    private static final ResourceLocation BLOCKS = new ResourceLocation("textures/atlas/blocks.png");
    private static final int MAX_PER_LAYER = 4000;

    @SuppressWarnings("unchecked")
    public List<EntityFX>[] fxLayers = new List[4];
    protected World worldObj;
    public TextureManager renderer;
    public Random rand = new Random();

    public EffectRenderer(World world, TextureManager textures) {
        this.worldObj = world;
        this.renderer = textures;
        for (int i = 0; i < fxLayers.length; i++) fxLayers[i] = new ArrayList<>();
    }

    /** The world particles spawn in: the one given at construction, else the client's current world. */
    private World mcc$world() {
        if (worldObj != null) return worldObj;
        return legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$theWorld(null);
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
                if (fx == null) {
                    list.remove(i--);
                    continue;
                }
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
            if (layer == 0) RenderContext.bindTexture(TextureLocations.modern(particleTextures.getResourceDomain(), particleTextures.getResourcePath()));
            if (layer == 1 || layer == 2) RenderContext.bindTexture(TextureLocations.modern(BLOCKS.getResourceDomain(), BLOCKS.getResourcePath()));
            for (EntityFX fx : new ArrayList<>(list)) {
                if (fx == null) continue;
                try {
                    if (layer == 3) {
                        // lit particles bind their own texture and start/draw the Tessellator themselves
                        t.setBrightness(fx.getBrightnessForRender(partialTicks));
                        fx.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
                        if (t.isDrawing) t.draw();
                        continue;
                    }
                    t.startDrawingQuads();
                    t.setBrightness(fx.getBrightnessForRender(partialTicks));
                    fx.renderParticle(t, partialTicks, rx, rxz, rz, ryz, rxy);
                    if (t.isDrawing) t.draw();
                } catch (RuntimeException | LinkageError e) {
                    if (t.isDrawing) t.mcc$reset();
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

    /** The 64 digging particles of a broken block, unless the block draws its own (Block.addDestroyEffects). */
    public void addBlockDestroyEffects(int x, int y, int z, Block block, int meta) {
        World world = mcc$world();
        if (world == null || block == null || block.isAir(world, x, y, z) || block.addDestroyEffects(world, x, y, z, meta, this)) return;
        int n = 4;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                for (int k = 0; k < n; ++k) {
                    double px = x + (i + 0.5D) / n, py = y + (j + 0.5D) / n, pz = z + (k + 0.5D) / n;
                    addEffect(new EntityDiggingFX(world, px, py, pz, px - x - 0.5D, py - y - 0.5D, pz - z - 0.5D, block, meta)
                            .applyColourMultiplier(x, y, z));
                }
            }
        }
    }

    /** One digging particle on the given face of the block being mined. */
    public void addBlockHitEffects(int x, int y, int z, int side) {
        World world = mcc$world();
        if (world == null) return;
        Block block = world.getBlock(x, y, z);
        if (block == null || block.getMaterial() == Material.air) return;
        float f = 0.1F;
        double px = x + rand.nextDouble() * (block.getBlockBoundsMaxX() - block.getBlockBoundsMinX() - f * 2.0F) + f + block.getBlockBoundsMinX();
        double py = y + rand.nextDouble() * (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() - f * 2.0F) + f + block.getBlockBoundsMinY();
        double pz = z + rand.nextDouble() * (block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ() - f * 2.0F) + f + block.getBlockBoundsMinZ();
        switch (side) {
            case 0 -> py = y + block.getBlockBoundsMinY() - f;
            case 1 -> py = y + block.getBlockBoundsMaxY() + f;
            case 2 -> pz = z + block.getBlockBoundsMinZ() - f;
            case 3 -> pz = z + block.getBlockBoundsMaxZ() + f;
            case 4 -> px = x + block.getBlockBoundsMinX() - f;
            case 5 -> px = x + block.getBlockBoundsMaxX() + f;
            default -> {
            }
        }
        addEffect(new EntityDiggingFX(world, px, py, pz, 0.0D, 0.0D, 0.0D, block, world.getBlockMetadata(x, y, z), side)
                .applyColourMultiplier(x, y, z).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
    }

    /** Forge: lets the block draw its own hit particles (Block.addHitEffects) before the default ones. */
    public void addBlockHitEffects(int x, int y, int z, MovingObjectPosition target) {
        World world = mcc$world();
        if (world == null || target == null) return;
        Block block = world.getBlock(x, y, z);
        if (block != null && !block.addHitEffects(world, target, this)) addBlockHitEffects(x, y, z, target.sideHit);
    }

    public String getStatistics() {
        int n = 0;
        for (List<EntityFX> list : fxLayers) n += list.size();
        return String.valueOf(n);
    }
}
