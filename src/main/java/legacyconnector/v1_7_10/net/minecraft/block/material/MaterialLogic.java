package legacyconnector.v1_7_10.net.minecraft.block.material;

/** Plants, rails, redstone and carpets: not solid, no collision, breakable in adventure mode. */
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
    public boolean getCanBlockGrass() {
        return false;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }
}
