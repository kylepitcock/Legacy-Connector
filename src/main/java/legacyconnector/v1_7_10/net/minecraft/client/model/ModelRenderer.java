package legacyconnector.v1_7_10.net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10 model part. Rendering applies the legacy transform (rotation point, Z-Y-X rotation, offsets) to the active
 * {@link PoseStack} and emits the boxes into an entity buffer for the currently bound texture.
 */
public class ModelRenderer {
    public float textureWidth;
    public float textureHeight;
    private int textureOffsetX;
    private int textureOffsetY;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public boolean mirror;
    public boolean showModel = true;
    public boolean isHidden;
    public List cubeList = new ArrayList();
    public List childModels;
    public final String boxName;
    private final ModelBase baseModel;
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public ModelRenderer(ModelBase model, String name) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.baseModel = model;
        this.boxName = name;
        model.boxList.add(this);
        setTextureSize(model.textureWidth, model.textureHeight);
    }

    public ModelRenderer(ModelBase model) {
        this(model, null);
    }

    public ModelRenderer(ModelBase model, int u, int v) {
        this(model);
        setTextureOffset(u, v);
    }

    public void addChild(ModelRenderer child) {
        if (childModels == null) childModels = new ArrayList();
        childModels.add(child);
    }

    public ModelRenderer setTextureOffset(int u, int v) {
        this.textureOffsetX = u;
        this.textureOffsetY = v;
        return this;
    }

    public ModelRenderer addBox(String name, float x, float y, float z, int w, int h, int d) {
        name = boxName + "." + name;
        int[] off = baseModel.mcc$textureOffset(name);
        if (off != null) setTextureOffset(off[0], off[1]);
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, x, y, z, w, h, d, 0.0F).func_78244_a(name));
        return this;
    }

    public ModelRenderer addBox(float x, float y, float z, int w, int h, int d) {
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, x, y, z, w, h, d, 0.0F));
        return this;
    }

    public void addBox(float x, float y, float z, int w, int h, int d, float grow) {
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, x, y, z, w, h, d, grow));
    }

    public void setRotationPoint(float x, float y, float z) {
        rotationPointX = x;
        rotationPointY = y;
        rotationPointZ = z;
    }

    public ModelRenderer setTextureSize(int w, int h) {
        textureWidth = w;
        textureHeight = h;
        return this;
    }

    public void render(float scale) {
        if (isHidden || !showModel) return;
        PoseStack pose = RenderContext.pose();
        if (pose == null) return;
        pose.pushPose();
        pose.translate(offsetX, offsetY, offsetZ);
        transform(pose, scale);
        draw(pose, scale);
        if (childModels != null) for (Object c : childModels) ((ModelRenderer) c).render(scale);
        pose.popPose();
    }

    public void renderWithRotation(float scale) {
        render(scale);
    }

    /** Applies this part's transform to the stack without drawing (used to attach other renders to a part). */
    public void postRender(float scale) {
        if (isHidden || !showModel) return;
        PoseStack pose = RenderContext.pose();
        if (pose != null) transform(pose, scale);
    }

    private void transform(PoseStack pose, float scale) {
        pose.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleX != 0 || rotateAngleY != 0 || rotateAngleZ != 0) {
            pose.mulPose(new Quaternionf().rotationZYX(rotateAngleZ, rotateAngleY, rotateAngleX));
        }
    }

    private void draw(PoseStack pose, float scale) {
        if (cubeList.isEmpty()) return;
        // Modern cubes are emitted in 1/16 units; legacy passes the unit scale explicitly (almost always 1/16).
        boolean rescale = Math.abs(scale - 0.0625F) > 1.0E-6F;
        if (rescale) {
            pose.pushPose();
            pose.scale(scale * 16.0F, scale * 16.0F, scale * 16.0F);
        }
        var buffer = RenderContext.modelBuffer();
        int light = RenderContext.light(), overlay = RenderContext.overlay(), color = RenderContext.argb();
        for (Object o : cubeList) ((ModelBox) o).mcc$cube.compile(pose.last(), buffer, light, overlay, color);
        if (rescale) pose.popPose();
    }
}
