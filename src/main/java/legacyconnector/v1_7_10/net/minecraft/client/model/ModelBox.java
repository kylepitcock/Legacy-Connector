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
        // vanilla 1.7.10's unwrapping (a mirrored box flips x), so code reading or drawing the quads sees the real UVs
        float x1 = x - grow, y1 = y - grow, z1 = z - grow;
        float x2 = x + w + grow, y2 = y + h + grow, z2 = z + d + grow;
        if (renderer.mirror) {
            float t = x2;
            x2 = x1;
            x1 = t;
        }
        PositionTextureVertex p0 = new PositionTextureVertex(x1, y1, z1, 0.0F, 0.0F);
        PositionTextureVertex p1 = new PositionTextureVertex(x2, y1, z1, 0.0F, 8.0F);
        PositionTextureVertex p2 = new PositionTextureVertex(x2, y2, z1, 8.0F, 8.0F);
        PositionTextureVertex p3 = new PositionTextureVertex(x1, y2, z1, 8.0F, 0.0F);
        PositionTextureVertex p4 = new PositionTextureVertex(x1, y1, z2, 0.0F, 0.0F);
        PositionTextureVertex p5 = new PositionTextureVertex(x2, y1, z2, 0.0F, 8.0F);
        PositionTextureVertex p6 = new PositionTextureVertex(x2, y2, z2, 8.0F, 8.0F);
        PositionTextureVertex p7 = new PositionTextureVertex(x1, y2, z2, 8.0F, 0.0F);
        this.vertexPositions = new PositionTextureVertex[] {p0, p1, p2, p3, p4, p5, p6, p7};
        float tw = renderer.textureWidth, th = renderer.textureHeight;
        this.quadList = new TexturedQuad[] {
                new TexturedQuad(new PositionTextureVertex[] {p5, p1, p2, p6}, u + d + w, v + d, u + d + w + d, v + d + h, tw, th),
                new TexturedQuad(new PositionTextureVertex[] {p0, p4, p7, p3}, u, v + d, u + d, v + d + h, tw, th),
                new TexturedQuad(new PositionTextureVertex[] {p5, p4, p0, p1}, u + d, v, u + d + w, v + d, tw, th),
                new TexturedQuad(new PositionTextureVertex[] {p2, p3, p7, p6}, u + d + w, v + d, u + d + w + w, v, tw, th),
                new TexturedQuad(new PositionTextureVertex[] {p1, p0, p3, p2}, u + d, v + d, u + d + w, v + d + h, tw, th),
                new TexturedQuad(new PositionTextureVertex[] {p4, p5, p6, p7}, u + d + w + d, v + d, u + d + w + d + w, v + d + h, tw, th)};
        if (renderer.mirror) for (TexturedQuad q : quadList) q.flipFace();
    }

    /** Draws the box's quads (1.7.10's immediate-mode path: the caller binds the texture). */
    public void render(legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator tessellator, float scale) {
        for (TexturedQuad q : quadList) q.draw(tessellator, scale);
    }

    public ModelBox func_78244_a(String name) {
        this.field_78247_g = name;
        return this;
    }
}
