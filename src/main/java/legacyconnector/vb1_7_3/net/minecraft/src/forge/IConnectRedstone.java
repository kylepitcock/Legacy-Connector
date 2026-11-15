package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;

/** Forge 1.0: whether redstone dust next to this block connects to it (direction as Beta's dust passed it, -1 for above/below). */
public interface IConnectRedstone {
    boolean canConnectRedstone(BlockView world, int x, int y, int z, int direction);
}
