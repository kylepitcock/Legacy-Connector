package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A ladder: climbable, attached to the side of a block (metadata 2-5). */
public class BlockLadder extends Block {
    public BlockLadder(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockLadder() {
        super(Material.circuits);
    }

    @Override
    public boolean isLadder(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        return true;
    }
}
