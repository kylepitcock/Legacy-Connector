package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;

/**
 * Forge 1.12.2's smooth lighter: each vertex's light is interpolated from the 2x2x2 light corners of the faces around it and darkened
 * by the trilinear ambient occlusion of the 3x3x3 neighbourhood.
 */
public class VertexLighterSmoothAo extends VertexLighterFlat {
    public VertexLighterSmoothAo(BlockColors colors) {
        super(colors);
    }

    @Override
    protected void updateLightmap(float[] normal, float[] lightmap, float x, float y, float z) {
        lightmap[0] = calcLightmap(blockInfo.getBlockLight(), x, y, z);
        lightmap[1] = calcLightmap(blockInfo.getSkyLight(), x, y, z);
    }

    @Override
    protected void updateColor(float[] normal, float[] color, float x, float y, float z, float tint, int multiplier) {
        super.updateColor(normal, color, x, y, z, tint, multiplier);
        float a = getAo(x, y, z);
        color[0] *= a;
        color[1] *= a;
        color[2] *= a;
    }

    protected float calcLightmap(float[][][][] light, float x, float y, float z) {
        x *= 2;
        y *= 2;
        z *= 2;
        float l2 = x * x + y * y + z * z;
        if (l2 > 6 - 2e-2F) {
            float s = (float) Math.sqrt((6 - 2e-2F) / l2);
            x *= s;
            y *= s;
            z *= s;
        }
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        float e1 = 1 + 1e-4F;
        if (ax > 2 - 1e-4F && ay <= e1 && az <= e1) {
            x = x < 0 ? -2 + 1e-4F : 2 - 1e-4F;
        } else if (ay > 2 - 1e-4F && az <= e1 && ax <= e1) {
            y = y < 0 ? -2 + 1e-4F : 2 - 1e-4F;
        } else if (az > 2 - 1e-4F && ax <= e1 && ay <= e1) {
            z = z < 0 ? -2 + 1e-4F : 2 - 1e-4F;
        }
        ax = Math.abs(x);
        ay = Math.abs(y);
        az = Math.abs(z);
        if (ax <= e1 && ay + az > 3f - 1e-4F) {
            float s = (3f - 1e-4F) / (ay + az);
            y *= s;
            z *= s;
        } else if (ay <= e1 && az + ax > 3f - 1e-4F) {
            float s = (3f - 1e-4F) / (az + ax);
            z *= s;
            x *= s;
        } else if (az <= e1 && ax + ay > 3f - 1e-4F) {
            float s = (3f - 1e-4F) / (ax + ay);
            x *= s;
            y *= s;
        } else if (ax + ay + az > 4 - 1e-4F) {
            float s = (4 - 1e-4F) / (ax + ay + az);
            x *= s;
            y *= s;
            z *= s;
        }
        float l = 0;
        float s = 0;
        for (int ix = 0; ix <= 1; ix++) {
            for (int iy = 0; iy <= 1; iy++) {
                for (int iz = 0; iz <= 1; iz++) {
                    float vx = x * (1 - ix * 2);
                    float vy = y * (1 - iy * 2);
                    float vz = z * (1 - iz * 2);
                    float s3 = vx + vy + vz + 4;
                    float sx = vy + vz + 3;
                    float sy = vz + vx + 3;
                    float sz = vx + vy + 3;
                    float bx = (2 * vx + vy + vz + 6) / (s3 * sy * sz * (vx + 2));
                    s += bx;
                    l += bx * light[0][ix][iy][iz];
                    float by = (2 * vy + vz + vx + 6) / (s3 * sz * sx * (vy + 2));
                    s += by;
                    l += by * light[1][ix][iy][iz];
                    float bz = (2 * vz + vx + vy + 6) / (s3 * sx * sy * (vz + 2));
                    s += bz;
                    l += bz * light[2][ix][iy][iz];
                }
            }
        }
        if (s != 0) l /= s;
        if (l > 15f * 0x20 / 0xFFFF) l = 15f * 0x20 / 0xFFFF;
        if (l < 0) l = 0;
        return l;
    }

    protected float getAo(float x, float y, float z) {
        int sx = x < 0 ? 1 : 2;
        int sy = y < 0 ? 1 : 2;
        int sz = z < 0 ? 1 : 2;
        if (x < 0) x++;
        if (y < 0) y++;
        if (z < 0) z++;
        float[][][] ao = blockInfo.getAo();
        float a = 0;
        a += ao[sx - 1][sy - 1][sz - 1] * (1 - x) * (1 - y) * (1 - z);
        a += ao[sx - 1][sy - 1][sz] * (1 - x) * (1 - y) * z;
        a += ao[sx - 1][sy][sz - 1] * (1 - x) * y * (1 - z);
        a += ao[sx - 1][sy][sz] * (1 - x) * y * z;
        a += ao[sx][sy - 1][sz - 1] * x * (1 - y) * (1 - z);
        a += ao[sx][sy - 1][sz] * x * (1 - y) * z;
        a += ao[sx][sy][sz - 1] * x * y * (1 - z);
        a += ao[sx][sy][sz] * x * y * z;
        return Math.max(0.0F, Math.min(1.0F, a));
    }

    @Override
    public void updateBlockInfo() {
        blockInfo.updateShift();
        blockInfo.updateLightMatrix();
    }
}
