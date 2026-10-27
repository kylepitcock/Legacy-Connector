package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** A block facing one of the four horizontal sides (its subclasses add FACING to their state). */
public abstract class BlockHorizontal extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    protected BlockHorizontal(Material material) {
        super(material);
    }

    protected BlockHorizontal(Material material, MapColor color) {
        super(material, color);
    }
}
