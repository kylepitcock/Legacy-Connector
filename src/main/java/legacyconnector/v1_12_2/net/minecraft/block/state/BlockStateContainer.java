package legacyconnector.v1_12_2.net.minecraft.block.state;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;

/** All states of a block: one per combination of its properties' values. */
public class BlockStateContainer {
    private final Block block;
    private final ImmutableList<IProperty<?>> properties;
    private final ImmutableList<IBlockState> validStates;

    public BlockStateContainer(Block block, IProperty<?>... properties) {
        this.block = block;
        List<IProperty<?>> sorted = new ArrayList<>(List.of(properties));
        sorted.sort((a, b) -> a.getName().compareTo(b.getName()));
        this.properties = ImmutableList.copyOf(sorted);
        List<StateImpl> states = new ArrayList<>();
        combine(states, new LinkedHashMapHolder(), 0);
        for (StateImpl s : states) s.link(states);
        this.validStates = ImmutableList.copyOf(states);
    }

    /** Insertion-ordered value assignment being built. */
    private static final class LinkedHashMapHolder {
        final Map<IProperty<?>, Comparable<?>> values = new java.util.LinkedHashMap<>();
    }

    private void combine(List<StateImpl> out, LinkedHashMapHolder current, int index) {
        if (index == properties.size()) {
            out.add(new StateImpl(block, ImmutableMap.copyOf(current.values)));
            return;
        }
        IProperty<?> p = properties.get(index);
        for (Comparable<?> v : p.getAllowedValues()) {
            current.values.put(p, v);
            combine(out, current, index + 1);
        }
        current.values.remove(p);
    }

    public ImmutableList<IBlockState> getValidStates() {
        return validStates;
    }

    public IBlockState getBaseState() {
        return validStates.get(0);
    }

    public Block getBlock() {
        return block;
    }

    public Collection<IProperty<?>> getProperties() {
        return properties;
    }

    public IProperty<?> getProperty(String name) {
        for (IProperty<?> p : properties) if (p.getName().equals(name)) return p;
        return null;
    }

    private static final class StateImpl implements IBlockState {
        private final Block block;
        private final ImmutableMap<IProperty<?>, Comparable<?>> values;
        private final Map<String, StateImpl> neighbours = new HashMap<>();

        StateImpl(Block block, ImmutableMap<IProperty<?>, Comparable<?>> values) {
            this.block = block;
            this.values = values;
        }

        void link(List<StateImpl> all) {
            for (StateImpl other : all) {
                if (other == this) continue;
                // a neighbour differs from this state in exactly one property
                IProperty<?> differing = null;
                int diffs = 0;
                for (var e : values.entrySet()) {
                    if (!e.getValue().equals(other.values.get(e.getKey()))) {
                        differing = e.getKey();
                        diffs++;
                    }
                }
                if (diffs == 1) neighbours.put(differing.getName() + "=" + other.values.get(differing), other);
            }
        }

        @Override
        public Collection<IProperty<?>> getPropertyKeys() {
            return values.keySet();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Comparable<T>> T getValue(IProperty<T> property) {
            Comparable<?> v = values.get(property);
            if (v == null) {
                // a vanilla block carries no properties here: the first allowed value stands in (a state read must not fail loading)
                var allowed = property == null ? null : property.getAllowedValues();
                return allowed == null || allowed.isEmpty() ? null : allowed.iterator().next();
            }
            return (T) v;
        }

        @Override
        public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
            Comparable<?> current = property == null ? null : values.get(property);
            if (current == null) return this; // vanilla blocks carry no properties here: the state stays as it is
            if (current.equals(value)) return this;
            StateImpl next = neighbours.get(property.getName() + "=" + value);
            return next != null ? next : this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
            List<T> allowed = new ArrayList<>(property.getAllowedValues());
            T current = getValue(property);
            return withProperty(property, allowed.get((allowed.indexOf(current) + 1) % allowed.size()));
        }

        @Override
        public ImmutableMap<IProperty<?>, Comparable<?>> getProperties() {
            return values;
        }

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public Material getMaterial() {
            return block.getMaterialLegacy();
        }

        @Override
        public String toString() {
            return block.getRegistryName() + values.toString();
        }
    }
}
