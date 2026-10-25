package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType;
import legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable;

/** A food that is also a seed (carrots, potatoes): planted on the soil it grows the crop block. */
public class ItemSeedFood extends ItemFood implements IPlantable {
    private final Block crop;
    private final Block soil;

    public ItemSeedFood(int heal, float saturation, Block crop, Block soil) {
        super(heal, saturation, false);
        this.crop = crop;
        this.soil = soil;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
        return EnumPlantType.Crop;
    }

    @Override
    public Block getPlant(IBlockAccess world, int x, int y, int z) {
        return crop;
    }

    @Override
    public int getPlantMetadata(IBlockAccess world, int x, int y, int z) {
        return 0;
    }
}
