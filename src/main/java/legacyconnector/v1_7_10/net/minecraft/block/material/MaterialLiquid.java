package legacyconnector.v1_7_10.net.minecraft.block.material;

/** The material of water and lava style fluids: not solid, replaceable, no movement blocking. */
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
