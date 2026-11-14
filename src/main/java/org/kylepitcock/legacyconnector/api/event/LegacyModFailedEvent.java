package org.kylepitcock.legacyconnector.api.event;

import java.util.List;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.kylepitcock.legacyconnector.api.LegacyMod;

/**
 * A legacy mod failed in a lifecycle phase and was disabled (added in API 0.3). Posted right after the failure, on every mod's event
 * bus, after {@link org.kylepitcock.legacyconnector.api.LegacyAddon#onLegacyModFailed}. Failures of Forge 1.20.1 mods are not reported
 * here (they are plain NeoForge mods that FML itself loads).
 */
public class LegacyModFailedEvent extends Event implements IModBusEvent {
    private final LegacyMod mod;
    private final String phase;
    private final String message;
    private final List<String> missingDependencies;

    public LegacyModFailedEvent(LegacyMod mod, String phase, String message, List<String> missingDependencies) {
        this.mod = mod;
        this.phase = phase;
        this.message = message;
        this.missingDependencies = List.copyOf(missingDependencies);
    }

    /** The mod that failed ({@link LegacyMod#failed()} is true). */
    public LegacyMod getMod() {
        return mod;
    }

    /** The lifecycle phase: {@code construction}, {@code preInit}, {@code init}, {@code imc}, {@code postInit} or {@code loadComplete}. */
    public String getPhase() {
        return phase;
    }

    /** The failure message (starts with the missing dependencies when there are any). */
    public String getMessage() {
        return message;
    }

    /** The required dependencies the loader found not installed, one line each ("X requires Y (range), which is not installed"); often the cause. */
    public List<String> getMissingDependencies() {
        return missingDependencies;
    }
}
