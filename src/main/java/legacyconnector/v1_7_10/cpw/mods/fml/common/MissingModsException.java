package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.util.Set;
import legacyconnector.v1_7_10.cpw.mods.fml.common.versioning.ArtifactVersion;

/** Mods a mod requires are absent or of the wrong version. */
public class MissingModsException extends EnhancedRuntimeException {
    private static final long serialVersionUID = 1L;
    public final Set<ArtifactVersion> missingMods;

    public MissingModsException(Set<ArtifactVersion> missingMods) {
        super(mcc$describe(missingMods));
        this.missingMods = missingMods;
    }

    private static String mcc$describe(Set<ArtifactVersion> mods) {
        StringBuilder b = new StringBuilder("Missing mods:");
        if (mods != null) for (ArtifactVersion v : mods) b.append(' ').append(v.getLabel()).append(' ').append(v.getRangeString());
        return b.toString();
    }

    @Override
    protected void printStackTrace(WrappedPrintStream stream) {
        stream.println("Missing Mods:");
        if (missingMods != null) for (ArtifactVersion v : missingMods) stream.println(String.format("\t%s : %s", v.getLabel(), v.getRangeString()));
        stream.println("");
    }
}
