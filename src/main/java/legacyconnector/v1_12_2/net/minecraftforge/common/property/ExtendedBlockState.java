package legacyconnector.v1_12_2.net.minecraftforge.common.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** A state container whose states also carry unlisted properties. */
public class ExtendedBlockState extends BlockStateContainer {
    private final ImmutableList<IUnlistedProperty<?>> unlistedProperties;

    public ExtendedBlockState(Block block, IProperty<?>[] properties, IUnlistedProperty<?>[] unlistedProperties) {
        super(block, properties);
        this.unlistedProperties = ImmutableList.copyOf(unlistedProperties);
    }

    public Collection<IUnlistedProperty<?>> getUnlistedProperties() {
        return unlistedProperties;
    }

    @Override
    public IBlockState getBaseState() {
        return new Extended(super.getBaseState(), new LinkedHashMap<>());
    }

    private final class Extended implements IExtendedBlockState {
        private final IBlockState clean;
        private final Map<IUnlistedProperty<?>, Object> unlisted;

        Extended(IBlockState clean, Map<IUnlistedProperty<?>, Object> unlisted) {
            this.clean = clean;
            this.unlisted = unlisted;
        }

        @Override
        public Collection<IProperty<?>> getPropertyKeys() {
            return clean.getPropertyKeys();
        }

        @Override
        public <T extends Comparable<T>> T getValue(IProperty<T> property) {
            return clean.getValue(property);
        }

        @Override
        public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
            return new Extended(clean.withProperty(property, value), unlisted);
        }

        @Override
        public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
            return new Extended(clean.cycleProperty(property), unlisted);
        }

        @Override
        public ImmutableMap<IProperty<?>, Comparable<?>> getProperties() {
            return clean.getProperties();
        }

        @Override
        public Block getBlock() {
            return clean.getBlock();
        }

        @Override
        public Material getMaterial() {
            return clean.getMaterial();
        }

        @Override
        public Collection<IUnlistedProperty<?>> getUnlistedNames() {
            return unlistedProperties;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <V> V getValue(IUnlistedProperty<V> property) {
            return (V) unlisted.get(property);
        }

        @Override
        public <V> IExtendedBlockState withProperty(IUnlistedProperty<V> property, V value) {
            Map<IUnlistedProperty<?>, Object> next = new LinkedHashMap<>(unlisted);
            next.put(property, value);
            return new Extended(clean, next);
        }

        @Override
        public ImmutableMap<IUnlistedProperty<?>, com.google.common.base.Optional<?>> getUnlistedProperties() {
            ImmutableMap.Builder<IUnlistedProperty<?>, com.google.common.base.Optional<?>> b = ImmutableMap.builder();
            for (IUnlistedProperty<?> p : unlistedProperties) b.put(p, com.google.common.base.Optional.fromNullable(unlisted.get(p)));
            return b.build();
        }

        @Override
        public IBlockState getClean() {
            return clean;
        }

        @Override
        public String toString() {
            return clean + unlisted.toString();
        }
    }
}
