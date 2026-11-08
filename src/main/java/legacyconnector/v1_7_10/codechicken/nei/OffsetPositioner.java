package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import legacyconnector.v1_7_10.codechicken.nei.api.IStackPositioner;

/** Shifts every stack by a fixed offset. */
public class OffsetPositioner implements IStackPositioner {
    public final int offsetx;
    public final int offsety;

    public OffsetPositioner(int x, int y) {
        offsetx = x;
        offsety = y;
    }

    public ArrayList<PositionedStack> positionStacks(ArrayList<PositionedStack> stacks) {
        for (PositionedStack stack : stacks) {
            stack.relx += offsetx;
            stack.rely += offsety;
        }
        return stacks;
    }
}
