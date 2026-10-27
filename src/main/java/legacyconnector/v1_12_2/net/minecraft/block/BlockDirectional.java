package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;

/** A block facing any of the six sides (its subclasses add FACING to their state). */
public abstract class BlockDirectional extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    protected BlockDirectional(Material material) {
        super(material);
    }
}
