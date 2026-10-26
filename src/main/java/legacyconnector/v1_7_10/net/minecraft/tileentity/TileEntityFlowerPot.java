package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * A flower pot's plant (item and metadata), saved as "Item" (id) and "Data". A modern flower pot is a block per plant and has
 * no block entity, so for a vanilla pot this is a view: the plant is read from the potted block, and {@link #func_145964_a}
 * swaps the pot block for the one holding the new plant.
 */
public class TileEntityFlowerPot extends TileEntity {
    private Item flowerPotItem;
    private int flowerPotData;
    /** The modern flower pot this views; null for a standalone tile entity. */
    private final net.minecraft.world.level.Level mcc$level;
    private final net.minecraft.core.BlockPos mcc$pos;

    public TileEntityFlowerPot() {
        this.mcc$level = null;
        this.mcc$pos = null;
    }

    public TileEntityFlowerPot(Item item, int data) {
        this();
        this.flowerPotItem = item;
        this.flowerPotData = data;
    }

    /** View of the vanilla flower pot at a position. */
    public TileEntityFlowerPot(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        this.mcc$level = level;
        this.mcc$pos = pos.immutable();
        if (level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock pot
                && pot.getPotted() != net.minecraft.world.level.block.Blocks.AIR) {
            var plant = pot.getPotted().defaultBlockState();
            var block = org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyBlock(plant);
            this.flowerPotItem = block == null ? null : Item.getItemFromBlock(block);
            this.flowerPotData = org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyMeta(plant);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Item", flowerPotItem == null ? 0 : Item.getIdFromItem(flowerPotItem));
        tag.setInteger("Data", flowerPotData);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.flowerPotItem = Item.getItemById(tag.getInteger("Item"));
        this.flowerPotData = tag.getInteger("Data");
    }

    /** Puts a plant (item and metadata; null for none) into the pot. */
    public void func_145964_a(Item item, int data) {
        this.flowerPotItem = item;
        this.flowerPotData = data;
        if (mcc$level != null) mcc$plant(item, data);
    }

    /** Replaces the modern pot by the pot holding the plant's block (the empty pot when the plant cannot be potted). */
    private void mcc$plant(Item item, int data) {
        if (!(mcc$level.getBlockState(mcc$pos).getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock current)) return;
        net.minecraft.world.level.block.FlowerPotBlock empty = current.getEmptyPot();
        net.minecraft.world.level.block.Block target = empty;
        var legacyBlock = item == null ? null : legacyconnector.v1_7_10.net.minecraft.block.Block.getBlockFromItem(item);
        if (legacyBlock != null) {
            var plant = org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.toModern(legacyBlock, data);
            if (plant != null) {
                var full = empty.getFullPotsView().get(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(plant.getBlock()));
                if (full != null && full.get() != net.minecraft.world.level.block.Blocks.AIR) target = full.get();
            }
        }
        if (target != current) mcc$level.setBlock(mcc$pos, target.defaultBlockState(), 3);
    }

    public Item getFlowerPotItem() {
        return flowerPotItem;
    }

    public int getFlowerPotData() {
        return flowerPotData;
    }
}
