package legacyconnector.v1_12_2.net.minecraft.client.audio;

/** A sound updated every tick ({@code update()} moves it, changes its volume) until it says it is done. */
public interface ITickableSound extends ISound, legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    boolean isDonePlaying();
}
