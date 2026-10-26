package cpw.mods.fml.common.asm.transformers;

/**
 * FML's runtime deobfuscation (notch to SRG names) of Minecraft classes. Legacy mod classes are already remapped by the
 * loader and Minecraft is not loaded, so classes pass through unchanged and names map to themselves.
 */
public class DeobfuscationTransformer implements net.minecraft.launchwrapper.IClassTransformer, net.minecraft.launchwrapper.IClassNameTransformer {
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
