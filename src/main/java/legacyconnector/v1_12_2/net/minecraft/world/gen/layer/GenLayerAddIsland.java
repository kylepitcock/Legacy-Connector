package legacyconnector.v1_12_2.net.minecraft.world.gen.layer;

/** Grows and erodes land along coasts using the diagonal neighbours. */
public class GenLayerAddIsland extends GenLayerNeighbors {
    public GenLayerAddIsland(long seed, GenLayer parent) {
        super(seed, parent);
    }

    @Override
    int transform(int c, int n, int e, int w, int s, int a, int b, int d, int f) {
        if (c != 0 || (a == 0 && b == 0 && d == 0 && f == 0)) {
            if (c > 0 && (a == 0 || b == 0 || d == 0 || f == 0)) {
                if (nextInt(5) == 0) return c == 4 ? 4 : 0;
                return c;
            }
            return c;
        }
        int count = 1;
        int v = 1;
        if (a != 0 && nextInt(count++) == 0) v = a;
        if (b != 0 && nextInt(count++) == 0) v = b;
        if (d != 0 && nextInt(count++) == 0) v = d;
        if (f != 0 && nextInt(count++) == 0) v = f;
        if (nextInt(3) == 0) return v;
        return v == 4 ? 4 : 0;
    }
}
