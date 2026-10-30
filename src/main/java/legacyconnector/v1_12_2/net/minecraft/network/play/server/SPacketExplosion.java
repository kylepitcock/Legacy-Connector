package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** An explosion on the client: the blocks it destroyed (for the particles) and the push it gives the player; sent as the modern packet. */
public class SPacketExplosion implements Packet<INetHandlerPlayClient> {
    private double posX;
    private double posY;
    private double posZ;
    private float strength;
    private List<BlockPos> affectedBlockPositions;
    private float motionX;
    private float motionY;
    private float motionZ;

    public SPacketExplosion() {
    }

    public SPacketExplosion(double xIn, double yIn, double zIn, float strengthIn, List<BlockPos> affectedBlockPositionsIn, Vec3d motion) {
        this.posX = xIn;
        this.posY = yIn;
        this.posZ = zIn;
        this.strength = strengthIn;
        this.affectedBlockPositions = new ArrayList<>(affectedBlockPositionsIn);
        if (motion != null) {
            this.motionX = (float) motion.x;
            this.motionY = (float) motion.y;
            this.motionZ = (float) motion.z;
        }
    }

    public float getMotionX() {
        return motionX;
    }

    public float getMotionY() {
        return motionY;
    }

    public float getMotionZ() {
        return motionZ;
    }

    public double getX() {
        return posX;
    }

    public double getY() {
        return posY;
    }

    public double getZ() {
        return posZ;
    }

    public float getStrength() {
        return strength;
    }

    public List<BlockPos> getAffectedBlockPositions() {
        return affectedBlockPositions;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        List<net.minecraft.core.BlockPos> blocks = new ArrayList<>();
        if (affectedBlockPositions != null) for (BlockPos p : affectedBlockPositions) blocks.add(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
        boolean large = strength >= 2.0F && !blocks.isEmpty();
        return new net.minecraft.network.protocol.game.ClientboundExplodePacket(posX, posY, posZ, strength, blocks,
                new net.minecraft.world.phys.Vec3(motionX, motionY, motionZ),
                blocks.isEmpty() ? net.minecraft.world.level.Explosion.BlockInteraction.KEEP : net.minecraft.world.level.Explosion.BlockInteraction.DESTROY,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, large ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER
                        : net.minecraft.core.particles.ParticleTypes.EXPLOSION, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);
    }
}
