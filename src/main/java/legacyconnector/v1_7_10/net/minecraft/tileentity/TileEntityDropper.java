package legacyconnector.v1_7_10.net.minecraft.tileentity;

/** The dropper's nine slots (the same as the dispenser's). */
public class TileEntityDropper extends TileEntityDispenser {
    public TileEntityDropper() {
        super();
    }

    public TileEntityDropper(net.minecraft.world.level.block.entity.DispenserBlockEntity dropper) {
        super(dropper);
    }
}
