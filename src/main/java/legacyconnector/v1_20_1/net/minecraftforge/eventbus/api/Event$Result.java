package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

/** Forge's tri-state event result ({@code Event.Result}); NeoForge's event bus dropped it. Events the layer posts itself read it, see {@code Results1201}. */
public enum Event$Result {
    DENY, DEFAULT, ALLOW
}
