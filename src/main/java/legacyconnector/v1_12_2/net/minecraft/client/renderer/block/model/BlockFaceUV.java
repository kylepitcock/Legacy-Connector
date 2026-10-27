package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

/** 1.12.2 face texture coordinates of a model element (u1, v1, u2, v2 in 0..16) and their rotation in degrees. */
public class BlockFaceUV {
    public float[] uvs;
    public final int rotation;

    public BlockFaceUV(float[] uvsIn, int rotationIn) {
        this.uvs = uvsIn;
        this.rotation = rotationIn;
    }

    public float getVertexU(int index) {
        if (uvs == null) throw new NullPointerException("uvs");
        int i = getVertexRotated(index);
        return i != 0 && i != 1 ? uvs[2] : uvs[0];
    }

    public float getVertexV(int index) {
        if (uvs == null) throw new NullPointerException("uvs");
        int i = getVertexRotated(index);
        return i != 0 && i != 3 ? uvs[3] : uvs[1];
    }

    private int getVertexRotated(int index) {
        return (index + rotation / 90) % 4;
    }

    public int getVertexRotatedRev(int index) {
        return (index + 4 - rotation / 90) % 4;
    }

    public void setUvs(float[] uvsIn) {
        if (uvs == null) uvs = uvsIn;
    }

    net.minecraft.client.renderer.block.model.BlockFaceUV mcc$modern() {
        return new net.minecraft.client.renderer.block.model.BlockFaceUV(uvs == null ? null : uvs.clone(), rotation);
    }
}
