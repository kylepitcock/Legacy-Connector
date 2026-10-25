package legacyconnector.v1_12_2.net.minecraft.launchwrapper;

/** Legacy bytecode transformer. Registered transformers are never run: they target 1.7.10 Minecraft bytecode. */
public interface IClassTransformer {
    byte[] transform(String name, String transformedName, byte[] basicClass);
}
