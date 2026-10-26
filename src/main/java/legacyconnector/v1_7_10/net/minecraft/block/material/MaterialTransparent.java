package legacyconnector.v1_7_10.net.minecraft.block.material;

/** Air and fire: not solid, replaceable, no collision. */
public class MaterialTransparent extends Material {
    public MaterialTransparent(MapColor color) {
        super(color);
        setReplaceable();
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
