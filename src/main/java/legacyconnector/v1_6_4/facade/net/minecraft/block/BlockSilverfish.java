package legacyconnector.v1_6_4.facade.net.minecraft.block;

/** Facade of the 1.7.10 shim's {@code BlockSilverfish} for 1.6.4 mods: the block-id forms of its static helpers. Never instantiated. */
public final class BlockSilverfish {
    private BlockSilverfish() {}

    /** 1.6.4 static {@code getPosingIdByMetadata(blockId)}: whether the block of the id can hide a silverfish (stone, cobblestone, stone bricks). */
    public static boolean getPosingIdByMetadata(int blockId) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = Block.mcc$fromId(blockId);
        return block != null && legacyconnector.v1_7_10.net.minecraft.block.BlockSilverfish.func_150196_a(block);
    }

    /** 1.6.4 static {@code getMetadataForBlockType(blockId)}: the silverfish block metadata that imitates the block (1 cobblestone, 2 stone bricks, else 0). */
    public static int getMetadataForBlockType(int blockId) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = Block.mcc$fromId(blockId);
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.cobblestone) return 1;
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stonebrick) return 2;
        return 0;
    }
}
