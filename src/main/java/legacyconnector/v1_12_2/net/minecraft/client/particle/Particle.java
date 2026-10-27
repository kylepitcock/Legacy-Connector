package legacyconnector.v1_12_2.net.minecraft.client.particle;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 client particle, ticked and drawn by {@link ParticleManager} ({@code Minecraft.effectRenderer}). Movement, ageing and colours are
 * 1.12's; block collision uses the modern client level. Particles that keep 1.12's default drawing (a camera-facing quad of their cell of the
 * particle sheet) are drawn by the layer; ones with their own {@code renderParticle} need the BufferBuilder of the rendering shim
 * ({@link ParticleManager#mcc$customRenderer}).
 */
public class Particle {
    private static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    protected World world;
    protected double prevPosX;
    protected double prevPosY;
    protected double prevPosZ;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected double motionX;
    protected double motionY;
    protected double motionZ;
    private AxisAlignedBB boundingBox = EMPTY_AABB;
    protected boolean onGround;
    protected boolean canCollide = true;
    protected boolean isExpired;
    protected float width = 0.6F;
    protected float height = 1.8F;
    protected Random rand = new Random();
    protected int particleTextureIndexX;
    protected int particleTextureIndexY;
    protected float particleTextureJitterX;
    protected float particleTextureJitterY;
    protected int particleAge;
    protected int particleMaxAge;
    protected float particleScale;
    protected float particleGravity;
    protected float particleRed;
    protected float particleGreen;
    protected float particleBlue;
    protected float particleAlpha = 1.0F;
    protected float particleAngle;
    protected float prevParticleAngle;
    /** The block-atlas sprite a layer-1 particle shows (setParticleTexture). */
    protected legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite particleTexture;
    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;
    public static Vec3d cameraViewDir = new Vec3d(0.0D, 0.0D, 0.0D);

    protected Particle(World worldIn, double posXIn, double posYIn, double posZIn) {
        this.world = worldIn;
        setSize(0.2F, 0.2F);
        setPosition(posXIn, posYIn, posZIn);
        this.prevPosX = posXIn;
        this.prevPosY = posYIn;
        this.prevPosZ = posZIn;
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
        this.particleTextureJitterX = rand.nextFloat() * 3.0F;
        this.particleTextureJitterY = rand.nextFloat() * 3.0F;
        this.particleScale = (rand.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.particleMaxAge = (int) (4.0F / (rand.nextFloat() * 0.9F + 0.1F));
        this.particleAge = 0;
    }

    public Particle(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        this(worldIn, xCoordIn, yCoordIn, zCoordIn);
        this.motionX = xSpeedIn + (Math.random() * 2.0D - 1.0D) * 0.4000000059604645D;
        this.motionY = ySpeedIn + (Math.random() * 2.0D - 1.0D) * 0.4000000059604645D;
        this.motionZ = zSpeedIn + (Math.random() * 2.0D - 1.0D) * 0.4000000059604645D;
        float f = (float) (Math.random() + Math.random() + 1.0D) * 0.15F;
        float f1 = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (f1 == 0.0F) f1 = 1.0F;
        this.motionX = motionX / (double) f1 * (double) f * 0.4000000059604645D;
        this.motionY = motionY / (double) f1 * (double) f * 0.4000000059604645D + 0.10000000149011612D;
        this.motionZ = motionZ / (double) f1 * (double) f * 0.4000000059604645D;
    }

    public Particle multiplyVelocity(float multiplier) {
        this.motionX *= multiplier;
        this.motionY = (motionY - 0.10000000149011612D) * (double) multiplier + 0.10000000149011612D;
        this.motionZ *= multiplier;
        return this;
    }

    public Particle multipleParticleScaleBy(float scale) {
        setSize(0.2F * scale, 0.2F * scale);
        this.particleScale *= scale;
        return this;
    }

    public void setRBGColorF(float particleRedIn, float particleGreenIn, float particleBlueIn) {
        this.particleRed = particleRedIn;
        this.particleGreen = particleGreenIn;
        this.particleBlue = particleBlueIn;
    }

    public void setAlphaF(float alpha) {
        this.particleAlpha = alpha;
    }

    public boolean shouldDisableDepth() {
        return false;
    }

    public float getRedColorF() {
        return particleRed;
    }

    public float getGreenColorF() {
        return particleGreen;
    }

    public float getBlueColorF() {
        return particleBlue;
    }

    public void setMaxAge(int particleLifeTime) {
        this.particleMaxAge = particleLifeTime;
    }

    public void onUpdate() {
        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        this.motionY -= 0.04D * (double) particleGravity;
        move(motionX, motionY, motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;
        if (onGround) {
            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }
    }

    /** 0: the particle sheet, 1: the block atlas (a sprite), 3: the particle draws with its own texture. */
    public int getFXLayer() {
        return 0;
    }

    public void setParticleTextureIndex(int particleTextureIndex) {
        this.particleTextureIndexX = particleTextureIndex % 16;
        this.particleTextureIndexY = particleTextureIndex / 16;
    }

    public void nextTextureIndexX() {
        ++particleTextureIndexX;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", Pos (" + posX + "," + posY + "," + posZ + "), RGBA (" + particleRed + "," + particleGreen + "," + particleBlue
                + "," + particleAlpha + "), Age " + particleAge;
    }

    public void setExpired() {
        this.isExpired = true;
    }

    protected void setSize(float particleWidth, float particleHeight) {
        if (particleWidth != width || particleHeight != height) {
            this.width = particleWidth;
            this.height = particleHeight;
            AxisAlignedBB bb = getBoundingBox();
            setBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.minX + (double) width, bb.minY + (double) height, bb.minZ + (double) width));
        }
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        float f = width / 2.0F;
        float f1 = height;
        setBoundingBox(new AxisAlignedBB(x - (double) f, y, z - (double) f, x + (double) f, y + (double) f1, z + (double) f));
    }

    /** Moves by the given amount, stopped by the blocks of the client world unless {@link #canCollide} is off. */
    public void move(double x, double y, double z) {
        double origY = y;
        double origX = x;
        double origZ = z;
        var level = world == null ? null : world.mcc$level();
        if (canCollide && level != null) {
            AxisAlignedBB bb = getBoundingBox();
            var box = new net.minecraft.world.phys.AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            try {
                var moved = net.minecraft.world.entity.Entity.collideBoundingBox(null, new net.minecraft.world.phys.Vec3(x, y, z), box, level, java.util.List.of());
                x = moved.x;
                y = moved.y;
                z = moved.z;
            } catch (RuntimeException e) {
                // a chunk that is not loaded: move freely
            }
        }
        if (x != 0.0D || y != 0.0D || z != 0.0D) {
            setBoundingBox(getBoundingBox().offset(x, y, z));
            resetPositionToBB();
        }
        this.onGround = origY != y && origY < 0.0D;
        if (origX != x) this.motionX = 0.0D;
        if (origZ != z) this.motionZ = 0.0D;
    }

    protected void resetPositionToBB() {
        AxisAlignedBB bb = getBoundingBox();
        this.posX = (bb.minX + bb.maxX) / 2.0D;
        this.posY = bb.minY;
        this.posZ = (bb.minZ + bb.maxZ) / 2.0D;
    }

    /** The packed block and sky light at the particle. */
    public int getBrightnessForRender(float partialTick) {
        BlockPos pos = new BlockPos(posX, posY, posZ);
        var level = world == null ? null : world.mcc$level();
        if (level == null) return 0xF000F0;
        var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        return level.hasChunkAt(p) ? net.minecraft.client.renderer.LevelRenderer.getLightColor(level, p) : 0;
    }

    public boolean isAlive() {
        return !isExpired;
    }

    public AxisAlignedBB getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(AxisAlignedBB bb) {
        this.boundingBox = bb;
    }

    // ------------------------------------------------------------------ the layer's default drawing

    /** The default 1.12 quad, for {@link ParticleManager}: x, y, z, u, v of four corners around the interpolated position. */
    public double[][] mcc$defaultQuad(float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float f = (float) particleTextureIndexX / 16.0F;
        float f1 = f + 0.0624375F;
        float f2 = (float) particleTextureIndexY / 16.0F;
        float f3 = f2 + 0.0624375F;
        float f4 = 0.1F * particleScale;
        float x = (float) (prevPosX + (posX - prevPosX) * (double) partialTicks - interpPosX);
        float y = (float) (prevPosY + (posY - prevPosY) * (double) partialTicks - interpPosY);
        float z = (float) (prevPosZ + (posZ - prevPosZ) * (double) partialTicks - interpPosZ);
        double[][] v = {
            {-rotationX * f4 - rotationXY * f4, -rotationZ * f4, -rotationYZ * f4 - rotationXZ * f4, f1, f3},
            {-rotationX * f4 + rotationXY * f4, rotationZ * f4, -rotationYZ * f4 + rotationXZ * f4, f1, f2},
            {rotationX * f4 + rotationXY * f4, rotationZ * f4, rotationYZ * f4 + rotationXZ * f4, f, f2},
            {rotationX * f4 - rotationXY * f4, -rotationZ * f4, rotationYZ * f4 - rotationXZ * f4, f, f3},
        };
        if (particleAngle != 0.0F) {
            // 1.12 rotated the quad around the view direction by the interpolated angle
            float angle = particleAngle + (particleAngle - prevParticleAngle) * partialTicks;
            double cos = Math.cos(angle * 0.5F), sin = Math.sin(angle * 0.5F);
            double qx = sin * cameraViewDir.x, qy = sin * cameraViewDir.y, qz = sin * cameraViewDir.z, qw = cos;
            for (double[] p : v) {
                double px = p[0], py = p[1], pz = p[2];
                double dot = qx * px + qy * py + qz * pz;
                double qq = qx * qx + qy * qy + qz * qz;
                double cx = qy * pz - qz * py, cy = qz * px - qx * pz, cz = qx * py - qy * px;
                p[0] = 2.0D * dot * qx + (qw * qw - qq) * px + 2.0D * qw * cx;
                p[1] = 2.0D * dot * qy + (qw * qw - qq) * py + 2.0D * qw * cy;
                p[2] = 2.0D * dot * qz + (qw * qw - qq) * pz + 2.0D * qw * cz;
            }
        }
        for (double[] p : v) {
            p[0] += x;
            p[1] += y;
            p[2] += z;
        }
        return v;
    }

    public int mcc$argb() {
        return ((int) (particleAlpha * 255) & 255) << 24 | ((int) (particleRed * 255) & 255) << 16 | ((int) (particleGreen * 255) & 255) << 8
                | ((int) (particleBlue * 255) & 255);
    }

    // ------------------------------------------------------------------ 1.12 drawing (area R1)

    /** A block-atlas sprite for a layer-1 particle (falling dust, mods' sprite particles). */
    public void setParticleTexture(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite texture) {
        this.particleTexture = texture;
    }

    /**
     * 1.12's default drawing: a camera-facing quad of the particle's cell of the sheet (or its sprite) with its colour and light, added
     * to the buffer the particle manager began. Subclasses that override it add their own geometry.
     */
    public void renderParticle(legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder buffer, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn,
            float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        double[][] q = mcc$defaultQuad(partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        if (particleTexture != null) {
            float u0 = particleTexture.getMinU(), u1 = particleTexture.getMaxU(), v0 = particleTexture.getMinV(), v1 = particleTexture.getMaxV();
            q[0][3] = u1;
            q[0][4] = v1;
            q[1][3] = u1;
            q[1][4] = v0;
            q[2][3] = u0;
            q[2][4] = v0;
            q[3][3] = u0;
            q[3][4] = v1;
        }
        int light = getBrightnessForRender(partialTicks);
        int sky = light >> 16 & 65535, block = light & 65535;
        for (double[] v : q) {
            buffer.pos(v[0], v[1], v[2]).tex(v[3], v[4]).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(sky, block).endVertex();
        }
    }
}
