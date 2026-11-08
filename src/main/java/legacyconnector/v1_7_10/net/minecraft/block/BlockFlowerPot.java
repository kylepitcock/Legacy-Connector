package legacyconnector.v1_7_10.net.minecraft.block;

/** The flower pot; RenderBlocks draws the plant its tile entity holds inside it (render type 33). */
public class BlockFlowerPot extends BlockContainer {
    public BlockFlowerPot(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockFlowerPot() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.circuits);
        setBlockBoundsForItemRender();
    }

    private static legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack(legacyconnector.v1_7_10.net.minecraft.block.Block block, int damage) {
        return new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(block, 1, damage);
    }

    /** 1.4.7/1.5 Forge: the plant a pot of the given metadata (1 to 11) holds; null for an empty pot. */
    public static legacyconnector.v1_7_10.net.minecraft.item.ItemStack getPlantForMeta(int meta) {
        switch (meta) {
            case 1: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_flower, 0);
            case 2: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.yellow_flower, 0);
            case 3: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.sapling, 0);
            case 4: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.sapling, 1);
            case 5: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.sapling, 2);
            case 6: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.sapling, 3);
            case 7: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.red_mushroom, 0);
            case 8: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.brown_mushroom, 0);
            case 9: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.cactus, 0);
            case 10: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.deadbush, 0);
            case 11: return stack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.tallgrass, 2);
            default: return null;
        }
    }

    /** The inverse of {@link #getPlantForMeta}: the pot metadata for a plant stack, 0 when it cannot be potted. */
    public static int getMetaForPlant(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        for (int meta = 1; meta <= 11; meta++) {
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack plant = getPlantForMeta(meta);
            if (plant != null && plant.getItem() == stack.getItem() && plant.getItemDamage() == stack.getItemDamage()) return meta;
        }
        return 0;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        float h = 0.375F;
        float r = h / 2.0F;
        setBlockBounds(0.5F - r, 0.0F, 0.5F - r, 0.5F + r, h, 0.5F + r);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 33;
    }

    @Override
    public boolean isFlowerPot() {
        return true;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity createNewTileEntity(legacyconnector.v1_7_10.net.minecraft.world.World world, int meta) {
        return new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFlowerPot(null, 0);
    }
}
