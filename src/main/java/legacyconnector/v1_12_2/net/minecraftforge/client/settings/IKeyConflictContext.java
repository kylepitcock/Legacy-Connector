package legacyconnector.v1_12_2.net.minecraftforge.client.settings;

/** Forge: when a key binding is active and which other bindings it can conflict with. */
public interface IKeyConflictContext {
    boolean isActive();

    boolean conflicts(IKeyConflictContext other);

    /** The NeoForge context with the same meaning (a mod's own context is asked through an adapter). */
    default net.neoforged.neoforge.client.settings.IKeyConflictContext mcc$modern() {
        IKeyConflictContext self = this;
        return new net.neoforged.neoforge.client.settings.IKeyConflictContext() {
            @Override
            public boolean isActive() {
                return self.isActive();
            }

            @Override
            public boolean conflicts(net.neoforged.neoforge.client.settings.IKeyConflictContext other) {
                return other == this || other == net.neoforged.neoforge.client.settings.KeyConflictContext.UNIVERSAL;
            }
        };
    }
}
