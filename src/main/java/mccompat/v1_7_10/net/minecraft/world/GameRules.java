package mccompat.v1_7_10.net.minecraft.world;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 game rules: string-keyed values. Rules vanilla still has ({@code doDaylightCycle}, {@code keepInventory},
 * ...) read and write the level's real rules; rules mods create ({@code addGameRule}) live in a per-level table
 * (not saved with the world).
 */
public class GameRules {
    private static final Map<String, Key<BooleanValue>> BOOLEANS = new ConcurrentHashMap<>();
    private static final Map<String, Key<IntegerValue>> INTEGERS = new ConcurrentHashMap<>();
    private static final Map<Level, GameRules> BY_LEVEL = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static {
        net.minecraft.world.level.GameRules.visitGameRuleTypes(new net.minecraft.world.level.GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(Key<BooleanValue> key, net.minecraft.world.level.GameRules.Type<BooleanValue> type) {
                BOOLEANS.put(key.getId(), key);
            }

            @Override
            public void visitInteger(Key<IntegerValue> key, net.minecraft.world.level.GameRules.Type<IntegerValue> type) {
                INTEGERS.put(key.getId(), key);
            }
        });
    }

    private final Level level;
    private final Map<String, String> custom = new TreeMap<>();

    public GameRules() {
        this(null);
    }

    private GameRules(Level level) {
        this.level = level;
    }

    /** The rules of a level (one instance per level so mod-created rules persist across calls). */
    public static GameRules mcc$of(Level level) {
        if (level == null) return new GameRules(null);
        return BY_LEVEL.computeIfAbsent(level, GameRules::new);
    }

    public void addGameRule(String name, String value) {
        custom.put(name, value);
    }

    public void setOrCreateGameRule(String name, String value) {
        Key<BooleanValue> b = BOOLEANS.get(name);
        Key<IntegerValue> i = INTEGERS.get(name);
        if (level != null && level.getServer() != null && b != null) {
            level.getGameRules().getRule(b).set(Boolean.parseBoolean(value), level.getServer());
        } else if (level != null && level.getServer() != null && i != null) {
            try {
                level.getGameRules().getRule(i).set(Integer.parseInt(value.trim()), level.getServer());
            } catch (NumberFormatException ignored) {
                // as vanilla: an unparsable value leaves the rule alone
            }
        } else {
            custom.put(name, value);
        }
    }

    public String getGameRuleStringValue(String name) {
        Key<BooleanValue> b = BOOLEANS.get(name);
        Key<IntegerValue> i = INTEGERS.get(name);
        if (level != null && b != null) return Boolean.toString(level.getGameRules().getBoolean(b));
        if (level != null && i != null) return Integer.toString(level.getGameRules().getInt(i));
        String v = custom.get(name);
        return v == null ? "" : v;
    }

    public boolean getGameRuleBooleanValue(String name) {
        Key<BooleanValue> b = BOOLEANS.get(name);
        if (level != null && b != null) return level.getGameRules().getBoolean(b);
        return Boolean.parseBoolean(custom.get(name));
    }

    public boolean hasRule(String name) {
        return BOOLEANS.containsKey(name) || INTEGERS.containsKey(name) || custom.containsKey(name);
    }

    public String[] getRules() {
        java.util.Set<String> names = new java.util.TreeSet<>(custom.keySet());
        names.addAll(BOOLEANS.keySet());
        names.addAll(INTEGERS.keySet());
        return names.toArray(String[]::new);
    }
}
