package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/** Passes quads on with their positions moved by a transformation and their normals turned by its normal matrix. */
public class TRSRTransformer extends VertexTransformer {
    private final TRSRTransformation transformation;

    public TRSRTransformer(IVertexConsumer parent, TRSRTransformation transformation) {
        super(parent);
        this.transformation = transformation == null ? TRSRTransformation.identity() : transformation;
    }

    @Override
    public void put(int element, float... data) {
        VertexFormatElement.EnumUsage usage = getVertexFormat().getElement(element).getUsage();
        if (usage == VertexFormatElement.EnumUsage.POSITION) {
            Vector4f pos = new Vector4f(at(data, 0), at(data, 1), at(data, 2), 1.0F);
            transformation.transformPosition(pos);
            if (pos.w != 0.0F && pos.w != 1.0F) pos.scale(1.0F / pos.w);
            data = copy(data, 3);
            data[0] = pos.x;
            data[1] = pos.y;
            data[2] = pos.z;
        } else if (usage == VertexFormatElement.EnumUsage.NORMAL) {
            Vector3f normal = new Vector3f(at(data, 0), at(data, 1), at(data, 2));
            if (normal.lengthSquared() > 0.0F) transformation.transformNormal(normal);
            data = copy(data, 3);
            data[0] = normal.x;
            data[1] = normal.y;
            data[2] = normal.z;
        }
        super.put(element, data);
    }

    private static float at(float[] data, int i) {
        return i < data.length ? data[i] : 0.0F;
    }

    private static float[] copy(float[] data, int min) {
        float[] out = new float[Math.max(min, data.length)];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }
}
