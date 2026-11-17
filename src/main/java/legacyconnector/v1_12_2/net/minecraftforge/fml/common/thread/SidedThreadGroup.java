package legacyconnector.v1_12_2.net.minecraftforge.fml.common.thread;

import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** A thread group that names the logical side its threads work for. */
public class SidedThreadGroup extends ThreadGroup {
    private final Side side;

    public SidedThreadGroup(Side side) {
        super(side == Side.CLIENT ? "Client sided threads" : "Server sided threads");
        this.side = side;
    }

    public Side getSide() {
        return side;
    }
}
