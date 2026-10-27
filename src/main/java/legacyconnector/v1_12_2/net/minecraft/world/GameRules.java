package legacyconnector.v1_12_2.net.minecraft.world;

/**
 * 1.12.2 game rules by name, over the modern rules of a level (the same names: doDaylightCycle, keepInventory, ...). Rules
 * a mod adds that the modern game lacks are kept here.
 */
public class GameRules {
    private final net.minecraft.world.level.GameRules modern;
    private final java.util.Map<String, String> extra = new java.util.HashMap<>();

    public GameRules() {
        this(null);
    }

    public GameRules(net.minecraft.world.level.GameRules modern) {
        this.modern = modern;
    }

    private net.minecraft.world.level.GameRules.Value<?> mcc$rule(String name) {
        if (modern == null || name == null) return null;
        net.minecraft.world.level.GameRules.Value<?>[] found = {null};
        net.minecraft.world.level.GameRules.visitGameRuleTypes(new net.minecraft.world.level.GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends net.minecraft.world.level.GameRules.Value<T>> void visit(net.minecraft.world.level.GameRules.Key<T> key, net.minecraft.world.level.GameRules.Type<T> type) {
                if (found[0] == null && key.getId().equals(name)) found[0] = modern.getRule(key);
            }
        });
        return found[0];
    }

    public void addGameRule(String name, String value, ValueType type) {
        if (mcc$rule(name) == null) extra.put(name, value);
    }

    public void setOrCreateGameRule(String name, String value) {
        var rule = mcc$rule(name);
        if (rule instanceof net.minecraft.world.level.GameRules.BooleanValue b) b.set(Boolean.parseBoolean(value), null);
        else if (rule instanceof net.minecraft.world.level.GameRules.IntegerValue i) {
            try {
                i.set(Integer.parseInt(value), null);
            } catch (NumberFormatException e) {
                // 1.12.2 kept an unparsable value as a string; the modern rule keeps its value
            }
        } else extra.put(name, value);
    }

    public String getString(String name) {
        var rule = mcc$rule(name);
        return rule != null ? rule.serialize() : extra.getOrDefault(name, "");
    }

    public boolean getBoolean(String name) {
        var rule = mcc$rule(name);
        if (rule instanceof net.minecraft.world.level.GameRules.BooleanValue b) return b.get();
        return Boolean.parseBoolean(getString(name));
    }

    public int getInt(String name) {
        var rule = mcc$rule(name);
        if (rule instanceof net.minecraft.world.level.GameRules.IntegerValue i) return i.get();
        try {
            return Integer.parseInt(getString(name));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean hasRule(String name) {
        return mcc$rule(name) != null || extra.containsKey(name);
    }

    public boolean areSameType(String key, ValueType type) {
        var rule = mcc$rule(key);
        if (rule instanceof net.minecraft.world.level.GameRules.BooleanValue) return type == ValueType.BOOLEAN_VALUE || type == ValueType.ANY_VALUE;
        if (rule instanceof net.minecraft.world.level.GameRules.IntegerValue) return type == ValueType.NUMERICAL_VALUE || type == ValueType.ANY_VALUE;
        return type == ValueType.ANY_VALUE;
    }

    public enum ValueType {
        ANY_VALUE, BOOLEAN_VALUE, NUMERICAL_VALUE
    }
}
