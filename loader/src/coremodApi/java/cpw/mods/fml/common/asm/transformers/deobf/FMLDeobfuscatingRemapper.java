package cpw.mods.fml.common.asm.transformers.deobf;

import org.objectweb.asm.commons.Remapper;

/**
 * FML's notch -> SRG remapper. Legacy mod classes already use SRG names and Minecraft's classes are not present while
 * coremod transformers run, so every mapping is the identity.
 */
public class FMLDeobfuscatingRemapper extends Remapper {
    // Read reflectively by CodeChickenLib's ObfMapping; empty because every name here is already SRG.
    private java.util.Map<String, java.util.Map<String, String>> rawFieldMaps = new java.util.HashMap<>();
    private java.util.Map<String, java.util.Map<String, String>> rawMethodMaps = new java.util.HashMap<>();

    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();

    @Override
    public String map(String typeName) {
        return typeName;
    }

    public String unmap(String typeName) {
        return typeName;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return name;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return name;
    }

    public String getStaticFieldType(String oldType, String oldName, String newType, String newName) {
        return null;
    }
}
