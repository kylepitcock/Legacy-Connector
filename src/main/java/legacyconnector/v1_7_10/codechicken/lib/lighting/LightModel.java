package legacyconnector.v1_7_10.codechicken.lib.lighting;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;

/** Directional lighting for model faces: an ambient colour plus lights, applied to a vertex colour through its normal. */
public class LightModel {
    public static class Light {
        public final Vector3 ambient = new Vector3();
        public final Vector3 diffuse = new Vector3();
        public final Vector3 position;

        public Light(Vector3 position) {
            this.position = position.copy().normalize();
        }

        public Light setDiffuse(Vector3 d) {
            diffuse.set(d);
            return this;
        }

        public Light setAmbient(Vector3 a) {
            ambient.set(a);
            return this;
        }
    }

    /** Minecraft's own face shading: top bright, bottom dark, the sides in between. */
    public static final LightModel standardLightModel = new LightModel()
            .setAmbient(new Vector3(0.4, 0.4, 0.4))
            .addLight(new Light(new Vector3(0.2, 1, -0.7)).setDiffuse(new Vector3(0.6, 0.6, 0.6)))
            .addLight(new Light(new Vector3(-0.2, 1, 0.7)).setDiffuse(new Vector3(0.6, 0.6, 0.6)));

    private final Vector3 ambient = new Vector3();
    private final List<Light> lights = new ArrayList<>();

    public LightModel setAmbient(Vector3 vec) {
        ambient.set(vec);
        return this;
    }

    public LightModel addLight(Light light) {
        lights.add(light);
        return this;
    }

    /** The colour (0xRRGGBBAA) lit for a face with this normal. */
    public int apply(int colour, Vector3 normal) {
        double r = ambient.x;
        double g = ambient.y;
        double b = ambient.z;
        for (Light l : lights) {
            double d = Math.max(0, normal.dotProduct(l.position));
            r += l.ambient.x + l.diffuse.x * d;
            g += l.ambient.y + l.diffuse.y * d;
            b += l.ambient.z + l.diffuse.z * d;
        }
        int cr = Math.min(255, (int) ((colour >>> 24) * r));
        int cg = Math.min(255, (int) ((colour >> 16 & 0xFF) * g));
        int cb = Math.min(255, (int) ((colour >> 8 & 0xFF) * b));
        return cr << 24 | cg << 16 | cb << 8 | (colour & 0xFF);
    }
}
