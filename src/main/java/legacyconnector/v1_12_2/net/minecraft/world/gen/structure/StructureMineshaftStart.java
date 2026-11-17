package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 start of a mineshaft. The modern game places mineshafts itself, so a start created by legacy code (a mod's own mineshaft
 * generator) holds the type and the chunk but builds no corridors of its own: it has no pieces and reports an empty bounding box.
 */
public class StructureMineshaftStart extends StructureStart {
    private final MapGenMineshaft.Type mineShaftType;

    public StructureMineshaftStart() {
        this.mineShaftType = MapGenMineshaft.Type.NORMAL;
    }

    public StructureMineshaftStart(World worldIn, Random rand, int chunkX, int chunkZ, MapGenMineshaft.Type type) {
        super(chunkX, chunkZ);
        this.mineShaftType = type == null ? MapGenMineshaft.Type.NORMAL : type;
    }

    public MapGenMineshaft.Type getType() {
        return mineShaftType;
    }
}
