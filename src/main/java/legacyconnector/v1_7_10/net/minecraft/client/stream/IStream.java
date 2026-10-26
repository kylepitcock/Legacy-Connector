package legacyconnector.v1_7_10.net.minecraft.client.stream;

/**
 * 1.7.10's Twitch broadcasting ({@code Minecraft.getTwitchStream()}). Broadcasting no longer exists, so the game's stream
 * is a {@link NullStream}: never broadcasting, never ready. Members that named Twitch SDK types are left out.
 */
public interface IStream {
    default void shutdownStream() {
    }

    default void func_152935_j() {
    }

    default void func_152922_k() {
    }

    /** Whether a broadcast is running. */
    default boolean func_152936_l() {
        return false;
    }

    default boolean func_152924_m() {
        return false;
    }

    default boolean func_152934_n() {
        return false;
    }

    default boolean isPaused() {
        return false;
    }

    default void func_152931_p() {
    }

    default void func_152916_q() {
    }

    default void func_152933_r() {
    }

    default void func_152915_s() {
    }

    default void func_152930_t() {
    }

    default void func_152914_u() {
    }

    default void func_152909_x() {
    }

    default boolean func_152908_z() {
        return false;
    }

    default int func_152920_A() {
        return 0;
    }

    default boolean func_152927_B() {
        return false;
    }

    default String func_152921_C() {
        return null;
    }

    default void func_152917_b(String message) {
    }

    /** Whether broadcasting could be started (signed in, SDK loaded). */
    default boolean func_152928_D() {
        return false;
    }

    default boolean func_152913_F() {
        return false;
    }

    default void func_152910_a(boolean muted) {
    }

    default boolean func_152929_G() {
        return false;
    }
}
