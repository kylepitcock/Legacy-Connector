package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

/** Forge 1.20.1's {@code Event.Result}; NeoForge 1.21.1 dropped results from the event bus (a few events carry their own tri-state). */
public enum Event$Result {
    DENY, DEFAULT, ALLOW
}
