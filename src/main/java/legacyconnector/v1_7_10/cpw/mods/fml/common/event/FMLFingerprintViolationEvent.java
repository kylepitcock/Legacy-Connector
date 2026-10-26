package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import java.io.File;
import java.util.Set;

/**
 * A mod's jar is not signed with the certificate its {@code @Mod(certificateFingerprint)} names. Translated jars are
 * no longer signed by their authors, so the layer does not check signatures and never fires this event.
 */
public class FMLFingerprintViolationEvent extends FMLEvent {
    public final boolean isDirectory;
    public final Set<String> fingerprints;
    public final File source;
    public final String expectedFingerprint;

    public FMLFingerprintViolationEvent(boolean isDirectory, File source, com.google.common.collect.ImmutableSet<String> fingerprints, String expectedFingerprint) {
        this.isDirectory = isDirectory;
        this.source = source;
        this.fingerprints = fingerprints;
        this.expectedFingerprint = expectedFingerprint;
    }
}
