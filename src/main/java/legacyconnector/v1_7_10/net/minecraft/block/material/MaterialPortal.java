package legacyconnector.v1_7_10.net.minecraft.block.material;

/** Portals: not solid, no collision. */
public class MaterialPortal extends Material {
    public MaterialPortal(MapColor color) {
        super(color);
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean getCanBlockGrass() {
        return false;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }
}
