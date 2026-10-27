package legacyconnector.v1_12_2.net.minecraft.client.audio;

import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** A sound that follows something: its {@code update()} moves it every tick until it sets {@link #donePlaying}. */
public abstract class MovingSound extends PositionedSound implements ITickableSound {
    protected boolean donePlaying;

    protected MovingSound(SoundEvent soundIn, SoundCategory categoryIn) {
        super(soundIn, categoryIn);
    }

    @Override
    public boolean isDonePlaying() {
        return donePlaying;
    }
}
