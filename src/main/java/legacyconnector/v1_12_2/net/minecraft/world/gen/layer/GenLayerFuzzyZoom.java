package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

/** Zoom whose corner cells are chosen at random instead of by majority. */
public class GenLayerFuzzyZoom extends GenLayerZoom {
    public GenLayerFuzzyZoom(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    protected int selectModeOrRandom(int a, int b, int c, int d) {
        return selectRandom(a, b, c, d);
    }
}
