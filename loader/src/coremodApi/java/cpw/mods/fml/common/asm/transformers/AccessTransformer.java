package cpw.mods.fml.common.asm.transformers;

import java.io.IOException;

/**
 * FML's access transformer: widened Minecraft members named in a rules file. Minecraft is not loaded here and the shim
 * is public and non-final throughout (as if every mod's rules were applied), so the rules are read and kept but no
 * class needs changing. Coremods subclass it with their own rules file (CodeChickenCore, InvTweaks, Waila).
 */
public class AccessTransformer implements net.minecraft.launchwrapper.IClassTransformer {
    /** The rules files this transformer was given, in order. */
    protected final java.util.List<String> mcc$rulesFiles = new java.util.ArrayList<>();

    /**
     * FML kept the parsed rules here, and a rule as an inner class with {@code name} and {@code desc}; CodeChickenCore (1.6.4) reads and rewrites both
     * reflectively ({@code MCPDeobfuscationTransformer}) to turn SRG rule names into MCP names. Nothing is parsed here, so the map stays empty.
     */
    private final com.google.common.collect.Multimap<String, Modifier> modifiers = com.google.common.collect.ArrayListMultimap.create();

    private class Modifier {
        public String name = "";
        public String desc = "";
        public boolean isClass;
        public boolean modifyClassVisibility;
    }

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
