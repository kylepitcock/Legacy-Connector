package legacyconnector.v1_12_2.net.minecraft.block.material;

/** Air-like materials (air, fire, structure void): not solid, no light blocking, no collision, replaceable. */
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
    public boolean blocksLight() {
        return false;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }
}
