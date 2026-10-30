package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.ITransformation;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;
import org.kylepitcock.legacyconnector.v1_12_2.client.Quads1122;

/**
 * 1.12.2 face bakery: one element face into a baked quad. The modern face bakery does the work (same corner order, uv rotation, uv lock,
 * element rotation and shading), and the result is converted to the legacy quad layout.
 */
public class FaceBakery {
    public BakedQuad makeBakedQuad(Vector3f posFrom, Vector3f posTo, BlockPartFace face, TextureAtlasSprite sprite, EnumFacing facing, ModelRotation modelRotationIn,
            BlockPartRotation partRotation, boolean uvLocked, boolean shade) {
        return makeBakedQuad(posFrom, posTo, face, sprite, facing, (ITransformation) modelRotationIn, partRotation, uvLocked, shade);
    }

    public BakedQuad makeBakedQuad(Vector3f posFrom, Vector3f posTo, BlockPartFace face, TextureAtlasSprite sprite, EnumFacing facing, ITransformation modelRotationIn,
            BlockPartRotation partRotation, boolean uvLocked, boolean shade) {
        net.minecraft.client.resources.model.ModelState state = modelState(modelRotationIn, uvLocked);
        var modernSprite = sprite == null ? Quads1122.missingSprite() : sprite.mcc$modern();
        if (modernSprite == null) modernSprite = Quads1122.missingSprite();
        var quad = new net.minecraft.client.renderer.block.model.FaceBakery().bakeQuad(new org.joml.Vector3f(posFrom.x, posFrom.y, posFrom.z),
                new org.joml.Vector3f(posTo.x, posTo.y, posTo.z), face.mcc$modern(), modernSprite, facing.mcc$modern(), state,
                partRotation == null ? null : partRotation.mcc$modern(), shade);
        return Quads1122.toLegacy(quad);
    }

    private static net.minecraft.client.resources.model.ModelState modelState(ITransformation transform, boolean uvLocked) {
        if (transform instanceof ModelRotation rotation) {
            var modern = rotation.mcc$modern();
            if (!uvLocked) return modern;
            return new net.minecraft.client.resources.model.ModelState() {
                @Override
                public com.mojang.math.Transformation getRotation() {
                    return modern.getRotation();
                }

                @Override
                public boolean isUvLocked() {
                    return true;
                }
            };
        }
        javax.vecmath.Matrix4f m = transform == null ? null : transform.getMatrix();
        org.joml.Matrix4f joml = new org.joml.Matrix4f();
        if (m != null) {
            joml.set(m.m00, m.m10, m.m20, m.m30, m.m01, m.m11, m.m21, m.m31, m.m02, m.m12, m.m22, m.m32, m.m03, m.m13, m.m23, m.m33);
        }
        com.mojang.math.Transformation t = new com.mojang.math.Transformation(joml);
        return new net.minecraft.client.resources.model.ModelState() {
            @Override
            public com.mojang.math.Transformation getRotation() {
                return t;
            }

            @Override
            public boolean isUvLocked() {
                return uvLocked;
            }
        };
    }

    /** The side a baked quad faces, from the normal of its first three vertices (7 ints per vertex, position first); UP if none fits. */
    public static EnumFacing getFacingFromVertexData(int[] faceData) {
        float x0 = Float.intBitsToFloat(faceData[0]), y0 = Float.intBitsToFloat(faceData[1]), z0 = Float.intBitsToFloat(faceData[2]);
        float x1 = Float.intBitsToFloat(faceData[7]), y1 = Float.intBitsToFloat(faceData[8]), z1 = Float.intBitsToFloat(faceData[9]);
        float x2 = Float.intBitsToFloat(faceData[14]), y2 = Float.intBitsToFloat(faceData[15]), z2 = Float.intBitsToFloat(faceData[16]);
        float ax = x0 - x1, ay = y0 - y1, az = z0 - z1;
        float bx = x2 - x1, by = y2 - y1, bz = z2 - z1;
        float nx = by * az - bz * ay, ny = bz * ax - bx * az, nz = bx * ay - by * ax;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0F) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        EnumFacing best = null;
        float f = 0.0F;
        for (EnumFacing facing : EnumFacing.values()) {
            float dot = nx * facing.getFrontOffsetX() + ny * facing.getFrontOffsetY() + nz * facing.getFrontOffsetZ();
            if (dot >= 0.0F && dot > f) {
                f = dot;
                best = facing;
            }
        }
        return best == null ? EnumFacing.UP : best;
    }
}
