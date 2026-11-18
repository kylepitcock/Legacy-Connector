package legacyconnector.v1_7_10.net.minecraft.client.audio;

/** An entry of a sound event's pool: a weight and something to pick (a sound file, or another event). */
public interface ISoundEventAccessor {
    int getWeight();

    Object func_148720_g();
}
