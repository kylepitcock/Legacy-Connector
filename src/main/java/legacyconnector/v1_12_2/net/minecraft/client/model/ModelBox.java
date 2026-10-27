package legacyconnector.v1_12_2.net.minecraft.client.model;

import java.util.EnumSet;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/**
 * A textured box. {@link ModelRenderer} draws it as a modern {@link ModelPart.Cube} (the same uv unwrapping); the legacy quads stay
 * for code that reads them or draws them itself through {@link #render}.
 */
public class ModelBox {
    private final PositionTextureVertex[] vertexPositions;
    private final TexturedQuad[] quadList;
    public final float posX1;
    public final float posY1;
    public final float posZ1;
    public final float posX2;
    public final float posY2;
    public final float posZ2;
    public String boxName;
    /** The modern cube (client only: models are built by common code too). */
    ModelPart.Cube mcc$cube;

    public ModelBox(ModelRenderer renderer, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta) {
        this(renderer, texU, texV, x, y, z, dx, dy, dz, delta, renderer.mirror);
    }

    public ModelBox(ModelRenderer renderer, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror) {
        posX1 = x;
        posY1 = y;
        posZ1 = z;
        posX2 = x + dx;
        posY2 = y + dy;
        posZ2 = z + dz;
        mcc$cube = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                ? new ModelPart.Cube(texU, texV, x, y, z, dx, dy, dz, delta, delta, delta, mirror, renderer.textureWidth, renderer.textureHeight,
                        EnumSet.allOf(Direction.class))
                : null;
        vertexPositions = new PositionTextureVertex[8];
        quadList = new TexturedQuad[6];
        float f = x + dx, f1 = y + dy, f2 = z + dz;
        x = x - delta;
        y = y - delta;
        z = z - delta;
        f = f + delta;
        f1 = f1 + delta;
        f2 = f2 + delta;
        if (mirror) {
            float f3 = f;
            f = x;
            x = f3;
        }
        PositionTextureVertex p7 = new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
        PositionTextureVertex p0 = new PositionTextureVertex(f, y, z, 0.0F, 8.0F);
        PositionTextureVertex p1 = new PositionTextureVertex(f, f1, z, 8.0F, 8.0F);
        PositionTextureVertex p2 = new PositionTextureVertex(x, f1, z, 8.0F, 0.0F);
        PositionTextureVertex p3 = new PositionTextureVertex(x, y, f2, 0.0F, 0.0F);
        PositionTextureVertex p4 = new PositionTextureVertex(f, y, f2, 0.0F, 8.0F);
        PositionTextureVertex p5 = new PositionTextureVertex(f, f1, f2, 8.0F, 8.0F);
        PositionTextureVertex p6 = new PositionTextureVertex(x, f1, f2, 8.0F, 0.0F);
        vertexPositions[0] = p7;
        vertexPositions[1] = p0;
        vertexPositions[2] = p1;
        vertexPositions[3] = p2;
        vertexPositions[4] = p3;
        vertexPositions[5] = p4;
        vertexPositions[6] = p5;
        vertexPositions[7] = p6;
        float tw = renderer.textureWidth, th = renderer.textureHeight;
        quadList[0] = new TexturedQuad(new PositionTextureVertex[] {p4, p0, p1, p5}, texU + dz + dx, texV + dz, texU + dz + dx + dz, texV + dz + dy, tw, th);
        quadList[1] = new TexturedQuad(new PositionTextureVertex[] {p7, p3, p6, p2}, texU, texV + dz, texU + dz, texV + dz + dy, tw, th);
        quadList[2] = new TexturedQuad(new PositionTextureVertex[] {p4, p3, p7, p0}, texU + dz, texV, texU + dz + dx, texV + dz, tw, th);
        quadList[3] = new TexturedQuad(new PositionTextureVertex[] {p1, p2, p6, p5}, texU + dz + dx, texV + dz, texU + dz + dx + dx, texV, tw, th);
        quadList[4] = new TexturedQuad(new PositionTextureVertex[] {p0, p7, p2, p1}, texU + dz, texV + dz, texU + dz + dx, texV + dz + dy, tw, th);
        quadList[5] = new TexturedQuad(new PositionTextureVertex[] {p3, p4, p5, p6}, texU + dz + dx + dz, texV + dz, texU + dz + dx + dz + dx, texV + dz + dy, tw, th);
        if (mirror) for (TexturedQuad q : quadList) q.flipFace();
    }

    /** Draws the legacy quads through the buffer (the caller binds the texture), as 1.12.2 did. */
    public void render(BufferBuilder renderer, float scale) {
        for (TexturedQuad q : quadList) q.draw(renderer, scale);
    }

    public ModelBox setBoxName(String name) {
        boxName = name;
        return this;
    }
}
