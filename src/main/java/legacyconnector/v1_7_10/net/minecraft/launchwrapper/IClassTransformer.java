package legacyconnector.v1_7_10.net.minecraft.launchwrapper;

/** Legacy bytecode transformer. Registered transformers are never run: they target 1.7.10 Minecraft bytecode. */
public interface IClassTransformer {
    byte[] transform(String name, String transformedName, byte[] basicClass);
}
