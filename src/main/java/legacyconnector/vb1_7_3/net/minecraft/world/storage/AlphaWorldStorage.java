package legacyconnector.vb1_7_3.net.minecraft.world.storage;

import java.io.File;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerApiB173;

/** Directory access for legacy mods that save additional world data. */
public class AlphaWorldStorage implements WorldStorage {
    private final World world;
    public AlphaWorldStorage(World world){this.world=world;}
    public File getDirectory(){return PlayerApiB173.storageDirectory(world);}
}
