package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.io.IOException;
import java.util.Map;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Anchor;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Edge;

/** Saved inventory-relative intent for the two independently movable modern controls. */
public final class InventoryControlPositionsB173 {
    private InventoryControlPositionsB173() {}

    public static Anchor get(String control) {
        Anchor fallback = defaults(control);
        return parse(LegacyConfig.currentValue(key(control), encode(fallback)), fallback);
    }

    public static void save(String control, Anchor anchor) {
        String key = key(control);
        if (!InventoryControlDockB173.valid(anchor)) throw new IllegalArgumentException("Invalid inventory control anchor");
        try { LegacyConfig.save(Map.of(key, encode(anchor))); }
        catch (IOException error) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Could not save inventory control position", error);
        }
    }

    static Anchor defaults(String control) {
        return switch (control) {
            case "offhand" -> new Anchor(Edge.LEFT, .05, 0, 0);
            case "recipeBook" -> new Anchor(Edge.LEFT, .25, 0, 0);
            default -> throw new IllegalArgumentException("Unknown inventory control: " + control);
        };
    }
    private static String key(String control) { defaults(control);return "b173.inventoryControls." + control; }
    static Anchor parse(String raw, Anchor fallback) {
        if (raw == null || raw.length() > 128) return fallback;
        String[] parts = raw.split(",", -1);
        if (parts.length != 4) return fallback;
        try {
            var anchor = new Anchor(Edge.valueOf(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
            return InventoryControlDockB173.valid(anchor) ? anchor : fallback;
        } catch (IllegalArgumentException malformed) { return fallback; }
    }
    static String encode(Anchor anchor) {
        if (!InventoryControlDockB173.valid(anchor)) throw new IllegalArgumentException("Invalid inventory control anchor");
        return anchor.edge().name() + "," + Double.toString(anchor.along()) + "," + anchor.freeX() + "," + anchor.freeY();
    }
}
