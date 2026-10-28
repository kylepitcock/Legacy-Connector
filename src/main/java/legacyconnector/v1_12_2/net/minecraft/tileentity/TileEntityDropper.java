package legacyconnector.v1_12_2.net.minecraft.tileentity;

/** 1.12.2's dropper: a dispenser that drops its items instead of using them. */
public class TileEntityDropper extends TileEntityDispenser {
    public TileEntityDropper() {
    }

    /** A view of a game dropper. */
    public TileEntityDropper(net.minecraft.world.level.block.entity.DropperBlockEntity dropper) {
        super(dropper);
    }

    @Override
    public String getName() {
        return hasCustomName() ? customName : "container.dropper";
    }

    @Override
    public String getGuiID() {
        return "minecraft:dropper";
    }
}
