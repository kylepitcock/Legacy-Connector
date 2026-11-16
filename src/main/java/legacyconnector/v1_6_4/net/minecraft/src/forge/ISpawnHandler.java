package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface ISpawnHandler {
    void writeSpawnData(java.io.DataOutputStream data) throws java.io.IOException;

    void readSpawnData(java.io.DataInputStream data) throws java.io.IOException;
}
