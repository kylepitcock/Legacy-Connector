package legacyconnector.v1_7_10.net.minecraft.util;

/** Something that saves itself as JSON (1.7.10 {@code IJsonSerializable}): the player statistics file stores its values this way. */
public interface IJsonSerializable {
    /** Reads the state back from the JSON the object wrote. */
    void func_152753_a(com.google.gson.JsonElement json);

    /** The JSON that holds the state. */
    com.google.gson.JsonElement func_151003_a();
}
