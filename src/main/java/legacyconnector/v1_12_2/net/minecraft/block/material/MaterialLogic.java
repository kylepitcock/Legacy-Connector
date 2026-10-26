package legacyconnector.v1_12_2.net.minecraft.block.material;

/** Thin blocks (rails, wires, plants): not solid, let light through, no collision. */
public class MaterialLogic extends Material {
    public MaterialLogic(MapColor color) {
        super(color);
        setAdventureModeExempt();
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
