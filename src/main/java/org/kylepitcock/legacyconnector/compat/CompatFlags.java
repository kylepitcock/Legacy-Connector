package org.kylepitcock.legacyconnector.compat;

import org.kylepitcock.legacyconnector.core.flags.Switch;

/**
 * Switches that name a specific mod. They live here, in a {@code compat} package, so the core registry
 * ({@link org.kylepitcock.legacyconnector.core.flags.Flags}) stays free of mod names; the documentation page lists them with the others.
 */
public enum CompatFlags implements Switch {
    /** Logs why the Palladium flight key handling does or does not act. */
    PALLADIUM_FLIGHT_DEBUG("legacyconnector.debug.palladiumFlight", Type.FLAG, "false", Category.DEBUG,
            "1.20.1 Palladium compat: logs the flight key handling."),
    /** Logs the renderer registrations the Palladium compat sees. */
    PALLADIUM_RENDERER_REGISTRY_DEBUG("legacyconnector.debug.palladiumRendererRegistry", Type.FLAG, "false", Category.DEBUG,
            "1.20.1 Palladium compat: logs the renderer registry calls.");

    private final String property;
    private final Type type;
    private final String defaultText;
    private final Category category;
    private final String description;

    CompatFlags(String property, Type type, String defaultText, Category category, String description) {
        this.property = property;
        this.type = type;
        this.defaultText = defaultText;
        this.category = category;
        this.description = description;
    }

    @Override public String property() { return property; }
    @Override public Type type() { return type; }
    @Override public String defaultText() { return defaultText; }
    @Override public Category category() { return category; }
    @Override public String description() { return description; }
}
