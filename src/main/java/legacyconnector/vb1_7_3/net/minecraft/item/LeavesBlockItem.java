package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/**
 * Beta 1.7.3 {@code ItemLeaves}: the item of a leaves block, one subtype per leaf kind; placed leaves get the no-decay bit (8), and the
 * icon is the vanilla leaves texture tinted with that kind's foliage colour. Mods pass it to {@code ModLoader.RegisterBlock} for their own
 * leaves.
 */
public class LeavesBlockItem extends BlockItem {
    public LeavesBlockItem(int id) {
        super(id);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getTextureId(int meta) {
        Block leaves = Block.BLOCKS[18];
        return leaves == null ? super.getTextureId(meta) : leaves.getTexture(0, meta);
    }

    @Override
    public int getColorMultiplier(int meta) {
        if ((meta & 1) == 1) return 0x619961; // spruce
        if ((meta & 2) == 2) return 0x80A755; // birch
        return 0x48B518;
    }

    @Override
    public int getPlacementMetadata(int meta) {
        return meta | 8;
    }
}
