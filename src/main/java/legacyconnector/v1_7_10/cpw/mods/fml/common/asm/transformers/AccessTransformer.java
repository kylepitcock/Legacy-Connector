package legacyconnector.v1_7_10.cpw.mods.fml.common.asm.transformers;

import java.io.IOException;

/**
 * FML's access transformer: widened Minecraft members named in a rules file. Minecraft is not loaded here and the shim
 * is public and non-final throughout (as if every mod's rules were applied), so the rules are read and kept but no
 * class needs changing. Coremods subclass it with their own rules file (CodeChickenCore, InvTweaks, Waila).
 */
public class AccessTransformer implements legacyconnector.v1_7_10.net.minecraft.launchwrapper.IClassTransformer {
    /** The rules files this transformer was given, in order. */
    protected final java.util.List<String> mcc$rulesFiles = new java.util.ArrayList<>();

    public AccessTransformer() throws IOException {
        this("fml_at.cfg");
    }

    protected AccessTransformer(String rulesFile) throws IOException {
        readMapFile(rulesFile);
    }

    void readMapFile(String rulesFile) throws IOException {
        if (rulesFile != null) mcc$rulesFiles.add(rulesFile);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        return bytes;
    }
}
