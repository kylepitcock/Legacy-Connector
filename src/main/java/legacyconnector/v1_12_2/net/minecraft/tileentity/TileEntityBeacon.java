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

    /** The coloured parts of the beam (a mod's beacon fills them in its update; a view reads the modern beacon's). */
    private final java.util.List<BeamSegment> beamSegments = new java.util.ArrayList<>();
    /** Whether the beam reaches the sky (a full pyramid is not needed). */
    private boolean isComplete;

    public java.util.List<BeamSegment> getBeamSegments() {
        if (mcc$beacon != null) {
            beamSegments.clear();
            for (var section : mcc$beacon.getBeamSections()) {
                int c = section.getColor();
                BeamSegment segment = new BeamSegment(new float[] {(c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F});
                segment.height = section.getHeight();
                beamSegments.add(segment);
            }
        }
        return beamSegments;
    }

    /** How strongly the beam is drawn: fully while it reaches the sky, not at all otherwise. */
    public float shouldBeamRender() {
        if (mcc$beacon != null) return mcc$beacon.getBeamSections().isEmpty() ? 0.0F : 1.0F;
        return isComplete ? 1.0F : 0.0F;
    }

    /** A vertical part of the beam with one colour. */
    public static class BeamSegment {
        private final float[] colors;
        private int height;

        public BeamSegment(float[] colorsIn) {
            this.colors = colorsIn;
            this.height = 1;
        }

        protected void incrementHeight() {
            ++this.height;
        }

        public float[] getColors() {
            return colors;
        }

        public int getHeight() {
            return height;
        }
    }
}
