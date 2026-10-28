package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** 1.12.2's bed tile entity: the bed's colour. A bed of the game is seen through a view reading its colour and part. */
public class TileEntityBed extends TileEntity implements org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private EnumDyeColor color = EnumDyeColor.RED;
    private final net.minecraft.world.level.block.entity.BedBlockEntity mcc$bed;

    public TileEntityBed() {
        this.mcc$bed = null;
    }

    /** A view of a game bed. */
    public TileEntityBed(net.minecraft.world.level.block.entity.BedBlockEntity bed) {
        this.mcc$bed = bed;
        mcc$refresh();
    }

    @Override
    public void mcc$refresh() {
        if (mcc$bed != null) color = EnumDyeColor.byMetadata(mcc$bed.getColor().getId());
    }

    /** The colour of the bed item placed (its metadata). */
    public void setItemValues(ItemStack stack) {
        var backing = stack.mcc$backing();
        if (backing == null && !stack.isEmpty()) backing = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        if (backing != null && backing.getItem() instanceof net.minecraft.world.item.BedItem b
                && b.getBlock() instanceof net.minecraft.world.level.block.BedBlock bed) {
            setColor(EnumDyeColor.byMetadata(bed.getColor().getId()));
        } else {
            setColor(EnumDyeColor.byMetadata(stack.getMetadata()));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("color")) color = EnumDyeColor.byMetadata(compound.getInteger("color"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("color", color.getMetadata());
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public EnumDyeColor getColor() {
        return color;
    }

    public void setColor(EnumDyeColor colorIn) {
        this.color = colorIn;
        markDirty();
    }

    /** Whether this is the head half of the bed. */
    public boolean isHeadPiece() {
        if (mcc$bed != null) {
            var state = mcc$bed.getBlockState();
            return state.hasProperty(net.minecraft.world.level.block.BedBlock.PART)
                    && state.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD;
        }
        return (getBlockMetadata() & 8) != 0;
    }

    /** The bed item of this colour. */
    public ItemStack getItemStack() {
        var modern = net.minecraft.world.level.block.BedBlock.class;
        var item = switch (color.getMetadata()) {
            case 0 -> net.minecraft.world.item.Items.WHITE_BED;
            case 1 -> net.minecraft.world.item.Items.ORANGE_BED;
            case 2 -> net.minecraft.world.item.Items.MAGENTA_BED;
            case 3 -> net.minecraft.world.item.Items.LIGHT_BLUE_BED;
            case 4 -> net.minecraft.world.item.Items.YELLOW_BED;
            case 5 -> net.minecraft.world.item.Items.LIME_BED;
            case 6 -> net.minecraft.world.item.Items.PINK_BED;
            case 7 -> net.minecraft.world.item.Items.GRAY_BED;
            case 8 -> net.minecraft.world.item.Items.LIGHT_GRAY_BED;
            case 9 -> net.minecraft.world.item.Items.CYAN_BED;
            case 10 -> net.minecraft.world.item.Items.PURPLE_BED;
            case 11 -> net.minecraft.world.item.Items.BLUE_BED;
            case 12 -> net.minecraft.world.item.Items.BROWN_BED;
            case 13 -> net.minecraft.world.item.Items.GREEN_BED;
            case 14 -> net.minecraft.world.item.Items.RED_BED;
            default -> net.minecraft.world.item.Items.BLACK_BED;
        };
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(new net.minecraft.world.item.ItemStack(item));
    }
}
