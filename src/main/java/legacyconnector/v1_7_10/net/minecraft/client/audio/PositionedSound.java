package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** Base of the legacy sounds mods define: where, how loud and whether it repeats. */
public abstract class PositionedSound implements ISound {
    protected final ResourceLocation positionedSoundLocation;
    protected float volume = 1.0F;
    protected float field_147663_c = 1.0F;
    protected float xPosF;
    protected float yPosF;
    protected float zPosF;
    protected boolean repeat;
    protected int field_147665_h;
    protected AttenuationType field_147666_i = AttenuationType.LINEAR;

    protected PositionedSound(ResourceLocation location) {
        this.positionedSoundLocation = location;
    }

    @Override
    public ResourceLocation getPositionedSoundLocation() {
        return positionedSoundLocation;
    }

    @Override
    public boolean canRepeat() {
        return repeat;
    }

    @Override
    public int getRepeatDelay() {
        return field_147665_h;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return field_147663_c;
    }

    @Override
    public float getXPosF() {
        return xPosF;
    }

    @Override
    public float getYPosF() {
        return yPosF;
    }

    @Override
    public float getZPosF() {
        return zPosF;
    }

    @Override
    public AttenuationType getAttenuationType() {
        return field_147666_i;
    }
}
