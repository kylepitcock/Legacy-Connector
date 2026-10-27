package legacyconnector.v1_12_2.net.minecraft.client.model;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import org.joml.Quaternionf;
import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 model part. Rendering applies the legacy transform (offset, rotation point, Z-Y-X rotation) to the render context's matrix
 * stack and emits the boxes as modern cubes into an entity buffer for the bound texture, with the current colour and lightmap
 * coordinates. Inside a display list being compiled the draw is recorded and replayed with each call of the list.
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
    private boolean compiled;
    private int displayList;
    public boolean mirror;
    public boolean showModel = true;
    public boolean isHidden;
    public List<ModelBox> cubeList = Lists.newArrayList();
    public List<ModelRenderer> childModels;
    public final String boxName;
    private final ModelBase baseModel;
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public ModelRenderer(ModelBase model, String boxNameIn) {
        textureWidth = 64.0F;
        textureHeight = 32.0F;
        baseModel = model;
        model.boxList.add(this);
        boxName = boxNameIn;
        setTextureSize(model.textureWidth, model.textureHeight);
    }

    public ModelRenderer(ModelBase model) {
        this(model, null);
    }

    public ModelRenderer(ModelBase model, int texOffX, int texOffY) {
        this(model);
        setTextureOffset(texOffX, texOffY);
    }

    public void addChild(ModelRenderer renderer) {
        if (childModels == null) childModels = Lists.newArrayList();
        childModels.add(renderer);
    }

    public ModelRenderer setTextureOffset(int x, int y) {
        textureOffsetX = x;
        textureOffsetY = y;
        return this;
    }

    public ModelRenderer addBox(String partName, float offX, float offY, float offZ, int width, int height, int depth) {
        partName = boxName + "." + partName;
        TextureOffset offset = baseModel.getTextureOffset(partName);
        if (offset != null) setTextureOffset(offset.textureOffsetX, offset.textureOffsetY);
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, offX, offY, offZ, width, height, depth, 0.0F).setBoxName(partName));
        return this;
    }

    public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth) {
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, offX, offY, offZ, width, height, depth, 0.0F));
        return this;
    }

    public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth, boolean mirrored) {
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, offX, offY, offZ, width, height, depth, 0.0F, mirrored));
        return this;
    }

    public void addBox(float offX, float offY, float offZ, int width, int height, int depth, float scaleFactor) {
        cubeList.add(new ModelBox(this, textureOffsetX, textureOffsetY, offX, offY, offZ, width, height, depth, scaleFactor));
    }

    public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn) {
        rotationPointX = rotationPointXIn;
        rotationPointY = rotationPointYIn;
        rotationPointZ = rotationPointZIn;
    }

    public void render(float scale) {
        if (isHidden || !showModel) return;
        if (DisplayLists.record(() -> render(scale))) return;
        PoseStack pose = RenderContext.pose();
        if (pose == null) return;
        pose.pushPose();
        try {
            pose.translate(offsetX, offsetY, offsetZ);
            transform(pose, scale);
            draw(pose, scale);
            if (childModels != null) for (ModelRenderer child : childModels) child.render(scale);
        } finally {
            pose.popPose();
        }
    }

    public void renderWithRotation(float scale) {
        render(scale);
    }

    /** Applies this part's transform to the matrix stack without drawing (attaching other renders to a part). */
    public void postRender(float scale) {
        if (isHidden || !showModel) return;
        if (DisplayLists.record(() -> postRender(scale))) return;
        PoseStack pose = RenderContext.pose();
        if (pose != null) transform(pose, scale);
    }

    private void transform(PoseStack pose, float scale) {
        pose.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleX != 0 || rotateAngleY != 0 || rotateAngleZ != 0) pose.mulPose(new Quaternionf().rotationZYX(rotateAngleZ, rotateAngleY, rotateAngleX));
    }

    private void draw(PoseStack pose, float scale) {
        if (cubeList.isEmpty()) return;
        // modern cubes are emitted in 1/16 units; legacy code passes the unit scale explicitly (almost always 1/16)
        boolean rescale = Math.abs(scale - 0.0625F) > 1.0E-6F;
        if (rescale) {
            pose.pushPose();
            pose.scale(scale * 16.0F, scale * 16.0F, scale * 16.0F);
        }
        var buffer = RenderContext.modelBuffer();
        int light = RenderContext.vertexLight(), overlay = RenderContext.overlay(), color = RenderContext.argb();
        for (ModelBox box : cubeList) {
            if (box.mcc$cube != null) box.mcc$cube.compile(pose.last(), buffer, light, overlay, color);
        }
        if (rescale) pose.popPose();
    }

    public ModelRenderer setTextureSize(int textureWidthIn, int textureHeightIn) {
        textureWidth = textureWidthIn;
        textureHeight = textureHeightIn;
        return this;
    }
}
