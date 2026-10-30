package legacyconnector.v1_12_2.net.minecraft.block.state.pattern;

import com.google.common.base.Predicate;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** Matches the states of one block whose properties pass their tests (a pumpkin facing any way, a skull of a kind). */
public class BlockStateMatcher implements Predicate<IBlockState> {
    public static final Predicate<IBlockState> ANY = input -> true;
    private final Block block;
    private final Map<IProperty<?>, Predicate<?>> propertyPredicates = new HashMap<>();

    private BlockStateMatcher(Block blockIn) {
        this.block = blockIn;
    }

    public static BlockStateMatcher forBlock(Block blockIn) {
        return new BlockStateMatcher(blockIn);
    }

    @Override
    public boolean apply(IBlockState state) {
        if (state == null || !state.getBlock().equals(block)) return false;
        if (propertyPredicates.isEmpty()) return true;
        for (Map.Entry<IProperty<?>, Predicate<?>> e : propertyPredicates.entrySet()) {
            if (!matches(state, e.getKey(), e.getValue())) return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T extends Comparable<T>> boolean matches(IBlockState state, IProperty<T> property, Predicate<?> predicate) {
        return ((Predicate) predicate).apply(state.getValue(property));
    }

    public <V extends Comparable<V>> BlockStateMatcher where(IProperty<V> property, Predicate<? extends V> is) {
        if (!block.getBlockState().getProperties().contains(property)) throw new IllegalArgumentException(block + " cannot support property " + property);
        propertyPredicates.put(property, is);
        return this;
    }
}
