package legacyconnector.v1_12_2.net.minecraft.client.particle;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 base of full-bright particles that age through a row of frames of the sheet (firework sparks, end rods, totems): fading out over
 * the second half of their life, optionally towards a fade colour, with air friction and a vertical acceleration.
 */
public class ParticleSimpleAnimated extends Particle {
    private final int textureIdx;
    private final int numAgingFrames;
    private final float yAccel;
    private float baseAirFriction = 0.91F;
    private float fadeTargetRed;
    private float fadeTargetGreen;
    private float fadeTargetBlue;
    private boolean fadingColor;

    public ParticleSimpleAnimated(World worldIn, double x, double y, double z, int textureIdxIn, int numFrames, float yAccelIn) {
        super(worldIn, x, y, z);
        textureIdx = textureIdxIn;
        numAgingFrames = numFrames;
        yAccel = yAccelIn;
    }

    public void setColor(int rgb) {
        setRBGColorF((float) ((rgb & 16711680) >> 16) / 255.0F, (float) ((rgb & 65280) >> 8) / 255.0F, (float) (rgb & 255) / 255.0F);
    }

    public void setColorFade(int rgb) {
        fadeTargetRed = (float) ((rgb & 16711680) >> 16) / 255.0F;
        fadeTargetGreen = (float) ((rgb & 65280) >> 8) / 255.0F;
        fadeTargetBlue = (float) (rgb & 255) / 255.0F;
        fadingColor = true;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) setExpired();
        if (particleAge > particleMaxAge / 2) {
            setAlphaF(1.0F - ((float) particleAge - (float) (particleMaxAge / 2)) / (float) particleMaxAge);
            if (fadingColor) {
                particleRed += (fadeTargetRed - particleRed) * 0.2F;
                particleGreen += (fadeTargetGreen - particleGreen) * 0.2F;
                particleBlue += (fadeTargetBlue - particleBlue) * 0.2F;
            }
        }
        setParticleTextureIndex(textureIdx + (numAgingFrames - 1 - particleAge * numAgingFrames / Math.max(1, particleMaxAge)));
        motionY += (double) yAccel;
        move(motionX, motionY, motionZ);
        motionX *= (double) baseAirFriction;
        motionY *= (double) baseAirFriction;
        motionZ *= (double) baseAirFriction;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
        }
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 15728880;
    }

    protected void setBaseAirFriction(float friction) {
        baseAirFriction = friction;
    }

    /** The fade colour state, copied to a firework spark's trail. */
    void mcc$copyFade(ParticleSimpleAnimated to) {
        to.fadeTargetRed = fadeTargetRed;
        to.fadeTargetGreen = fadeTargetGreen;
        to.fadeTargetBlue = fadeTargetBlue;
        to.fadingColor = fadingColor;
    }
}
