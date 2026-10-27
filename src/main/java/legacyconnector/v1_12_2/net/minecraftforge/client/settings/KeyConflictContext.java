package legacyconnector.v1_12_2.net.minecraftforge.client.settings;

/** Forge's key contexts: always, only with a screen open, only in game. Mapped onto NeoForge's identical contexts. */
public enum KeyConflictContext implements IKeyConflictContext {
    UNIVERSAL(net.neoforged.neoforge.client.settings.KeyConflictContext.UNIVERSAL),
    GUI(net.neoforged.neoforge.client.settings.KeyConflictContext.GUI),
    IN_GAME(net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME);

    private final net.neoforged.neoforge.client.settings.KeyConflictContext modern;

    KeyConflictContext(net.neoforged.neoforge.client.settings.KeyConflictContext modern) {
        this.modern = modern;
    }

    @Override
    public boolean isActive() {
        return modern.isActive();
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        if (this == UNIVERSAL || other == UNIVERSAL || other == null) return true;
        return other == this;
    }

    @Override
    public net.neoforged.neoforge.client.settings.IKeyConflictContext mcc$modern() {
        return modern;
    }

    public static IKeyConflictContext mcc$of(net.neoforged.neoforge.client.settings.IKeyConflictContext modern) {
        if (modern == net.neoforged.neoforge.client.settings.KeyConflictContext.GUI) return GUI;
        if (modern == net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME) return IN_GAME;
        return UNIVERSAL;
    }
}
