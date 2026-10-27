package legacyconnector.v1_12_2.net.minecraft.client.audio;

/** A weighted entry of a sound event: a sound, or another event picking one. */
public interface ISoundEventAccessor<T> {
    int getWeight();

    T cloneEntry();
}
