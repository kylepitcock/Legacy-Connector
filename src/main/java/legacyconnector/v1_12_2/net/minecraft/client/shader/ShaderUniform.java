package legacyconnector.v1_12_2.net.minecraft.client.shader;

import com.mojang.blaze3d.shaders.AbstractUniform;

/** 1.12.2 shader uniform: sets the matching uniform of the modern shader program (a missing uniform takes the values and ignores them). */
public class ShaderUniform {
    private final String shaderName;
    private final int uniformType;
    private final int uniformCount;
    private final ShaderManager shaderManager;
    private final AbstractUniform mcc$modern;

    public ShaderUniform(String name, int type, int count, ShaderManager manager) {
        this(name, type, count, manager, null);
    }

    ShaderUniform(String name, int type, int count, ShaderManager manager, AbstractUniform modern) {
        this.shaderName = name;
        this.uniformType = type;
        this.uniformCount = count;
        this.shaderManager = manager;
        this.mcc$modern = modern;
    }

    public String getShaderName() {
        return shaderName;
    }

    public void set(float p_148090_1_) {
        if (mcc$modern != null) mcc$modern.set(p_148090_1_);
    }

    public void set(float p_148087_1_, float p_148087_2_) {
        if (mcc$modern != null) mcc$modern.set(p_148087_1_, p_148087_2_);
    }

    public void set(float p_148095_1_, float p_148095_2_, float p_148095_3_) {
        if (mcc$modern != null) mcc$modern.set(p_148095_1_, p_148095_2_, p_148095_3_);
    }

    public void set(float p_148081_1_, float p_148081_2_, float p_148081_3_, float p_148081_4_) {
        if (mcc$modern != null) mcc$modern.set(p_148081_1_, p_148081_2_, p_148081_3_, p_148081_4_);
    }

    public void setSafe(float p_148092_1_, float p_148092_2_, float p_148092_3_, float p_148092_4_) {
        if (mcc$modern != null) mcc$modern.setSafe(p_148092_1_, p_148092_2_, p_148092_3_, p_148092_4_);
    }

    public void set(int p_148083_1_, int p_148083_2_, int p_148083_3_, int p_148083_4_) {
        if (mcc$modern != null) mcc$modern.set(p_148083_1_, p_148083_2_, p_148083_3_, p_148083_4_);
    }

    public void set(float[] p_148097_1_) {
        if (mcc$modern != null && p_148097_1_ != null) mcc$modern.set(p_148097_1_);
    }

    public void set(legacyconnector.v1_12_2.org.lwjgl.util.vector.Matrix4f matrix) {
        if (mcc$modern == null || matrix == null) return;
        mcc$modern.setMat4x4(matrix.m00, matrix.m01, matrix.m02, matrix.m03, matrix.m10, matrix.m11, matrix.m12, matrix.m13, matrix.m20, matrix.m21, matrix.m22,
                matrix.m23, matrix.m30, matrix.m31, matrix.m32, matrix.m33);
    }
}
