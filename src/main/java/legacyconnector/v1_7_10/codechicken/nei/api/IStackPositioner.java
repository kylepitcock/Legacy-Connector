package legacyconnector.v1_7_10.codechicken.nei.api;

import java.util.ArrayList;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;

/** Moves the ingredient stacks of a recipe onto the slots of a GUI (NEI's overlay positioning). */
public interface IStackPositioner {
    ArrayList<PositionedStack> positionStacks(ArrayList<PositionedStack> stacks);
}
