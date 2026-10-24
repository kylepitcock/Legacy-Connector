package legacyconnector.v1_7_10.codechicken.nei.recipe;

import legacyconnector.v1_7_10.codechicken.nei.api.IOverlayHandler;

/** NEI's recipe-to-crafting-grid transfer; JEI does its own transfers. */
public class DefaultOverlayHandler implements IOverlayHandler {
    public final int offsetx;
    public final int offsety;

    public DefaultOverlayHandler() {
        this(5, 11);
    }

    public DefaultOverlayHandler(int x, int y) {
        this.offsetx = x;
        this.offsety = y;
    }
}
