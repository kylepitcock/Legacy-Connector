package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.init.MobEffects;
import legacyconnector.v1_12_2.net.minecraft.potion.Potion;

/**
 * 1.12.2's beacon: the effects it can give per pyramid level and the pyramid level it has. A beacon of the game is seen through a view
 * reading the modern beacon's level.
 */
public class TileEntityBeacon extends TileEntity implements org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    public static final Potion[][] EFFECTS_LIST = new Potion[][] {{MobEffects.SPEED, MobEffects.HASTE}, {MobEffects.RESISTANCE, MobEffects.JUMP_BOOST},
            {MobEffects.STRENGTH}, {MobEffects.REGENERATION}};
    private int levels = -1;
    private final net.minecraft.world.level.block.entity.BeaconBlockEntity mcc$beacon;

    public TileEntityBeacon() {
        this.mcc$beacon = null;
    }

    /** A view of a game beacon. */
    public TileEntityBeacon(net.minecraft.world.level.block.entity.BeaconBlockEntity beacon) {
        this.mcc$beacon = beacon;
        mcc$refresh();
    }

    @Override
    public void mcc$refresh() {
        if (mcc$beacon == null) return;
        try {
            var f = net.minecraft.world.level.block.entity.BeaconBlockEntity.class.getDeclaredField("levels");
            f.setAccessible(true);
            levels = f.getInt(mcc$beacon);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // keep the last level
        }
    }

    /** The pyramid level (0-4; -1 before the first check). */
    public int getLevels() {
        return levels;
    }
}
