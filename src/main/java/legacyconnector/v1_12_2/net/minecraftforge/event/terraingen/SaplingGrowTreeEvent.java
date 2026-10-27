package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2: a sapling is about to grow into a tree; DENY stops it (terrain bus). */
@HasResult
public class SaplingGrowTreeEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent {
    private final BlockPos pos;
    private final Random rand;

    public SaplingGrowTreeEvent(World world, Random rand, BlockPos pos) {
        super(world);
        this.rand = rand;
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Random getRand() {
        return rand;
    }
}
