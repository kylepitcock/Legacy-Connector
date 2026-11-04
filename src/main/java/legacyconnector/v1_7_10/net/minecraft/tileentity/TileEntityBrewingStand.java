package legacyconnector.v1_7_10.net.minecraft.tileentity;

import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInventoryTile;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/** View of a vanilla brewing stand: its slots through the inventory view, plus the brew time. */
public class TileEntityBrewingStand extends VanillaInventoryTile {
    private final net.minecraft.world.level.block.entity.BrewingStandBlockEntity mcc$stand;

    /** Own brew time when this is a mod's subclass (no vanilla stand behind it). */
    private int mcc$ownBrewTime;

    /** A mod's subclass (custom brewing stands): four slots of its own, no vanilla block entity behind it. */
    public TileEntityBrewingStand() {
        super(new net.minecraft.world.SimpleContainer(4));
        this.mcc$stand = null;
    }

    public TileEntityBrewingStand(net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
        super(stand);
        this.mcc$stand = stand;
    }

    /** Ticks left of the brew (400 at the start, 0 when idle). */
    public int getBrewTime() {
        if (mcc$stand == null) return mcc$ownBrewTime;
        return VanillaOps.getInt(mcc$stand, net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class, "brewTime", 0);
    }

    public void func_145938_d(int ticks) {
        if (mcc$stand == null) {
            mcc$ownBrewTime = ticks;
            return;
        }
        VanillaOps.setInt(mcc$stand, net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class, "brewTime", ticks);
    }

    /** MCP name of {@link #getBrewTime}. */
    public int func_145935_i() {
        return getBrewTime();
    }
}
