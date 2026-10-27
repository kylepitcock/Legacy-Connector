package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import java.io.File;
import java.util.Set;

/**
 * Posted by FML 1.12 when a mod jar's signature did not match its {@code certificateFingerprint}. It is never posted here: every
 * legacy jar is rewritten at load time, so its original signature cannot be checked, and mods react to the event by warning or
 * refusing to run. Mods' handlers link and stay unused.
 */
public class FMLFingerprintViolationEvent extends FMLEvent {
    private final boolean isDirectory;
    private final Set<String> fingerprints;
    private final File source;
    private final String expectedFingerprint;

    public FMLFingerprintViolationEvent(boolean isDirectory, File source, com.google.common.collect.ImmutableSet<String> fingerprints, String expectedFingerprint) {
        this.isDirectory = isDirectory;
        this.source = source;
        this.fingerprints = fingerprints;
        this.expectedFingerprint = expectedFingerprint;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Set<String> getFingerprints() {
        return fingerprints;
    }

    public File getSource() {
        return source;
    }

    public String getExpectedFingerprint() {
        return expectedFingerprint;
    }
}
