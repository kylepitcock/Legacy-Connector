package legacyconnector.v1_20_1;

import java.util.Objects;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

/** Bridges the 1.20.1 Sheep.getColorArray result to the 1.21.1 packed sheep RGB color. */
public final class PalladiumColorCompat {
    private PalladiumColorCompat() {
    }

    /** Returns normalized red, green, and blue components in the legacy float-array shape. */
    public static float[] sheepColorArray(DyeColor color) {
        Objects.requireNonNull(color, "Dye color");
        return rgbToFloatArray(Sheep.getColor(color));
    }

    static float[] rgbToFloatArray(int rgb) {
        return new float[] {
                ((rgb >>> 16) & 0xFF) / 255.0F,
                ((rgb >>> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F
        };
    }
}
