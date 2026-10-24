package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** A positioned sound that moves: subclasses update its position and volume in {@link #update()}. */
public abstract class MovingSound extends PositionedSound implements ITickableSound {
    protected boolean donePlaying;

    protected MovingSound(ResourceLocation location) {
        super(location);
    }

    @Override
    public boolean isDonePlaying() {
        return donePlaying;
    }
}
