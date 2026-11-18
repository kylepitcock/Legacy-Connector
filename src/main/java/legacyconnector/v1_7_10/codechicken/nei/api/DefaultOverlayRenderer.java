package legacyconnector.v1_7_10.codechicken.nei.api;

import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;

/** The ghost-ingredient overlay NEI draws over a GUI's slots; JEI draws its own, so this only keeps the stacks. */
public class DefaultOverlayRenderer implements IRecipeOverlayRenderer {
    public final List<PositionedStack> ingreds;
    public final IStackPositioner positioner;

    /** NEI 1.4.7 / 1.5.2 took an ArrayList. */
    public DefaultOverlayRenderer(java.util.ArrayList<PositionedStack> ingreds, IStackPositioner positioner) {
        this((List<PositionedStack>) ingreds, positioner);
    }

    public DefaultOverlayRenderer(List<PositionedStack> ingreds, IStackPositioner positioner) {
        this.ingreds = ingreds;
        this.positioner = positioner;
    }
}
