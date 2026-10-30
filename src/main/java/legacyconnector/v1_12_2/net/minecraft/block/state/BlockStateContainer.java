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
        List<StateImplementation> states = new ArrayList<>();
        combine(states, new LinkedHashMapHolder(), 0);
        Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> byValues = new java.util.LinkedHashMap<>();
        for (StateImplementation s : states) byValues.put(s.getProperties(), s);
        for (StateImplementation s : states) {
            s.link(states);
            s.buildPropertyValueTable(byValues);
        }
        this.validStates = ImmutableList.copyOf(states);
    }

    /** Forge's builder: listed properties, plus unlisted ones (which make it an {@code ExtendedBlockState}). */
    public static class Builder {
        private final Block block;
        private final List<IProperty<?>> listed = new ArrayList<>();
        private final List<legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<?>> unlisted = new ArrayList<>();

        public Builder(Block block) {
            this.block = block;
        }

        public Builder add(IProperty<?>... props) {
            for (IProperty<?> p : props) if (p != null) listed.add(p);
            return this;
        }

        public Builder add(legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<?>... props) {
            for (var p : props) if (p != null) unlisted.add(p);
            return this;
        }

        public BlockStateContainer build() {
            IProperty<?>[] l = listed.toArray(new IProperty<?>[0]);
            if (unlisted.isEmpty()) return new BlockStateContainer(block, l);
            return new legacyconnector.v1_12_2.net.minecraftforge.common.property.ExtendedBlockState(block, l,
                    unlisted.toArray(new legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<?>[0]));
        }
    }

    /** Subclasses return their own state class (it must extend {@link StateImplementation}). */
    protected StateImplementation createState(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties) {
        return new StateImplementation(block, properties);
    }

    /** Forge's form with the unlisted properties' values (null for a plain container); mods override it to make their own states. */
    protected StateImplementation createState(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties,
            ImmutableMap<legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty<?>, com.google.common.base.Optional<?>> unlistedProperties) {
        return createState(block, properties);
    }

    /** Insertion-ordered value assignment being built. */
    private static final class LinkedHashMapHolder {
        final Map<IProperty<?>, Comparable<?>> values = new java.util.LinkedHashMap<>();
    }

    private void combine(List<StateImplementation> out, LinkedHashMapHolder current, int index) {
        if (index == properties.size()) {
            out.add(createState(block, ImmutableMap.copyOf(current.values), null));
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

    public static class StateImplementation extends BlockStateBase {
        private final Block block;
        private final ImmutableMap<IProperty<?>, Comparable<?>> values;
        private final Map<String, StateImplementation> neighbours = new HashMap<>();

        protected StateImplementation(Block block, ImmutableMap<IProperty<?>, Comparable<?>> values) {
            this.block = block;
            this.values = values;
        }

        /** The state for each other value of each property (vanilla's neighbour table; mods' state classes read it). */
        protected com.google.common.collect.ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> propertyValueTable;

        /** Builds {@link #propertyValueTable} from all states of the container by their property values. */
        public void buildPropertyValueTable(Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> map) {
            com.google.common.collect.Table<IProperty<?>, Comparable<?>, IBlockState> table = com.google.common.collect.HashBasedTable.create();
            for (var entry : values.entrySet()) {
                IProperty<?> property = entry.getKey();
                for (Comparable<?> value : property.getAllowedValues()) {
                    if (value.equals(entry.getValue())) continue;
                    Map<IProperty<?>, Comparable<?>> wanted = new HashMap<>(values);
                    wanted.put(property, value);
                    StateImplementation other = map.get(wanted);
                    if (other != null) table.put(property, value, other);
                }
            }
            this.propertyValueTable = com.google.common.collect.ImmutableTable.copyOf(table);
        }

        void link(List<StateImplementation> all) {
            for (StateImplementation other : all) {
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
            StateImplementation next = neighbours.get(property.getName() + "=" + value);
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
            Material m = block.getMaterialLegacy();
            return m != null ? m : Material.ROCK;
        }

        @Override
        public String toString() {
            return block.getRegistryName() + values.toString();
        }
    }
}
