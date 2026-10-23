package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType;
import legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** 1.7.10 seeds: an item that plants its crop block on top of its soil block. */
public class ItemSeeds extends Item implements IPlantable {
    private Block field_150925_a;
    private Block field_150184_b;

    /** Vanilla seeds are the modern items; the crop block is looked up lazily (the blocks do not exist yet when items are created). */
    public ItemSeeds(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemSeeds(Block crops, Block soil) {
        this.field_150925_a = crops;
        this.field_150184_b = soil;
    }

    private Block crops() {
        if (field_150925_a == null && mcc$vanilla != null) {
            field_150925_a = switch (mcc$vanilla.legacyName()) {
                case "minecraft:wheat_seeds" -> Blocks.wheat;
                case "minecraft:pumpkin_seeds" -> Blocks.pumpkin_stem;
                case "minecraft:melon_seeds" -> Blocks.melon_stem;
                case "minecraft:nether_wart" -> Blocks.nether_wart;
                default -> null;
            };
        }
        return field_150925_a;
    }

    private Block soil() {
        if (field_150184_b == null && mcc$vanilla != null) {
            field_150184_b = mcc$vanilla.legacyName().equals("minecraft:nether_wart") ? Blocks.soul_sand : Blocks.farmland;
        }
        return field_150184_b;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block crops = crops();
        if (side != 1 || crops == null) return false;
        if (!player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x, y + 1, z, side, stack)) return false;
        Block ground = world.getBlock(x, y, z);
        boolean soil = ground == soil()
                || ground != null && ground.canSustainPlant(world, x, y, z, ForgeDirection.UP, this);
        if (!soil || !world.isAirBlock(x, y + 1, z)) return false;
        world.setBlock(x, y + 1, z, crops);
        stack.mcc$setCount(stack.mcc$count() - 1);
        return true;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
        return crops() == Blocks.nether_wart ? EnumPlantType.Nether : EnumPlantType.Crop;
    }

    @Override
    public Block getPlant(IBlockAccess world, int x, int y, int z) {
        return crops();
    }

    @Override
    public int getPlantMetadata(IBlockAccess world, int x, int y, int z) {
        return 0;
    }
}
