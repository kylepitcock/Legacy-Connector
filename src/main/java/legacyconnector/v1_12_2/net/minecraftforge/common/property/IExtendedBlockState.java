package legacyconnector.v1_12_2.net.minecraftforge.common.property;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

public interface IExtendedBlockState extends IBlockState {
    Collection<IUnlistedProperty<?>> getUnlistedNames();

    <V> V getValue(IUnlistedProperty<V> property);

    <V> IExtendedBlockState withProperty(IUnlistedProperty<V> property, V value);

    ImmutableMap<IUnlistedProperty<?>, com.google.common.base.Optional<?>> getUnlistedProperties();

    IBlockState getClean();
}
