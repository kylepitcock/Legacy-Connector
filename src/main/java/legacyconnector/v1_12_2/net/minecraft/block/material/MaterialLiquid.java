package legacyconnector.v1_12_2.net.minecraft.block.material;

/** Fluids: not solid, no collision, replaceable and destroyed by pistons. */
public class MaterialLiquid extends Material {
    public MaterialLiquid(MapColor color) {
        super(color);
        setReplaceable();
        setNoPushMobility();
    }

    @Override
    public boolean isLiquid() {
        return true;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public boolean isSolid() {
        return false;
    }
}
