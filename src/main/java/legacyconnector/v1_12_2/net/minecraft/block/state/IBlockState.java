package legacyconnector.v1_12_2.net.minecraft.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;

/** A block with one value for each of its properties. */
public interface IBlockState {
    Collection<IProperty<?>> getPropertyKeys();

    <T extends Comparable<T>> T getValue(IProperty<T> property);

    <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value);

    <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property);

    ImmutableMap<IProperty<?>, Comparable<?>> getProperties();

    Block getBlock();

    Material getMaterial();

    // The block's (deprecated) per-state methods, as vanilla's StateImplementation forwards them. isSideSolid and getBlockFaceShape name
    // synthesized types; they are answered by SynthMethods1122.

    default int getLightValue() {
        return getBlock().getLightValue(this);
    }

    default boolean isOpaqueCube() {
        return getBlock().isOpaqueCube(this);
    }

    default boolean isFullBlock() {
        return getBlock().isFullBlock(this);
    }

    default boolean isFullCube() {
        return getBlock().isFullCube(this);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType() {
        return getBlock().getRenderType(this);
    }

    default boolean isNormalCube() {
        Material m = getMaterial();
        return m != null && m.isOpaque() && isFullCube();
    }
}
