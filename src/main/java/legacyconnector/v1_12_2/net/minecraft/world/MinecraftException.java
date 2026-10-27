package legacyconnector.v1_12_2.net.minecraft.world;

/** Thrown when a world's session lock is lost. */
public class MinecraftException extends Exception {
    public MinecraftException(String message) {
        super(message);
    }
}
