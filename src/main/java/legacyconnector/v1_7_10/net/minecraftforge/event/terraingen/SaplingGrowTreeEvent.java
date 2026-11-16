package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent;

/** A sapling is about to grow into a tree; DENY stops it (terrain gen bus). */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.HasResult
public class SaplingGrowTreeEvent extends WorldEvent {
    public final int x;
    public final int y;
    public final int z;
    public final Random rand;

    public SaplingGrowTreeEvent(World world, Random rand, int x, int y, int z) {
        super(world);
        this.rand = rand;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
