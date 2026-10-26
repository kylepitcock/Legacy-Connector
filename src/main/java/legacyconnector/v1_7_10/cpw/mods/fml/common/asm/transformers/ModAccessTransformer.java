package legacyconnector.v1_7_10.cpw.mods.fml.common.asm.transformers;

import java.io.IOException;
import java.util.jar.JarFile;

/** Applies the access transformer rules mods ship in their jars ({@code FMLAT} manifest entry); see {@link AccessTransformer}. */
public class ModAccessTransformer extends AccessTransformer {
    private static final java.util.Map<String, String> embedded = new java.util.LinkedHashMap<>();

    public ModAccessTransformer() throws Exception {
        super(ModAccessTransformer.class.getName());
    }

    /** Registers the rules files named by a jar's {@code FMLAT} manifest attribute. */
    public static void addJar(JarFile jar) throws IOException {
        var manifest = jar.getManifest();
        String ats = manifest == null ? null : manifest.getMainAttributes().getValue("FMLAT");
        if (ats == null) return;
        for (String at : ats.split(" ")) {
            if (!at.isBlank()) embedded.put(jar.getName() + "!META-INF/" + at.trim(), at.trim());
        }
    }
}
