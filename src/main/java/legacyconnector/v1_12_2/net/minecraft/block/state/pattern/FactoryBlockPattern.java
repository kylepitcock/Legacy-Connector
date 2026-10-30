package legacyconnector.v1_12_2.net.minecraft.block.state.pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockWorldState;

/** Builds a {@link BlockPattern} from aisles of strings and a predicate per character (a space matches anything). */
public class FactoryBlockPattern {
    private static final Joiner COMMA_JOIN = Joiner.on(",");
    private final List<String[]> depth = new ArrayList<>();
    private final Map<Character, Predicate<BlockWorldState>> symbolMap = new HashMap<>();
    private int aisleHeight;
    private int rowWidth;

    private FactoryBlockPattern() {
        symbolMap.put(' ', Predicates.alwaysTrue());
    }

    public FactoryBlockPattern aisle(String... aisle) {
        if (aisle.length == 0 || aisle[0].isEmpty()) throw new IllegalArgumentException("Empty pattern for aisle");
        if (depth.isEmpty()) {
            aisleHeight = aisle.length;
            rowWidth = aisle[0].length();
        }
        if (aisle.length != aisleHeight) {
            throw new IllegalArgumentException("Expected aisle with height of " + aisleHeight + ", but was given one with a height of " + aisle.length + ")");
        }
        for (String row : aisle) {
            if (row.length() != rowWidth) {
                throw new IllegalArgumentException("Not all rows in the given aisle are the correct width (expected " + rowWidth + ", found one with " + row.length() + ")");
            }
            for (char c : row.toCharArray()) symbolMap.putIfAbsent(c, null);
        }
        depth.add(aisle);
        return this;
    }

    public static FactoryBlockPattern start() {
        return new FactoryBlockPattern();
    }

    public FactoryBlockPattern where(char symbol, Predicate<BlockWorldState> blockMatcher) {
        symbolMap.put(symbol, blockMatcher);
        return this;
    }

    public BlockPattern build() {
        return new BlockPattern(makePredicateArray());
    }

    @SuppressWarnings("unchecked")
    private Predicate<BlockWorldState>[][][] makePredicateArray() {
        checkMissingPredicates();
        Predicate<BlockWorldState>[][][] out = (Predicate<BlockWorldState>[][][]) Array.newInstance(Predicate.class, depth.size(), aisleHeight, rowWidth);
        for (int i = 0; i < depth.size(); ++i) {
            for (int j = 0; j < aisleHeight; ++j) {
                for (int k = 0; k < rowWidth; ++k) out[i][j][k] = symbolMap.get(depth.get(i)[j].charAt(k));
            }
        }
        return out;
    }

    private void checkMissingPredicates() {
        List<Character> missing = new ArrayList<>();
        for (Map.Entry<Character, Predicate<BlockWorldState>> e : symbolMap.entrySet()) if (e.getValue() == null) missing.add(e.getKey());
        if (!missing.isEmpty()) throw new IllegalStateException("Predicates for character(s) " + COMMA_JOIN.join(missing) + " are missing");
    }
}
