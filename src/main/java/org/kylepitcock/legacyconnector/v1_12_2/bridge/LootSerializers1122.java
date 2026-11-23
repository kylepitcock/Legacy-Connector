package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.KilledByPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootConditionManager;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.RandomChance;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.RandomChanceWithLooting;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.EnchantRandomly;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.EnchantWithLevels;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.LootFunction;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.LootFunctionManager;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.LootingEnchantBonus;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.SetCount;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.SetDamage;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.SetMetadata;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.SetNBT;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions.Smelt;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.properties.EntityProperty;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.properties.EntityPropertyManager;

/**
 * The loot functions and conditions 1.12.2 registered by name ({@code LootFunctionManager}/{@code LootConditionManager}'s static blocks): the
 * JSON names of the mods' loot tables ({@code set_count}, {@code set_data}, {@code random_chance_with_looting}, ...). Without them a table's
 * entries kept no function and no condition: every entry dropped on every kill as the base item (Techguns' super mutants dropped stone bullets,
 * {@code techguns:itemshared} meta 0, where the table asks for meta 160 at a 2% chance). A name 1.12 did not know failed the whole table there;
 * here an unknown function leaves the stack as it is and an unknown condition passes, each reported once.
 */
public final class LootSerializers1122 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Loot1122");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static volatile boolean registered;

    private LootSerializers1122() {}

    /** Registers the vanilla types (once; called by the managers before their first lookup). */
    public static void ensure() {
        if (registered) return;
        synchronized (LootSerializers1122.class) {
            if (registered) return;
            registered = true;
            functions();
            conditions();
        }
    }

    private static RandomValueRange range(JsonObject json, String key, JsonDeserializationContext context, float fallback) {
        return json.has(key) ? context.deserialize(json.get(key), RandomValueRange.class) : new RandomValueRange(fallback);
    }

    private static void functions() {
        LootFunctionManager.registerFunction(new LootFunction.Serializer<SetCount>(new ResourceLocation("set_count"), SetCount.class) {
            @Override public void serialize(JsonObject o, SetCount f, JsonSerializationContext c) {}
            @Override public SetCount deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                return new SetCount(conditions, range(o, "count", c, 1));
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<SetMetadata>(new ResourceLocation("set_data"), SetMetadata.class) {
            @Override public void serialize(JsonObject o, SetMetadata f, JsonSerializationContext c) {}
            @Override public SetMetadata deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                return new SetMetadata(conditions, range(o, "data", c, 0));
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<SetDamage>(new ResourceLocation("set_damage"), SetDamage.class) {
            @Override public void serialize(JsonObject o, SetDamage f, JsonSerializationContext c) {}
            @Override public SetDamage deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                return new SetDamage(conditions, range(o, "damage", c, 1));
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<SetNBT>(new ResourceLocation("set_nbt"), SetNBT.class) {
            @Override public void serialize(JsonObject o, SetNBT f, JsonSerializationContext c) {}
            @Override public SetNBT deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                try {
                    return new SetNBT(conditions, legacyconnector.v1_12_2.net.minecraft.nbt.JsonToNBT.getTagFromJson(o.get("tag").getAsString()));
                } catch (Exception e) {
                    throw new com.google.gson.JsonSyntaxException(e);
                }
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<EnchantRandomly>(new ResourceLocation("enchant_randomly"), EnchantRandomly.class) {
            @Override public void serialize(JsonObject o, EnchantRandomly f, JsonSerializationContext c) {}
            @Override public EnchantRandomly deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                List<Enchantment> list = new ArrayList<>();
                if (o.has("enchantments") && o.get("enchantments").isJsonArray()) {
                    for (JsonElement e : o.getAsJsonArray("enchantments")) {
                        Enchantment found = Enchantment.getEnchantmentByLocation(e.getAsString());
                        if (found != null) list.add(found);
                    }
                }
                return new EnchantRandomly(conditions, list);
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<EnchantWithLevels>(new ResourceLocation("enchant_with_levels"), EnchantWithLevels.class) {
            @Override public void serialize(JsonObject o, EnchantWithLevels f, JsonSerializationContext c) {}
            @Override public EnchantWithLevels deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                boolean treasure = o.has("treasure") && o.get("treasure").getAsBoolean();
                return new EnchantWithLevels(conditions, range(o, "levels", c, 1), treasure);
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<LootingEnchantBonus>(new ResourceLocation("looting_enchant"), LootingEnchantBonus.class) {
            @Override public void serialize(JsonObject o, LootingEnchantBonus f, JsonSerializationContext c) {}
            @Override public LootingEnchantBonus deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                int limit = o.has("limit") ? o.get("limit").getAsInt() : 0;
                return new LootingEnchantBonus(conditions, range(o, "count", c, 0), limit);
            }
        });
        LootFunctionManager.registerFunction(new LootFunction.Serializer<Smelt>(new ResourceLocation("furnace_smelt"), Smelt.class) {
            @Override public void serialize(JsonObject o, Smelt f, JsonSerializationContext c) {}
            @Override public Smelt deserialize(JsonObject o, JsonDeserializationContext c, LootCondition[] conditions) {
                return new Smelt(conditions);
            }
        });
    }

    private static void conditions() {
        LootConditionManager.registerCondition(new LootCondition.Serializer<RandomChance>(new ResourceLocation("random_chance"), RandomChance.class) {
            @Override public void serialize(JsonObject o, RandomChance v, JsonSerializationContext c) {}
            @Override public RandomChance deserialize(JsonObject o, JsonDeserializationContext c) {
                return new RandomChance(o.get("chance").getAsFloat());
            }
        });
        LootConditionManager.registerCondition(new LootCondition.Serializer<RandomChanceWithLooting>(new ResourceLocation("random_chance_with_looting"),
                RandomChanceWithLooting.class) {
            @Override public void serialize(JsonObject o, RandomChanceWithLooting v, JsonSerializationContext c) {}
            @Override public RandomChanceWithLooting deserialize(JsonObject o, JsonDeserializationContext c) {
                return new RandomChanceWithLooting(o.get("chance").getAsFloat(), o.has("looting_multiplier") ? o.get("looting_multiplier").getAsFloat() : 0.0F);
            }
        });
        LootConditionManager.registerCondition(new LootCondition.Serializer<KilledByPlayer>(new ResourceLocation("killed_by_player"), KilledByPlayer.class) {
            @Override public void serialize(JsonObject o, KilledByPlayer v, JsonSerializationContext c) {}
            @Override public KilledByPlayer deserialize(JsonObject o, JsonDeserializationContext c) {
                return new KilledByPlayer(o.has("inverse") && o.get("inverse").getAsBoolean());
            }
        });
        LootConditionManager.registerCondition(new LootCondition.Serializer<EntityHasProperty>(new ResourceLocation("entity_properties"), EntityHasProperty.class) {
            @Override public void serialize(JsonObject o, EntityHasProperty v, JsonSerializationContext c) {}
            @Override public EntityHasProperty deserialize(JsonObject o, JsonDeserializationContext c) {
                List<EntityProperty> properties = new ArrayList<>();
                if (o.has("properties") && o.get("properties").isJsonObject()) {
                    for (var e : o.getAsJsonObject("properties").entrySet()) {
                        properties.add(EntityPropertyManager.getSerializerForName(new ResourceLocation(e.getKey())).deserialize(e.getValue(), c));
                    }
                }
                LootContext.EntityTarget target = o.has("entity") ? c.deserialize(o.get("entity"), LootContext.EntityTarget.class) : LootContext.EntityTarget.THIS;
                return new EntityHasProperty(properties.toArray(new EntityProperty[0]), target);
            }
        });
    }

    /** 1.12's {@code entity_properties} condition: every property holds for the target entity (none there fails). */
    public static final class EntityHasProperty implements LootCondition {
        private final EntityProperty[] properties;
        private final LootContext.EntityTarget target;

        public EntityHasProperty(EntityProperty[] properties, LootContext.EntityTarget target) {
            this.properties = properties;
            this.target = target;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            var entity = context.getEntity(target);
            if (entity == null) return false;
            for (EntityProperty p : properties) if (!p.testProperty(rand, entity)) return false;
            return true;
        }
    }

    /** A function of a name no mod registered: the stack is left as it is. */
    public static LootFunction unknownFunction(ResourceLocation name, LootCondition[] conditions) {
        if (REPORTED.add("function " + name)) LOG.warn("Unknown 1.12.2 loot function {}: ignored", name);
        return new LootFunction(conditions) {
            @Override
            public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
                return stack;
            }
        };
    }

    /** A condition of a name no mod registered: it passes. */
    public static LootCondition unknownCondition(ResourceLocation name) {
        if (REPORTED.add("condition " + name)) LOG.warn("Unknown 1.12.2 loot condition {}: treated as passing", name);
        return (rand, context) -> true;
    }
}
