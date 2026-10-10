package mccompat.v1_7_10.net.minecraft.client.model;

import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/** A textured box; becomes a modern {@link ModelPart.Cube}, which uses the identical UV unwrapping. */
public class ModelBox {
    public final float posX1;
    public final float posY1;
    public final float posZ1;
    public final float posX2;
    public final float posY2;
    public final float posZ2;
    public String field_78247_g;
    final ModelPart.Cube mcc$cube;

    public ModelBox(ModelRenderer renderer, int u, int v, float x, float y, float z, int w, int h, int d, float grow) {
        this.posX1 = x;
        this.posY1 = y;
        this.posZ1 = z;
        this.posX2 = x + w;
        this.posY2 = y + h;
        this.posZ2 = z + d;
        this.mcc$cube = new ModelPart.Cube(u, v, x, y, z, w, h, d, grow, grow, grow, renderer.mirror, renderer.textureWidth, renderer.textureHeight,
                EnumSet.allOf(Direction.class));
    }

    public ModelBox func_78244_a(String name) {
        this.field_78247_g = name;
        return this;
    }
}
