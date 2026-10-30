package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/**
 * Forge 1.12.2's lighting context of the block being drawn: its world, state and position, the 3x3x3 light, opacity and ambient
 * occlusion values around it (for smooth lighting), the packed light of the block and its six neighbours (flat lighting), the block's
 * colour multiplier per tint index and its random offset.
 */
public class BlockInfo {
    private static final EnumFacing[] SIDES = EnumFacing.values();
    private final BlockColors colors;
    private IBlockAccess world;
    private IBlockState state;
    private BlockPos blockPos;
    private final boolean[][][] t = new boolean[3][3][3];
    private final int[][][] s = new int[3][3][3];
    private final int[][][] b = new int[3][3][3];
    private final float[][][][] skyLight = new float[3][2][2][2];
    private final float[][][][] blockLight = new float[3][2][2][2];
    private final float[][][] ao = new float[3][3][3];
    private final int[] packed = new int[7];
    private boolean full;
    private float shx = 0.0F;
    private float shy = 0.0F;
    private float shz = 0.0F;
    private int cachedTint = -1;
    private int cachedMultiplier = -1;

    public BlockInfo(BlockColors colors) {
        this.colors = colors;
    }

    public int getColorMultiplier(int tint) {
        if (cachedTint == tint) return cachedMultiplier;
        cachedTint = tint;
        cachedMultiplier = colors == null ? -1 : colors.colorMultiplier(state, world, blockPos, tint);
        return cachedMultiplier;
    }

    public void updateShift() {
        Vec3d offset = state == null ? null : state.getOffset(world, blockPos);
        if (offset == null) {
            shx = shy = shz = 0.0F;
            return;
        }
        shx = (float) offset.x;
        shy = (float) offset.y;
        shz = (float) offset.z;
    }

    public void setWorld(IBlockAccess world) {
        this.world = world;
        cachedTint = -1;
        cachedMultiplier = -1;
    }

    public void setState(IBlockState state) {
        this.state = state;
        cachedTint = -1;
        cachedMultiplier = -1;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
        cachedTint = -1;
        cachedMultiplier = -1;
        shx = shy = shz = 0.0F;
    }

    public void reset() {
        world = null;
        state = null;
        blockPos = null;
        cachedTint = -1;
        cachedMultiplier = -1;
        shx = shy = shz = 0.0F;
    }

    private float combine(int c, int s1, int s2, int s3, boolean t0, boolean t1, boolean t2, boolean t3) {
        if (c == 0 && !t0) c = Math.max(0, Math.max(s1, s2) - 1);
        if (s1 == 0 && !t1) s1 = Math.max(0, c - 1);
        if (s2 == 0 && !t2) s2 = Math.max(0, c - 1);
        if (s3 == 0 && !t3) s3 = Math.max(0, Math.max(s1, s2) - 1);
        return (float) (c + s1 + s2 + s3) * 0x20 / (4 * 0xFFFF);
    }

    public void updateLightMatrix() {
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    BlockPos pos = blockPos.add(x - 1, y - 1, z - 1);
                    IBlockState here = world.getBlockState(pos);
                    t[x][y][z] = here.getLightOpacity(world, pos) < 15;
                    int brightness = here.getPackedLightmapCoords(world, pos);
                    s[x][y][z] = (brightness >> 0x14) & 0xF;
                    b[x][y][z] = (brightness >> 0x04) & 0xF;
                    ao[x][y][z] = here.getAmbientOcclusionLightValue();
                }
            }
        }
        for (EnumFacing side : SIDES) {
            if (!state.doesSideBlockRendering(world, blockPos, side)) {
                int x = side.getXOffset() + 1;
                int y = side.getYOffset() + 1;
                int z = side.getZOffset() + 1;
                s[x][y][z] = Math.max(s[1][1][1] - 1, s[x][y][z]);
                b[x][y][z] = Math.max(b[1][1][1] - 1, b[x][y][z]);
            }
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int x1 = x * 2, y1 = y * 2, z1 = z * 2;
                    int sxyz = s[x1][y1][z1], bxyz = b[x1][y1][z1];
                    boolean txyz = t[x1][y1][z1];
                    int sxz = s[x1][1][z1], sxy = s[x1][y1][1], syz = s[1][y1][z1];
                    int bxz = b[x1][1][z1], bxy = b[x1][y1][1], byz = b[1][y1][z1];
                    boolean txz = t[x1][1][z1], txy = t[x1][y1][1], tyz = t[1][y1][z1];
                    int sx = s[x1][1][1], sy = s[1][y1][1], sz = s[1][1][z1];
                    int bx = b[x1][1][1], by = b[1][y1][1], bz = b[1][1][z1];
                    boolean tx = t[x1][1][1], ty = t[1][y1][1], tz = t[1][1][z1];
                    skyLight[0][x][y][z] = combine(sx, sxz, sxy, sxyz, tx, txz, txy, txyz);
                    blockLight[0][x][y][z] = combine(bx, bxz, bxy, bxyz, tx, txz, txy, txyz);
                    skyLight[1][x][y][z] = combine(sy, sxy, syz, sxyz, ty, txy, tyz, txyz);
                    blockLight[1][x][y][z] = combine(by, bxy, byz, bxyz, ty, txy, tyz, txyz);
                    skyLight[2][x][y][z] = combine(sz, syz, sxz, sxyz, tz, tyz, txz, txyz);
                    blockLight[2][x][y][z] = combine(bz, byz, bxz, bxyz, tz, tyz, txz, txyz);
                }
            }
        }
    }

    public void updateFlatLighting() {
        full = state.isFullCube();
        packed[0] = state.getPackedLightmapCoords(world, blockPos);
        for (EnumFacing side : SIDES) packed[side.ordinal() + 1] = state.getPackedLightmapCoords(world, blockPos.offset(side));
    }

    public IBlockAccess getWorld() {
        return world;
    }

    public IBlockState getState() {
        return state;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean[][][] getTranslucent() {
        return t;
    }

    public float[][][][] getSkyLight() {
        return skyLight;
    }

    public float[][][][] getBlockLight() {
        return blockLight;
    }

    public float[][][] getAo() {
        return ao;
    }

    public int[] getPackedLight() {
        return packed;
    }

    public boolean isFullCube() {
        return full;
    }

    public float getShx() {
        return shx;
    }

    public float getShy() {
        return shy;
    }

    public float getShz() {
        return shz;
    }
}
