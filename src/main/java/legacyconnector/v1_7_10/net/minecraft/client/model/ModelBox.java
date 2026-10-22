package legacyconnector.v1_7_10.net.minecraft.client.model;

import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/** A textured box; becomes a modern {@link ModelPart.Cube}, which uses the identical UV unwrapping. */
public class ModelBox {
    // Declaration order matters: mods read these by field index (ObfuscationReflectionHelper.getPrivateValue(.., 1)).
    private PositionTextureVertex[] vertexPositions;
    /** The six faces (west, east, down, up, north, south as in vanilla's unwrapping), for code that reads or adjusts vertices. */
    public TexturedQuad[] quadList;
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
        // Models are built by common code too (entity constructors); only a client has the modern cube class.
        this.mcc$cube = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                ? new ModelPart.Cube(u, v, x, y, z, w, h, d, grow, grow, grow, renderer.mirror, renderer.textureWidth, renderer.textureHeight,
                        EnumSet.allOf(Direction.class))
                : null;
        float x2 = x + w, y2 = y + h, z2 = z + d;
        PositionTextureVertex v0 = new PositionTextureVertex(x2, y, z, 0.0F, 0.0F);
        PositionTextureVertex v1 = new PositionTextureVertex(x, y, z, 0.0F, 8.0F);
        PositionTextureVertex v2 = new PositionTextureVertex(x, y2, z, 8.0F, 8.0F);
        PositionTextureVertex v3 = new PositionTextureVertex(x2, y2, z, 8.0F, 0.0F);
        PositionTextureVertex v4 = new PositionTextureVertex(x2, y, z2, 0.0F, 0.0F);
        PositionTextureVertex v5 = new PositionTextureVertex(x, y, z2, 0.0F, 8.0F);
        PositionTextureVertex v6 = new PositionTextureVertex(x, y2, z2, 8.0F, 8.0F);
        PositionTextureVertex v7 = new PositionTextureVertex(x2, y2, z2, 8.0F, 0.0F);
        this.vertexPositions = new PositionTextureVertex[] {v0, v1, v2, v3, v4, v5, v6, v7};
        this.quadList = new TexturedQuad[] {
                new TexturedQuad(new PositionTextureVertex[] {v4, v0, v3, v7}), new TexturedQuad(new PositionTextureVertex[] {v1, v5, v6, v2}),
                new TexturedQuad(new PositionTextureVertex[] {v4, v5, v1, v0}), new TexturedQuad(new PositionTextureVertex[] {v3, v2, v6, v7}),
                new TexturedQuad(new PositionTextureVertex[] {v0, v1, v2, v3}), new TexturedQuad(new PositionTextureVertex[] {v5, v4, v7, v6})};
    }

    public ModelBox func_78244_a(String name) {
        this.field_78247_g = name;
        return this;
    }
}
