package example.versionprobe;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.kylepitcock.legacyconnector.api.event.LegacyLoadCompleteEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyModsDiscoveredEvent;
import org.kylepitcock.legacyconnector.api.event.LegacyRegistrationFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The addon is a normal NeoForge mod; this class also listens to the three lifecycle events on the mod bus, to show they arrive. */
@Mod("versionprobe")
public final class VersionProbeMod {
    private static final Logger LOG = LoggerFactory.getLogger("VersionProbe");

    public VersionProbeMod(IEventBus modBus) {
        modBus.addListener(LegacyModsDiscoveredEvent.class, e -> LOG.info("PROBE event LegacyModsDiscoveredEvent mods={}", e.getMods().size()));
        modBus.addListener(LegacyRegistrationFinishedEvent.class, e -> LOG.info("PROBE event LegacyRegistrationFinishedEvent"));
        modBus.addListener(LegacyLoadCompleteEvent.class, e -> LOG.info("PROBE event LegacyLoadCompleteEvent"));
    }
}
