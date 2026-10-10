package mccompat.v1_7_10.net.minecraft.client.renderer;

import mccompat.v1_7_10.net.minecraft.world.IBlockAccess;

/** World block rendering is done by modern baked models; this exists so legacy renderers can construct one. */
public class RenderBlocks {
    public IBlockAccess blockAccess;
    public boolean renderAllFaces;

    public RenderBlocks() {
    }

    public RenderBlocks(IBlockAccess access) {
        this.blockAccess = access;
    }
}
