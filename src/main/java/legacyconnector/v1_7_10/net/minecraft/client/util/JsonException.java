package legacyconnector.v1_7_10.net.minecraft.client.util;

/** 1.7.10 {@code net.minecraft.client.util.JsonException}: a JSON resource (shader, etc.) failed to parse. */
public class JsonException extends java.io.IOException {
    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
