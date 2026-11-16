package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * Signature changes of Minecraft 1.20.1 to 1.21.1 found by the ATM 9 ranking of 2026-11-15, round 6 (see {@code mc-calls.tsv}).
 */
public final class Round6Compat {
    private Round6Compat() {}

    /**
     * {@code InventoryChangeTrigger.TriggerInstance.hasItems(ItemLike...)} returned the condition object; 1.21.1 returns a {@code Criterion} holding
     * it. The same condition is built directly (any slot counts, no player predicate).
     */
    public static InventoryChangeTrigger.TriggerInstance hasItems(ItemLike[] items) {
        ItemPredicate[] predicates = new ItemPredicate[items.length];
        for (int i = 0; i < items.length; i++) predicates[i] = ItemPredicate.Builder.item().of(items[i]).build();
        return hasItems(predicates);
    }

    /** {@code InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate...)}. */
    public static InventoryChangeTrigger.TriggerInstance hasItems(ItemPredicate[] predicates) {
        return new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(predicates));
    }

    /**
     * Argument producer for {@code new RegionFileStorage(folder, sync)}: 1.21.1 also wants a {@code RegionStorageInfo} (level name, dimension, kind of
     * data) that only names the storage in log lines and error reports. Approximation: the folder name stands for the level, the dimension is the
     * overworld's key.
     */
    public static net.minecraft.world.level.chunk.storage.RegionStorageInfo regionInfo(java.nio.file.Path folder) {
        java.nio.file.Path name = folder.getFileName();
        return new net.minecraft.world.level.chunk.storage.RegionStorageInfo(name == null ? "legacy" : name.toString(), net.minecraft.world.level.Level.OVERWORLD, "chunk");
    }

    /**
     * {@code new Criterion(instance)}: 1.21.1's record also holds the trigger, which a condition no longer knows. Found through the id of a mod's own
     * condition ({@link LegacyCriterionInstance}) or, for an inventory condition, the vanilla trigger; other conditions are an error.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.advancements.Criterion<?> newCriterion(CriterionTriggerInstance instance) {
        net.minecraft.advancements.CriterionTrigger trigger = null;
        ResourceLocation id = criterionId(instance);
        if (id != null) trigger = net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.get(id);
        if (trigger == null) throw new IllegalArgumentException("No advancement trigger known for the condition " + instance);
        return new net.minecraft.advancements.Criterion(trigger, instance);
    }

    /** {@code WorldGenRegion.writeRadiusCutoff} (field read): the radius around the centre chunk blocks may be written in, now held by the generation step. */
    public static int writeRadius(net.minecraft.server.level.WorldGenRegion region) {
        return region.generatingStep.blockStateWriteRadius();
    }

    /** {@code ServerLevel.addDuringPortalTeleport(player)} / {@code addDuringCommandTeleport(player)}: one method for every teleport now. */
    public static void addDuringTeleport(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        level.addDuringTeleport(player);
    }

    /**
     * {@code CriterionTriggerInstance.getCriterion()}: the id of the trigger the condition belongs to. 1.21.1 conditions do not know their trigger; a
     * mod's own condition ({@link LegacyCriterionInstance}) carries the id it was built with, an inventory condition answers the vanilla trigger's id, the rest
     * answer null.
     */
    public static ResourceLocation criterionId(CriterionTriggerInstance instance) {
        if (instance instanceof LegacyCriterionInstance legacy) return legacy.getCriterion();
        if (instance instanceof InventoryChangeTrigger.TriggerInstance) return net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.getKey(net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED);
        return null;
    }

    /**
     * Argument producer for {@code new SimpleCookingSerializer(baker, time)} and {@code super(baker, time)} of a mod's own serializer class: the 1.21.1
     * factory has no recipe id; the 1.20.1 constructor gets {@link RoundCompat}'s unknown id.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.item.crafting.AbstractCookingRecipe.Factory cookingFactory(LegacyCookieBaker baker) {
        return (group, category, ingredient, result, xp, time) -> baker.create(RoundCompat.UNKNOWN_RECIPE, (String) group,
                (net.minecraft.world.item.crafting.CookingBookCategory) category, (net.minecraft.world.item.crafting.Ingredient) ingredient,
                (net.minecraft.world.item.ItemStack) result, (Float) xp, (Integer) time);
    }

    /** {@code GameProfile.isComplete()} (removed from authlib): an id and a name. */
    public static boolean profileIsComplete(com.mojang.authlib.GameProfile profile) {
        return profile.getId() != null && profile.getName() != null && !profile.getName().isBlank();
    }

    /** {@code UUIDUtil.getOrCreatePlayerUUID(profile)}: the profile's id, else the offline id of its name. */
    public static java.util.UUID playerUuid(com.mojang.authlib.GameProfile profile) {
        return profile.getId() != null ? profile.getId() : net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(profile.getName());
    }

    /** {@code builder.buildPatch(access, lookup)}: 1.21.1 also takes the cloner factory; the answer is the full lookup (patched registries on top of the given ones). */
    public static net.minecraft.core.HolderLookup.Provider buildPatch(net.minecraft.core.RegistrySetBuilder builder, net.minecraft.core.RegistryAccess access,
            net.minecraft.core.HolderLookup.Provider lookup) {
        return builder.buildPatch(access, lookup, new net.minecraft.core.Cloner.Factory()).full();
    }

    /**
     * {@code SuspiciousStewItem.saveMobEffect(stack, effect, ticks)}: adds the effect to the stew's effect component.
     */
    public static void saveStewEffect(net.minecraft.world.item.ItemStack stack, net.minecraft.world.effect.MobEffect effect, int ticks) {
        net.minecraft.world.item.component.SuspiciousStewEffects current = stack.getOrDefault(net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS,
                net.minecraft.world.item.component.SuspiciousStewEffects.EMPTY);
        stack.set(net.minecraft.core.component.DataComponents.SUSPICIOUS_STEW_EFFECTS, current.withEffectAdded(
                new net.minecraft.world.item.component.SuspiciousStewEffects.Entry(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), ticks)));
    }

    /**
     * {@code Tier.getTag()} (Forge): the tag of blocks that need this tier. 1.21.1 only names the blocks a tier can NOT mine, which is no substitute, so
     * the answer is null ("no tag", which the Forge contract allows).
     */
    public static net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tierTag(net.minecraft.world.item.Tier tier) {
        return null;
    }

    /** {@code ArmorTrim.innerTexture(material)} for a 1.20.1 armor material. */
    public static ResourceLocation trimInner(net.minecraft.world.item.armortrim.ArmorTrim trim, LegacyArmorMaterial material) {
        return trim.innerTexture(ArmorCompat.holder(material));
    }

    /** {@code ArmorTrim.outerTexture(material)} for a 1.20.1 armor material. */
    public static ResourceLocation trimOuter(net.minecraft.world.item.armortrim.ArmorTrim trim, LegacyArmorMaterial material) {
        return trim.outerTexture(ArmorCompat.holder(material));
    }

    /**
     * {@code RecipeManager.recipes} (field read): recipes by type and id, the shape 1.20.1 kept. A copy: the recipes are the 1.21.1 objects (holders
     * unwrapped), so a mod changes the manager through {@link #setRecipes}.
     */
    public static java.util.Map<net.minecraft.world.item.crafting.RecipeType<?>, java.util.Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>>> recipes(
            net.minecraft.world.item.crafting.RecipeManager manager) {
        java.util.Map<net.minecraft.world.item.crafting.RecipeType<?>, java.util.Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>>> out = new java.util.HashMap<>();
        for (net.minecraft.world.item.crafting.RecipeHolder<?> h : manager.getRecipes()) {
            out.computeIfAbsent(h.value().getType(), t -> new java.util.LinkedHashMap<>()).put(h.id(), h.value());
        }
        return out;
    }

    /** {@code RecipeManager.recipes = map} (field write): the manager takes exactly these recipes. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setRecipes(net.minecraft.world.item.crafting.RecipeManager manager, java.util.Map map) {
        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders = new java.util.ArrayList<>();
        for (Object byId : map.values()) {
            for (Object e : ((java.util.Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>>) byId).entrySet()) {
                java.util.Map.Entry<ResourceLocation, net.minecraft.world.item.crafting.Recipe<?>> entry = (java.util.Map.Entry) e;
                holders.add(new net.minecraft.world.item.crafting.RecipeHolder(entry.getKey(), entry.getValue()));
            }
        }
        manager.replaceRecipes(holders);
    }

    /**
     * {@code RecipeManager.fromJson(id, json)} (static): the recipe of a JSON file, through the recipe codec (every registered serializer, vanilla and
     * mod). Needs the running server's registries; throws {@code JsonParseException} on a bad file as before.
     */
    public static net.minecraft.world.item.crafting.Recipe<?> recipeFromJson(ResourceLocation id, com.google.gson.JsonObject json) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new com.google.gson.JsonParseException("No registries to read the recipe " + id + " with yet");
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        return net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops, json).getOrThrow(msg -> new com.google.gson.JsonParseException(id + ": " + msg));
    }

    /** Argument producer for the cauldron constructors: 1.20.1 passed the plain item map, 1.21.1 a named interaction map. */
    public static net.minecraft.core.cauldron.CauldronInteraction.InteractionMap interactionMap(java.util.Map<net.minecraft.world.item.Item, net.minecraft.core.cauldron.CauldronInteraction> map) {
        return new net.minecraft.core.cauldron.CauldronInteraction.InteractionMap("legacy_" + Integer.toHexString(System.identityHashCode(map)), map);
    }

    /** Argument producer for {@code new LayeredCauldronBlock(props, fillPredicate, map)}: the kind of precipitation that fills it (the first the predicate accepts). */
    public static net.minecraft.world.level.biome.Biome.Precipitation fillingPrecipitation(java.util.function.Predicate<net.minecraft.world.level.biome.Biome.Precipitation> predicate) {
        if (predicate.test(net.minecraft.world.level.biome.Biome.Precipitation.RAIN)) return net.minecraft.world.level.biome.Biome.Precipitation.RAIN;
        if (predicate.test(net.minecraft.world.level.biome.Biome.Precipitation.SNOW)) return net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
        return net.minecraft.world.level.biome.Biome.Precipitation.NONE;
    }

    private static net.minecraft.world.item.trading.ItemCost cost(net.minecraft.world.item.ItemStack s) {
        return new net.minecraft.world.item.trading.ItemCost(s.getItem(), Math.max(1, s.getCount()));
    }

    /** {@code new MerchantOffer(costA, costB, result, uses, maxUses, xp, priceMultiplier)}. */
    public static net.minecraft.world.item.trading.MerchantOffer newOffer(net.minecraft.world.item.ItemStack costA, net.minecraft.world.item.ItemStack costB,
            net.minecraft.world.item.ItemStack result, int uses, int maxUses, int xp, float priceMultiplier) {
        return newOffer(costA, costB, result, uses, maxUses, xp, priceMultiplier, 0);
    }

    /** {@code new MerchantOffer(costA, costB, result, uses, maxUses, xp, priceMultiplier, demand)}. */
    public static net.minecraft.world.item.trading.MerchantOffer newOffer(net.minecraft.world.item.ItemStack costA, net.minecraft.world.item.ItemStack costB,
            net.minecraft.world.item.ItemStack result, int uses, int maxUses, int xp, float priceMultiplier, int demand) {
        return new net.minecraft.world.item.trading.MerchantOffer(cost(costA), costB.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(cost(costB)), result,
                uses, maxUses, xp, priceMultiplier, demand);
    }

    /** {@code new MerchantOffers(tag)}: the offers of a compound with a {@code Recipes} list (empty offers for a tag that does not read). */
    public static net.minecraft.world.item.trading.MerchantOffers merchantOffersFromTag(net.minecraft.nbt.CompoundTag tag) {
        return net.minecraft.world.item.trading.MerchantOffers.CODEC.parse(MigrateContext.provider().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result().orElseGet(net.minecraft.world.item.trading.MerchantOffers::new);
    }

    /** {@code zombieVillager.setTradeOffers(tag)}. */
    public static void zombieSetTradeOffers(net.minecraft.world.entity.monster.ZombieVillager zombie, net.minecraft.nbt.CompoundTag tag) {
        zombie.setTradeOffers(merchantOffersFromTag(tag));
    }

    /** {@code zombieVillager.tradeOffers} (field read): the offers as a compound, null without any. */
    public static net.minecraft.nbt.CompoundTag zombieTradeOffers(net.minecraft.world.entity.monster.ZombieVillager zombie) {
        return zombie.tradeOffers == null ? null : Round5Compat.merchantOffersTag(zombie.tradeOffers);
    }

    /** {@code zombieVillager.tradeOffers = tag} (field write). */
    public static void zombieSetTradeOffersField(net.minecraft.world.entity.monster.ZombieVillager zombie, net.minecraft.nbt.CompoundTag tag) {
        zombie.setTradeOffers(tag == null ? null : merchantOffersFromTag(tag));
    }

    /** Argument producer for the mob effect packet: 1.21.1 also says whether the client blends the effect in (no for an update sent by 1.20.1 code). */
    public static boolean noBlend(int ignored) {
        return false;
    }

    /** {@code CrossbowItem.setCharged(stack, charged)} (static): a crossbow is charged while it holds projectiles, so only discharging can be done here. */
    public static void setCrossbowCharged(net.minecraft.world.item.ItemStack stack, boolean charged) {
        if (!charged) stack.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.EMPTY);
    }

    /** {@code MobEffectInstance.getCurativeItems()} / {@code MobEffect.getCurativeItems()}: the effect is cured by milk unless it says otherwise (the cures are per instance now). */
    public static java.util.List<net.minecraft.world.item.ItemStack> curativeItems(Object effect) {
        if (effect instanceof net.minecraft.world.effect.MobEffectInstance instance && !instance.getCures().contains(net.neoforged.neoforge.common.EffectCures.MILK)) {
            return new java.util.ArrayList<>();
        }
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        out.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
        return out;
    }

    /** {@code Arrow.setEffectsFromItem(stack)}: the potion contents of the stack become the arrow's. */
    public static void arrowEffectsFromItem(net.minecraft.world.entity.projectile.Arrow arrow, net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.alchemy.PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents != null) arrow.setPotionContents(contents);
    }

    /** {@code ChiseledBookShelfBlock.getHitSlot(vec)} (static, 1.20.1's rule): two rows of three, the top row first. */
    public static int bookshelfHitSlot(net.minecraft.world.phys.Vec2 hit) {
        int row = hit.y >= 0.5F ? 0 : 1;
        int column = hit.x < 0.375F ? 0 : hit.x < 0.6875F ? 1 : 2;
        return column + row * 3;
    }

    /** {@code MobCategory.byName(name)} (static). */
    public static net.minecraft.world.entity.MobCategory mobCategoryByName(String name) {
        for (net.minecraft.world.entity.MobCategory c : net.minecraft.world.entity.MobCategory.values()) if (c.getName().equals(name)) return c;
        return null;
    }

    /** {@code FoodData.eat(item, stack)}: the food of the stack (nothing when it has none). */
    public static void eatStack(net.minecraft.world.food.FoodData data, net.minecraft.world.item.Item item, net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.food.FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) data.eat(food);
    }

    /** {@code ResourceLocation.of(string, separator)} (static): namespace and path split at the separator (minecraft without one). */
    public static ResourceLocation locationOf(String string, char separator) {
        int i = string.indexOf(separator);
        return i >= 0 ? ResourceLocation.fromNamespaceAndPath(string.substring(0, i), string.substring(i + 1)) : ResourceLocation.withDefaultNamespace(string);
    }

    /** {@code Horse.getArmor()}: the body armor (horse armor) item. */
    public static net.minecraft.world.item.ItemStack horseArmor(net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
        return horse.getBodyArmorItem();
    }

    /** {@code LayeredCauldronBlock.RAIN}: the precipitation test that lets rain fill a cauldron. */
    public static java.util.function.Predicate<net.minecraft.world.level.biome.Biome.Precipitation> cauldronRain() {
        return p -> p == net.minecraft.world.level.biome.Biome.Precipitation.RAIN;
    }

    /** {@code LayeredCauldronBlock.SNOW}: the precipitation test that lets snow fill a cauldron. */
    public static java.util.function.Predicate<net.minecraft.world.level.biome.Biome.Precipitation> cauldronSnow() {
        return p -> p == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
    }

    /** {@code EquipmentSlot.byTypeAndIndex(type, index)} (static): the slot of that type with that index. */
    public static net.minecraft.world.entity.EquipmentSlot slotByTypeAndIndex(net.minecraft.world.entity.EquipmentSlot.Type type, int index) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == type && slot.getIndex() == index) return slot;
        }
        throw new IllegalArgumentException("Invalid slot type " + type + " with index " + index);
    }

    /** {@code Util.isBlank(string)}: gone (use the JDK). */
    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    /** {@code Painting.storeVariant(tag, variant)}: the variant's registry id under {@code "variant"}, as 1.20.1 wrote it. */
    public static void storePaintingVariant(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.Holder<net.minecraft.world.entity.decoration.PaintingVariant> variant) {
        variant.unwrapKey().ifPresent(key -> tag.putString("variant", key.location().toString()));
    }

    /** {@code Entity.portalEntrancePos} (field read): where the entity entered the portal it is in, null outside one. */
    public static net.minecraft.core.BlockPos portalEntrancePos(net.minecraft.world.entity.Entity entity) {
        return entity.portalProcess == null ? null : entity.portalProcess.getEntryPosition();
    }

    /**
     * {@code entity.portalEntrancePos = pos} (field write). Approximation: only an entity that is inside a portal this tick has a portal process to
     * update; the 1.20.1 code could also set the field ahead of the teleport, which has no 1.21.1 counterpart.
     */
    public static void setPortalEntrancePos(net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos pos) {
        if (entity.portalProcess != null && pos != null) entity.portalProcess.updateEntryPosition(pos);
    }

    /** {@code ArmorItem.knockbackResistance} (protected field of 1.20.1): the material's value. */
    public static float armorKnockbackResistance(net.minecraft.world.item.ArmorItem item) {
        return item.getMaterial().value().knockbackResistance();
    }

    /** {@code LootPool.entries} (field read, an array in 1.20.1): a copy of the pool's entry list. */
    public static net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] poolEntries(net.minecraft.world.level.storage.loot.LootPool pool) {
        return pool.entries.toArray(new net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[0]);
    }

    /** {@code LootPool.entries = array} (field write): the pool draws from the new entries (it reads its list on every roll). */
    public static void setPoolEntries(net.minecraft.world.level.storage.loot.LootPool pool, net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] entries) {
        pool.entries = new java.util.ArrayList<>(java.util.Arrays.asList(entries));
    }

    /** {@code Block.appendHoverText(stack, level, tooltip, flag)} called on a block: the 1.21.1 method with an empty tooltip context. */
    public static void blockHover(net.minecraft.world.level.block.Block block, net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        block.appendHoverText(stack, net.minecraft.world.item.Item.TooltipContext.EMPTY, tooltip, flag);
    }
}
