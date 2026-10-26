package legacyconnector.v1_7_10.net.minecraft.world;

/** A save error (a lost session lock, a chunk that could not be written), thrown by save handlers. */
public class MinecraftException extends Exception {
    public MinecraftException(String message) {
        super(message);
    }
}
