package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.Map;
import java.util.TreeMap;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * 1.12.2 game rules: the vanilla rules (same names in both versions) read and set the modern level's rules; rules mods add are
 * kept here as the strings 1.12 stored. A GameRules made by a mod itself (no level behind it) keeps everything as strings.
 */
public class GameRules {
    private static volatile Map<String, net.minecraft.world.level.GameRules.Key<?>> MODERN_KEYS;

    private final net.minecraft.world.level.GameRules modern;
    private final net.minecraft.server.MinecraftServer server;
    private final Map<String, Value> rules = new TreeMap<>();

    public GameRules() {
        this(null, null);
        for (String[] r : new String[][] {{"doFireTick", "true"}, {"mobGriefing", "true"}, {"keepInventory", "false"}, {"doMobSpawning", "true"},
                {"doMobLoot", "true"}, {"doTileDrops", "true"}, {"doEntityDrops", "true"}, {"commandBlockOutput", "true"},
                {"naturalRegeneration", "true"}, {"doDaylightCycle", "true"}, {"logAdminCommands", "true"}, {"showDeathMessages", "true"},
                {"randomTickSpeed", "3"}, {"sendCommandFeedback", "true"}, {"reducedDebugInfo", "false"}, {"spectatorsGenerateChunks", "true"},
                {"spawnRadius", "10"}, {"disableElytraMovementCheck", "false"}, {"maxEntityCramming", "24"}, {"doWeatherCycle", "true"},
                {"doLimitedCrafting", "false"}, {"maxCommandChainLength", "65536"}, {"announceAdvancements", "true"}}) {
            addGameRule(r[0], r[1], r[1].equals("true") || r[1].equals("false") ? ValueType.BOOLEAN_VALUE : ValueType.NUMERICAL_VALUE);
        }
    }

    private GameRules(net.minecraft.world.level.GameRules modern, net.minecraft.server.MinecraftServer server) {
        this.modern = modern;
        this.server = server;
    }

    private static final Map<net.minecraft.world.level.GameRules, GameRules> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** One view per modern rule set, so rules mods add stay with it. */
    public static GameRules mcc$of(net.minecraft.world.level.GameRules modern, net.minecraft.server.MinecraftServer server) {
        if (modern == null) return new GameRules();
        return VIEWS.computeIfAbsent(modern, m -> new GameRules(m, server));
    }

    private static Map<String, net.minecraft.world.level.GameRules.Key<?>> modernKeys() {
        Map<String, net.minecraft.world.level.GameRules.Key<?>> keys = MODERN_KEYS;
        if (keys != null) return keys;
        Map<String, net.minecraft.world.level.GameRules.Key<?>> found = new java.util.HashMap<>();
        net.minecraft.world.level.GameRules.visitGameRuleTypes(new net.minecraft.world.level.GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends net.minecraft.world.level.GameRules.Value<T>> void visit(net.minecraft.world.level.GameRules.Key<T> key,
                    net.minecraft.world.level.GameRules.Type<T> type) {
                found.put(key.getId(), key);
            }
        });
        found.put("doTileDrops", net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        MODERN_KEYS = keys = found;
        return keys;
    }

    private net.minecraft.world.level.GameRules.Value<?> modernRule(String name) {
        if (modern == null) return null;
        var key = modernKeys().get(name);
        return key == null ? null : modern.getRule(key);
    }

    public void addGameRule(String key, String value, ValueType type) {
        if (modernRule(key) != null) return;
        synchronized (rules) {
            rules.put(key, new Value(value, type));
        }
    }

    public void setOrCreateGameRule(String key, String value) {
        var rule = modernRule(key);
        if (rule instanceof net.minecraft.world.level.GameRules.BooleanValue b) {
            b.set(Boolean.parseBoolean(value), server);
            return;
        }
        if (rule instanceof net.minecraft.world.level.GameRules.IntegerValue i) {
            try {
                i.set(Integer.parseInt(value.trim()), server);
            } catch (NumberFormatException e) {
                // 1.12 kept the old number for an unparsable one
            }
            return;
        }
        synchronized (rules) {
            Value v = rules.get(key);
            if (v != null) v.setValue(value);
            else rules.put(key, new Value(value, ValueType.ANY_VALUE));
        }
    }

    public String getString(String name) {
        var rule = modernRule(name);
        if (rule != null) return rule.serialize();
        synchronized (rules) {
            Value v = rules.get(name);
            return v == null ? "" : v.valueString;
        }
    }

    public boolean getBoolean(String name) {
        var rule = modernRule(name);
        if (rule instanceof net.minecraft.world.level.GameRules.BooleanValue b) return b.get();
        synchronized (rules) {
            Value v = rules.get(name);
            return v != null && v.valueBoolean;
        }
    }

    public int getInt(String name) {
        var rule = modernRule(name);
        if (rule instanceof net.minecraft.world.level.GameRules.IntegerValue i) return i.get();
        synchronized (rules) {
            Value v = rules.get(name);
            return v == null ? 0 : v.valueInteger;
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (String name : getRules()) tag.setString(name, getString(name));
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        for (String key : tag.getKeySet()) setOrCreateGameRule(key, tag.getString(key));
    }

    public String[] getRules() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        if (modern != null) names.addAll(modernKeys().keySet());
        synchronized (rules) {
            names.addAll(rules.keySet());
        }
        return names.toArray(new String[0]);
    }

    public boolean hasRule(String name) {
        if (modernRule(name) != null) return true;
        synchronized (rules) {
            return rules.containsKey(name);
        }
    }

    public boolean areSameType(String key, ValueType type) {
        var rule = modernRule(key);
        if (rule != null) {
            return type == ValueType.ANY_VALUE || (rule instanceof net.minecraft.world.level.GameRules.BooleanValue ? type == ValueType.BOOLEAN_VALUE
                    : type == ValueType.NUMERICAL_VALUE);
        }
        synchronized (rules) {
            Value v = rules.get(key);
            return v != null && (v.type == type || type == ValueType.ANY_VALUE);
        }
    }

    static class Value {
        private String valueString;
        private boolean valueBoolean;
        private int valueInteger;
        private double valueDouble;
        private final ValueType type;

        Value(String value, ValueType type) {
            this.type = type;
            setValue(value);
        }

        void setValue(String value) {
            this.valueString = value;
            this.valueBoolean = Boolean.parseBoolean(value);
            this.valueInteger = this.valueBoolean ? 1 : 0;
            try {
                this.valueInteger = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // not a number
            }
            try {
                this.valueDouble = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // not a number
            }
        }
    }

    public enum ValueType {
        ANY_VALUE,
        BOOLEAN_VALUE,
        NUMERICAL_VALUE
    }
}
