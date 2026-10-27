package legacyconnector.v1_12_2.net.minecraft.client.particle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * {@code Minecraft.effectRenderer}: the mods' particles, ticked with the client and drawn after the vanilla ones (see
 * {@code LegacyParticles1122}). Vanilla particles a mod spawns by id become modern particles; block break and hit effects are the modern
 * particle engine's.
 */
public class ParticleManager {
    public static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");
    private static final int MAX_PARTICLES = 16384;

    /**
     * Draws a particle that overrides {@code renderParticle} (its own BufferBuilder geometry), for the rendering shim to set: arguments are
     * the particle, partial ticks and 1.12's five camera rotation values in renderParticle's order. Unset, such particles are not drawn.
     */
    public interface CustomRenderer {
        void render(Particle particle, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ);
    }

    public static volatile CustomRenderer mcc$customRenderer;

    protected World world;
    private final ArrayDeque<Particle> particles = new ArrayDeque<>();
    private final ArrayDeque<Particle> queue = new ArrayDeque<>();
    private final Random rand = new Random();

    public ParticleManager() {
    }

    public void addEffect(Particle effect) {
        if (effect == null) return;
        synchronized (queue) {
            queue.add(effect);
        }
    }

    public void updateEffects() {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                if (particles.size() >= MAX_PARTICLES) particles.removeFirst();
                particles.add(queue.removeFirst());
            }
        }
        var it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            try {
                p.onUpdate();
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Particles").debug("Legacy particle {} failed: {}", p.getClass().getName(), e.toString());
                p.setExpired();
            }
            if (!p.isAlive()) it.remove();
        }
    }

    public void clearEffects(World worldIn) {
        this.world = worldIn;
        particles.clear();
        synchronized (queue) {
            queue.clear();
        }
    }

    public List<Particle> mcc$particles() {
        return new ArrayList<>(particles);
    }

    public String getStatistics() {
        return String.valueOf(particles.size());
    }

    /** A vanilla particle by its 1.12 id ({@code EnumParticleTypes.getParticleID()}); returns null as the modern particle has no legacy object. */
    public Particle spawnEffectParticle(int particleId, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        org.kylepitcock.legacyconnector.v1_12_2.client.LegacyParticles1122.spawnVanilla(particleId, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
        return null;
    }

    public void addBlockDestroyEffects(BlockPos pos, IBlockState state) {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
        if (modern != null) net.minecraft.client.Minecraft.getInstance().particleEngine.destroy(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), modern);
    }

    public void addBlockHitEffects(BlockPos pos, EnumFacing side) {
        if (side != null) net.minecraft.client.Minecraft.getInstance().particleEngine.crack(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), side.mcc$modern());
    }
}
