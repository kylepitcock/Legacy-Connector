package legacyconnector.v1_7_10.cpw.mods.fml.common.asm.transformers;

/**
 * FML's runtime deobfuscation (notch to SRG names) of Minecraft classes. Legacy mod classes are already remapped by the
 * loader and Minecraft is not loaded, so classes pass through unchanged and names map to themselves.
 */
public class DeobfuscationTransformer implements legacyconnector.v1_7_10.net.minecraft.launchwrapper.IClassTransformer {
    public DeobfuscationTransformer() {
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        return bytes;
    }

    public String remapClassName(String name) {
        return name;
    }

    public String unmapClassName(String name) {
        return name;
    }
}
