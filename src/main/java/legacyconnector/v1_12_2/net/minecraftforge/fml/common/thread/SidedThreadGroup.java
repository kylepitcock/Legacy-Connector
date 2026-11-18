package legacyconnector.v1_12_2.net.minecraftforge.fml.common.thread;

import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** A thread group that names the logical side its threads work for. */
public class SidedThreadGroup extends ThreadGroup implements java.util.concurrent.ThreadFactory {
    private final Side side;

    public SidedThreadGroup(Side side) {
        super(side == Side.CLIENT ? "Client sided threads" : "Server sided threads");
        this.side = side;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(this, r, getName() + " thread");
    }

    public Side getSide() {
        return side;
    }
}
