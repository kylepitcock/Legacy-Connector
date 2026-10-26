package legacyconnector.v1_7_10.net.minecraft.client.stream;

/** The stream of a game that cannot broadcast (1.7.10's fallback when the Twitch SDK failed to load). */
public class NullStream implements IStream {
    public static final NullStream INSTANCE = new NullStream(null);
    private final Throwable field_152938_a;

    public NullStream(Throwable cause) {
        this.field_152938_a = cause;
    }

    public Throwable func_152937_a() {
        return field_152938_a;
    }
}
