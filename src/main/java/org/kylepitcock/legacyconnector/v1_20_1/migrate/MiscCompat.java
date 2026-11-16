package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Small signature changes of Minecraft 1.20.1 to 1.21.1 that need a line or two of code (see {@code mc-calls.tsv}). */
public final class MiscCompat {
    private MiscCompat() {}

    // ------------------------------------------------------------------ items and entities

    /**
     * 1.20.1 {@code hurtAndBreak(int, LivingEntity, Consumer<LivingEntity> onBroken)}: 1.21.1 wants the server level and reports the broken item;
     * the old callback receives the entity as before.
     */
    public static <T extends LivingEntity> void hurtAndBreak(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        stack.hurtAndBreak(amount, level, entity instanceof ServerPlayer sp ? sp : null, item -> onBroken.accept(entity));
    }

    /** {@code Player.broadcastBreakEvent(InteractionHand)}: the break animation of the stack in that hand. */
    public static void broadcastBreakEvent(LivingEntity entity, InteractionHand hand) {
        ItemStack held = entity.getItemInHand(hand);
        entity.onEquippedItemBroken(held.getItem(), hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public static void broadcastBreakEvent(LivingEntity entity, EquipmentSlot slot) {
        entity.onEquippedItemBroken(entity.getItemBySlot(slot).getItem(), slot);
    }

    public static void setSecondsOnFire(Entity entity, int seconds) {
        entity.igniteForSeconds(seconds);
    }

    public static int getMaxDamage(Item item) {
        return item.getDefaultInstance().getMaxDamage();
    }

    public static net.minecraft.world.phys.AABB newAabb(BlockPos a, BlockPos b) {
        return new net.minecraft.world.phys.AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /** {@code DyeColor.getTextureDiffuseColors()}: the color as float RGB. */
    public static float[] dyeColors(net.minecraft.world.item.DyeColor color) {
        int c = color.getTextureDiffuseColor();
        return new float[] {(c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F};
    }

    public static Component readComponent(FriendlyByteBuf buf) {
        return fromJson(buf.readUtf(262144));
    }

    public static FriendlyByteBuf writeComponent(FriendlyByteBuf buf, Component c) {
        return buf.writeUtf(toJson(c), 262144);
    }

    /** Trades took an {@code ItemCost} (item and count, no tag) for the price instead of a stack. */
    public static net.minecraft.world.item.trading.MerchantOffer newOffer(ItemStack costA, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        return new net.minecraft.world.item.trading.MerchantOffer(cost(costA), result, maxUses, xp, priceMultiplier);
    }

    public static net.minecraft.world.item.trading.MerchantOffer newOffer(ItemStack costA, ItemStack costB, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        return new net.minecraft.world.item.trading.MerchantOffer(cost(costA), costB.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(cost(costB)), result,
                maxUses, xp, priceMultiplier);
    }

    private static net.minecraft.world.item.trading.ItemCost cost(ItemStack s) {
        return new net.minecraft.world.item.trading.ItemCost(s.getItem(), Math.max(1, s.getCount()));
    }

    // ------------------------------------------------------------------ tools: attack damage and speed moved into the item attribute modifiers

    public static Item.Properties swordProps(net.minecraft.world.item.Tier tier, int damage, float speed, Item.Properties props) {
        return props.attributes(net.minecraft.world.item.SwordItem.createAttributes(tier, damage, speed));
    }

    public static Item.Properties diggerProps(net.minecraft.world.item.Tier tier, float damage, float speed, Item.Properties props) {
        return props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed));
    }

    public static Item.Properties diggerProps(net.minecraft.world.item.Tier tier, int damage, float speed, Item.Properties props) {
        return props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed));
    }

    public static net.minecraft.world.item.SwordItem newSword(net.minecraft.world.item.Tier tier, int damage, float speed, Item.Properties props) {
        return new net.minecraft.world.item.SwordItem(tier, props.attributes(net.minecraft.world.item.SwordItem.createAttributes(tier, damage, speed)));
    }

    public static net.minecraft.world.item.AxeItem newAxe(net.minecraft.world.item.Tier tier, float damage, float speed, Item.Properties props) {
        return new net.minecraft.world.item.AxeItem(tier, props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed)));
    }

    public static net.minecraft.world.item.ShovelItem newShovel(net.minecraft.world.item.Tier tier, float damage, float speed, Item.Properties props) {
        return new net.minecraft.world.item.ShovelItem(tier, props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed)));
    }

    public static net.minecraft.world.item.PickaxeItem newPickaxe(net.minecraft.world.item.Tier tier, int damage, float speed, Item.Properties props) {
        return new net.minecraft.world.item.PickaxeItem(tier, props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed)));
    }

    public static net.minecraft.world.item.HoeItem newHoe(net.minecraft.world.item.Tier tier, int damage, float speed, Item.Properties props) {
        return new net.minecraft.world.item.HoeItem(tier, props.attributes(net.minecraft.world.item.DiggerItem.createAttributes(tier, damage, speed)));
    }

    public static boolean isEdible(Item item) {
        return item.getDefaultInstance().has(net.minecraft.core.component.DataComponents.FOOD);
    }

    public static net.minecraft.world.food.FoodProperties getFoodProperties(Item item) {
        return item.getDefaultInstance().get(net.minecraft.core.component.DataComponents.FOOD);
    }

    public static boolean stackIsEdible(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    /** {@code SynchedEntityData.define} inside {@code defineSynchedData}: the 1.21.1 builder of the call in progress (remembered by the generated bridge). */
    public static <T> void define(net.minecraft.network.syncher.SynchedEntityData data, net.minecraft.network.syncher.EntityDataAccessor<T> accessor, T value) {
        if (data != null) {
            // after the entity exists (a mixin at the end of Entity.<init>): the id was defined with its default by LegacyEntityData; this sets the value wanted
            try {
                data.set(accessor, value);
            } catch (RuntimeException e) {
                // undefined id: nothing to set
            }
            return;
        }
        MigrateContext.builder().define(accessor, value);
    }

    public static int getMaxStackSize(Item item) {
        return item.getDefaultMaxStackSize();
    }

    public static boolean isDamageable(Item item) {
        return item.getDefaultInstance().isDamageableItem();
    }

    // ------------------------------------------------------------------ blocks

    public static DoorBlock newDoor(BlockBehaviour.Properties properties, BlockSetType type) {
        return new DoorBlock(type, properties);
    }

    public static TrapDoorBlock newTrapDoor(BlockBehaviour.Properties properties, BlockSetType type) {
        return new TrapDoorBlock(type, properties);
    }

    // ------------------------------------------------------------------ nbt and containers

    public static BlockPos readBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }

    public static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(tag, items, MigrateContext.provider());
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        return ContainerHelper.saveAllItems(tag, items, MigrateContext.provider());
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items, boolean alwaysPutTag) {
        return ContainerHelper.saveAllItems(tag, items, alwaysPutTag, MigrateContext.provider());
    }

    public static <T extends SavedData> T computeIfAbsent(DimensionDataStorage storage, Function<CompoundTag, T> loader, Supplier<T> factory, String name) {
        return storage.computeIfAbsent(new SavedData.Factory<>(factory, (tag, provider) -> {
            MigrateContext.remember(provider);
            return loader.apply(tag);
        }, null), name);
    }

    // ------------------------------------------------------------------ text and network

    /**
     * Preserve 1.20.1 Gson semantics for an absent component. Gson encoded a
     * null Component as the JSON literal {@code null}; the 1.21.1 Component
     * codec rejects a Java null before it can encode anything.
     *
     * <p>Palladium's NameChangeAbility intentionally synchronizes a null
     * cached name during initialization and teardown, so this is a normal
     * value, not an invalid caller.</p>
     */
    public static String toJson(Component c) {
        if (c == null) return "null";
        return Component.Serializer.toJson(c, MigrateContext.provider());
    }

    public static MutableComponent fromJson(String json) {
        if (json == null || "null".equals(json.trim())) return null;
        return Component.Serializer.fromJson(json, MigrateContext.provider());
    }

    public static MutableComponent fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) return null;
        return Component.Serializer.fromJson(json, MigrateContext.provider());
    }

    /** The 1.20.1 wire form of a stack: present flag, item registry id, count, NBT. Independent of the registry access, so it works on any buffer. */
    public static FriendlyByteBuf writeItem(FriendlyByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeVarInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
            buf.writeByte(stack.getCount());
            CompoundTag whole = ItemStackNbt.save(stack, new CompoundTag());
            whole.remove("id");
            whole.remove("Count");
            whole.remove("count");
            buf.writeNbt(whole.isEmpty() ? null : whole);
        }
        return buf;
    }

    public static ItemStack readItem(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.byId(buf.readVarInt());
        int count = buf.readByte();
        CompoundTag extra = buf.readNbt();
        CompoundTag whole = extra == null ? new CompoundTag() : extra;
        whole.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
        whole.putByte("Count", (byte) count);
        whole.putInt("count", count);
        return ItemStackNbt.of(whole);
    }

    public static FriendlyByteBuf writeNbt(FriendlyByteBuf buf, CompoundTag tag) {
        return buf.writeNbt((Tag) tag);
    }

    // ------------------------------------------------------------------ ingredients

    public static Ingredient ingredientFromJson(JsonElement json) {
        return Ingredient.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(com.google.gson.JsonParseException::new);
    }

    public static JsonElement ingredientToJson(Ingredient ingredient) {
        return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow(IllegalStateException::new);
    }

    public static void ingredientToNetwork(Ingredient ingredient, FriendlyByteBuf buf) {
        ItemStack[] items = ingredient.getItems();
        buf.writeVarInt(items.length);
        for (ItemStack s : items) writeItem(buf, s);
    }

    public static Ingredient ingredientFromNetwork(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) stacks.add(readItem(buf));
        return Ingredient.of(stacks.stream().filter(s -> !s.isEmpty()).map(ItemStack::getItem).toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    // ------------------------------------------------------------------ small signature changes (milestone 2)

    /** {@code Tier.getLevel()} had no successor: the tag of blocks the tier cannot mine drops from (vanilla levels 0 to 4). */
    public static net.minecraft.tags.TagKey<Block> incorrectBlocksForLevel(int level) {
        return switch (level) {
            case 0 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    /** {@code ResourceLocation.isValidResourceLocation(String)}. */
    public static boolean isValidResourceLocation(String s) {
        return net.minecraft.resources.ResourceLocation.tryParse(s) != null;
    }

    /** {@code SharedConstants.isAllowedChatCharacter(char)} moved to {@code StringUtil}. */
    public static boolean isAllowedChatCharacter(char c) {
        return net.minecraft.util.StringUtil.isAllowedChatCharacter(c);
    }

    /** {@code ItemStack.getUseDuration()}: 1.21.1 asks the user of the item; without one the item's own answer is used (0 when it needs the entity). */
    public static int getUseDuration(ItemStack stack) {
        try {
            return stack.getUseDuration(null);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** {@code ItemStack.getTooltipLines(Player, TooltipFlag)}: the context is the player's level (empty without one). */
    public static List<Component> getTooltipLines(ItemStack stack, Player player, net.minecraft.world.item.TooltipFlag flag) {
        Item.TooltipContext context = player != null ? Item.TooltipContext.of(player.level()) : Item.TooltipContext.EMPTY;
        return stack.getTooltipLines(context, player, flag);
    }

    /** {@code FoodProperties.getEffects()}: effect and probability pairs. */
    public static List<com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float>> getEffects(net.minecraft.world.food.FoodProperties food) {
        List<com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float>> out = new ArrayList<>();
        for (net.minecraft.world.food.FoodProperties.PossibleEffect e : food.effects()) out.add(com.mojang.datafixers.util.Pair.of(e.effect(), e.probability()));
        return out;
    }

    /** {@code VertexConsumer.defaultColor(r, g, b, a)} is gone: vertices carry their own color; the call has no effect. */
    public static void defaultColor(com.mojang.blaze3d.vertex.VertexConsumer consumer, int r, int g, int b, int a) {}

    public static void unsetDefaultColor(com.mojang.blaze3d.vertex.VertexConsumer consumer) {}

    // SpawnPlacements.Type became SpawnPlacementType with its constants in SpawnPlacementTypes
    public static net.minecraft.world.entity.SpawnPlacementType spawnOnGround() {
        return net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;
    }

    /** {@code SpawnPlacements.Type.valueOf(name)} (a mod reading the placement from its config): the enum constants are the four SpawnPlacementTypes now. */
    public static net.minecraft.world.entity.SpawnPlacementType spawnPlacementValueOf(String name) {
        return switch (name) {
            case "ON_GROUND" -> net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;
            case "IN_WATER" -> net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER;
            case "IN_LAVA" -> net.minecraft.world.entity.SpawnPlacementTypes.IN_LAVA;
            case "NO_RESTRICTIONS" -> net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS;
            default -> {
                net.minecraft.world.entity.SpawnPlacementType custom = RoundCompat.CUSTOM_PLACEMENTS.get(name);
                if (custom == null) throw new IllegalArgumentException("No enum constant SpawnPlacements.Type." + name);
                yield custom;
            }
        };
    }

    public static net.minecraft.world.entity.SpawnPlacementType spawnInWater() {
        return net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER;
    }

    public static net.minecraft.world.entity.SpawnPlacementType spawnInLava() {
        return net.minecraft.world.entity.SpawnPlacementTypes.IN_LAVA;
    }

    public static net.minecraft.world.entity.SpawnPlacementType spawnNoRestrictions() {
        return net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS;
    }

    /** {@code LivingEntity.getMobType()}, answered from the entity type tags that replaced {@code MobType}. */
    public static LegacyMobType getMobType(LivingEntity entity) {
        net.minecraft.world.entity.EntityType<?> type = entity.getType();
        if (type.is(net.minecraft.tags.EntityTypeTags.UNDEAD)) return LegacyMobType.UNDEAD;
        if (type.is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) return LegacyMobType.ARTHROPOD;
        if (type.is(net.minecraft.tags.EntityTypeTags.ILLAGER)) return LegacyMobType.ILLAGER;
        if (type.is(net.minecraft.tags.EntityTypeTags.AQUATIC)) return LegacyMobType.WATER;
        return LegacyMobType.UNDEFINED;
    }

    /** {@code MinecraftServer.getLootData()}. */
    public static LegacyLootData getLootData(net.minecraft.server.MinecraftServer server) {
        return new LegacyLootData(server);
    }

    /**
     * {@code Registry.register} after the registries froze (a mod adding a custom stat or a similar entry in common setup, which 1.20.1 allowed until the game
     * started): the registry is opened for this one entry.
     */
    public static <V, T extends V> T registerLate(net.minecraft.core.Registry<V> registry, ResourceLocation id, T value) {
        if (registry instanceof net.minecraft.core.MappedRegistry<V> mapped && mapped.frozen) {
            mapped.frozen = false;
            try {
                return net.minecraft.core.Registry.register(registry, id, value);
            } finally {
                mapped.frozen = true;
            }
        }
        return net.minecraft.core.Registry.register(registry, id, value);
    }

    public static <V, T extends V> T registerLate(net.minecraft.core.Registry<V> registry, String name, T value) {
        return registerLate(registry, ResourceLocation.parse(name), value);
    }

    /** {@code StructureType.register(name, codec)} (private in vanilla; mods open it with an access transformer): the codec becomes the type's MapCodec. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.level.levelgen.structure.StructureType registerStructureType(String name, com.mojang.serialization.Codec codec) {
        net.minecraft.world.level.levelgen.structure.StructureType type = () -> EntityCompat.toMapCodec(codec);
        return registerLate((net.minecraft.core.Registry) net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_TYPE, name, type);
    }

    /** {@code new DimensionDataStorage(folder, fixer)}: 1.21.1's storage also wants the registries its saved data is read with. */
    public static net.minecraft.world.level.storage.DimensionDataStorage dimensionDataStorage(java.io.File folder, com.mojang.datafixers.DataFixer fixer) {
        return new net.minecraft.world.level.storage.DimensionDataStorage(folder, fixer, legacyconnector.v1_20_1.ForgeApi1201.registries());
    }

    /** {@code DataFixerBuilder.buildUnoptimized()} (Datafixer Upper 6): 8 builds a result holding the fixer; nothing is optimised here either. */
    public static com.mojang.datafixers.DataFixer buildUnoptimized(com.mojang.datafixers.DataFixerBuilder builder) {
        return builder.build().fixer();
    }

    /** {@code new ShapedRecipe(id, group, category, width, height, ingredients, result)}: the shape is a pattern object now. */
    public static net.minecraft.world.item.crafting.ShapedRecipePattern pattern(int width, int height, NonNullList<Ingredient> ingredients) {
        return new net.minecraft.world.item.crafting.ShapedRecipePattern(width, height, ingredients, java.util.Optional.empty());
    }

    public static float intToFloat(int value) {
        return value;
    }

    public static List<net.minecraft.world.level.storage.loot.predicates.LootItemCondition> conditions(net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] array) {
        return java.util.Arrays.asList(array);
    }

    /** {@code new StemBlock(fruit, seed, properties)}: 1.21.1 names the fruit, the attached stem and the seed by key (the fruit block and its attached stem follow vanilla's naming). */
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block> stemFruitKey(net.minecraft.world.level.block.Block fruit) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(fruit));
    }

    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block> stemAttachedKey(net.minecraft.world.level.block.Block fruit) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(fruit);
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, id.withPath("attached_" + id.getPath() + "_stem"));
    }

    /** The seed supplier is not resolvable while blocks register (the item does not exist yet): its item when it is, melon seeds otherwise (only the pick-block result and drops of the vanilla stem class use it). */
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item> stemSeedKey(java.util.function.Supplier<?> seed) {
        try {
            Object item = seed.get();
            if (item instanceof net.minecraft.world.item.Item i) {
                net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i);
                if (!id.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getDefaultKey())) return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id);
            }
        } catch (RuntimeException notYet) {
            // the item is not registered yet
        }
        return net.minecraft.world.item.Items.MELON_SEEDS.builtInRegistryHolder().key();
    }

    /** {@code SwordItem.getDamage()}: the attack damage the sword was built with, kept as its main hand attack damage modifier in 1.21.1. */
    public static float swordDamage(net.minecraft.world.item.SwordItem sword) {
        return attackDamage(sword);
    }

    /** {@code DiggerItem.getAttackDamage()}: the same, for pickaxes, axes, shovels and hoes. */
    public static float diggerDamage(net.minecraft.world.item.DiggerItem tool) {
        return attackDamage(tool);
    }

    private static float attackDamage(net.minecraft.world.item.Item item) {
        net.minecraft.world.item.component.ItemAttributeModifiers mods = item.components().get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        double sum = 0;
        if (mods != null) {
            for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry e : mods.modifiers()) {
                if (e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow())
                        && e.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) sum += e.modifier().amount();
            }
        }
        return (float) sum;
    }

    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block> stemOfFruitKey(net.minecraft.world.level.block.Block fruit) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(fruit);
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, id.withPath(id.getPath() + "_stem"));
    }

    /** {@code FoodProperties.isFastFood()}: the flag became the eating time (0.8 s for fast food against the default 1.6 s). */
    public static boolean isFastFood(net.minecraft.world.food.FoodProperties food) {
        return food.eatSeconds() < 1.6F;
    }

    private static volatile java.lang.reflect.Method conditionsGetter;

    /** {@code LootItemConditionalFunction.Builder.getConditions()}: protected, returned an array in 1.20.1 and a list in 1.21.1 (read through reflection, the caller is a mod class). */
    public static net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] builderConditions(net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction.Builder<?> builder) {
        try {
            java.lang.reflect.Method m = conditionsGetter;
            if (m == null) {
                m = net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction.Builder.class.getDeclaredMethod("getConditions");
                m.setAccessible(true);
                conditionsGetter = m;
            }
            @SuppressWarnings("unchecked")
            List<net.minecraft.world.level.storage.loot.predicates.LootItemCondition> list = (List<net.minecraft.world.level.storage.loot.predicates.LootItemCondition>) m.invoke(builder);
            return list.toArray(new net.minecraft.world.level.storage.loot.predicates.LootItemCondition[0]);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ dispenser: BlockSource became a record

    public static BlockPos sourcePos(net.minecraft.core.dispenser.BlockSource s) {
        return s.pos();
    }

    public static ServerLevel sourceLevel(net.minecraft.core.dispenser.BlockSource s) {
        return s.level();
    }

    public static net.minecraft.world.level.block.state.BlockState sourceState(net.minecraft.core.dispenser.BlockSource s) {
        return s.state();
    }

    public static net.minecraft.world.level.block.entity.BlockEntity sourceEntity(net.minecraft.core.dispenser.BlockSource s) {
        return s.blockEntity();
    }

    public static double sourceX(net.minecraft.core.dispenser.BlockSource s) {
        return s.pos().getX() + 0.5;
    }

    public static double sourceY(net.minecraft.core.dispenser.BlockSource s) {
        return s.pos().getY() + 0.5;
    }

    public static double sourceZ(net.minecraft.core.dispenser.BlockSource s) {
        return s.pos().getZ() + 0.5;
    }

    // ------------------------------------------------------------------ other signature changes

    /** {@code Commands.performPrefixedCommand(source, command)} returned the result; it returns nothing now. */
    public static int performPrefixedCommand(net.minecraft.commands.Commands commands, net.minecraft.commands.CommandSourceStack source, String command) {
        commands.performPrefixedCommand(source, command);
        return 1;
    }

    /** {@code ItemStack.getAttributeModifiers(slot)}: the modifiers of the attribute component that apply in that slot. */
    public static com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier>
            getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> out =
                com.google.common.collect.ArrayListMultimap.create();
        net.minecraft.world.item.component.ItemAttributeModifiers mods = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.slot().test(slot)) out.put(e.attribute().value(), e.modifier());
        }
        return out;
    }

    /** {@code EnchantedBookItem.getEnchantments(stack)}: the stored enchantments as the list of {@code {id, lvl}} compounds. */
    public static net.minecraft.nbt.ListTag getBookEnchantments(ItemStack stack) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        net.minecraft.world.item.enchantment.ItemEnchantments stored = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var e : stored.entrySet()) {
            CompoundTag tag = new CompoundTag();
            e.getKey().unwrapKey().ifPresent(k -> tag.putString("id", k.location().toString()));
            tag.putShort("lvl", (short) e.getIntValue());
            list.add(tag);
        }
        return list;
    }

    public static net.minecraft.world.scores.Team getTeam(Entity entity) {
        return entity.getTeam();
    }

    public static <T> void writeId(FriendlyByteBuf buf, net.minecraft.core.IdMap<T> map, T value) {
        buf.writeVarInt(map.getId(value));
    }

    public static <T> T readById(FriendlyByteBuf buf, net.minecraft.core.IdMap<T> map) {
        return map.byId(buf.readVarInt());
    }

    public static java.util.function.Predicate<ItemStack> asPredicate(Ingredient ingredient) {
        return ingredient;
    }

    public static net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings setKeepLiquids(
            net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings, boolean keep) {
        return settings.setLiquidSettings(keep ? net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING
                : net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING);
    }

    /**
     * {@code CriteriaTriggers.register(trigger)}: the trigger carried its id ({@code getId()}); 1.21.1 registers by name. The registry may be frozen when a mod
     * registers in common setup, as in {@link #registerLate}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.advancements.CriterionTrigger registerTrigger(net.minecraft.advancements.CriterionTrigger trigger) {
        String name;
        try {
            name = String.valueOf(trigger.getClass().getMethod("getId").invoke(trigger));
        } catch (ReflectiveOperationException | RuntimeException e) {
            name = trigger.getClass().getName().toLowerCase(java.util.Locale.ROOT).replace('.', '_').replace('$', '_');
        }
        net.minecraft.core.Registry registry = BuiltInRegistries.TRIGGER_TYPES;
        return (net.minecraft.advancements.CriterionTrigger) registerLate(registry, ResourceLocation.parse(name), trigger);
    }

    /**
     * {@code CriteriaTriggers.CRITERIA} (a mixin accessor of YUNG's API and others): the id-to-trigger map is the {@code TRIGGER_TYPES} registry in 1.21.1. A view
     * over it; {@code put} registers late (the registry may be frozen), the rest reads the registry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.Map<ResourceLocation, net.minecraft.advancements.CriterionTrigger<?>> criteriaTriggerMap() {
        net.minecraft.core.Registry<net.minecraft.advancements.CriterionTrigger<?>> registry = BuiltInRegistries.TRIGGER_TYPES;
        return new java.util.AbstractMap<>() {
            @Override public net.minecraft.advancements.CriterionTrigger<?> put(ResourceLocation key, net.minecraft.advancements.CriterionTrigger<?> value) {
                net.minecraft.advancements.CriterionTrigger<?> old = registry.get(key);
                if (old == null) registerLate((net.minecraft.core.Registry) registry, key, value);
                return old;
            }
            @Override public net.minecraft.advancements.CriterionTrigger<?> get(Object key) {
                return key instanceof ResourceLocation id ? registry.get(id) : null;
            }
            @Override public boolean containsKey(Object key) {
                return key instanceof ResourceLocation id && registry.containsKey(id);
            }
            @Override public int size() {
                return registry.size();
            }
            @Override public java.util.Set<java.util.Map.Entry<ResourceLocation, net.minecraft.advancements.CriterionTrigger<?>>> entrySet() {
                java.util.Set<java.util.Map.Entry<ResourceLocation, net.minecraft.advancements.CriterionTrigger<?>>> out = new java.util.LinkedHashSet<>();
                for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.advancements.CriterionTrigger<?>>, net.minecraft.advancements.CriterionTrigger<?>> e : registry.entrySet())
                    out.add(new java.util.AbstractMap.SimpleImmutableEntry<>(e.getKey().location(), e.getValue()));
                return java.util.Collections.unmodifiableSet(out);
            }
        };
    }

    // ---- cauldron interaction maps (CauldronInteraction.WATER... were Map<Item, CauldronInteraction>, are InteractionMap records now)

    public static java.util.Map<Item, net.minecraft.core.cauldron.CauldronInteraction> cauldronMap(net.minecraft.core.cauldron.CauldronInteraction.InteractionMap map) {
        return map.map();
    }

    /** {@code ItemStack.ATTRIBUTE_MODIFIER_FORMAT}: the format of attribute tooltips (removed in 1.21). */
    private static final java.text.DecimalFormat ATTRIBUTE_FORMAT = net.minecraft.Util.make(new java.text.DecimalFormat("#.##"),
            f -> f.setDecimalFormatSymbols(java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT)));

    public static java.text.DecimalFormat attributeModifierFormat() {
        return ATTRIBUTE_FORMAT;
    }

    public static net.minecraft.world.level.material.Fluid bucketFluid(net.minecraft.world.item.BucketItem bucket) {
        return bucket.content;
    }

    /** {@code MobEffectInstance.save(CompoundTag)}: writes the effect into the tag and returns it (1.21.1 encodes with a codec). */
    public static CompoundTag saveEffect(net.minecraft.world.effect.MobEffectInstance effect, CompoundTag tag) {
        Tag saved = effect.save();
        if (saved instanceof CompoundTag c) tag.merge(c);
        return tag;
    }

    private static int cauldronMaps;

    /** {@code CauldronInteraction.newInteractionMap()}: a map of a mod's own cauldron kind (1.21.1 names it and keeps it in {@code INTERACTIONS}). */
    public static it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Item, net.minecraft.core.cauldron.CauldronInteraction> newCauldronMap() {
        return (it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<Item, net.minecraft.core.cauldron.CauldronInteraction>)
                net.minecraft.core.cauldron.CauldronInteraction.newInteractionMap("legacy_connector_" + cauldronMaps++).map();
    }

    // ---- round 2 of the ATM 9 blockers

    /** {@code RandomizableContainerBlockEntity.setLootTable(level, random, pos, id)} (static): now on {@code RandomizableContainer}, with a resource key. */
    public static void setBlockEntityLootTable(net.minecraft.world.level.BlockGetter level, net.minecraft.util.RandomSource random, BlockPos pos, ResourceLocation table) {
        net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, random, pos,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table));
    }

    /** {@code setLootTable(ResourceLocation, long)} of a loot container block entity: the table key and the seed are set apart now. */
    public static void setLootTable(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container, ResourceLocation table, long seed) {
        container.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table));
        container.setLootTableSeed(seed);
    }

    /** {@code DataResult.get()}: the value on the left, the partial result with the error on the right. */
    public static <R> com.mojang.datafixers.util.Either<R, com.mojang.serialization.DataResult.Error<R>> dataResultGet(com.mojang.serialization.DataResult<R> result) {
        var value = result.result();
        if (value.isPresent()) return com.mojang.datafixers.util.Either.left(value.get());
        return com.mojang.datafixers.util.Either.right(result.error().get());
    }

    /** {@code Entity.canChangeDimensions()}: 1.21.1 asks for the two levels; the entity's own is the answer without a destination. */
    public static boolean canChangeDimensions(Entity entity) {
        return entity.canChangeDimensions(entity.level(), entity.level());
    }

    /** {@code Entity.getMyRidingOffset()}: replaced by the vehicle's attachment points; no offset here. */
    public static double myRidingOffset(Entity entity) {
        return 0.0D;
    }

    /** {@code EnchantmentHelper.enchantItem(random, stack, level, allowTreasure)}: the enchantments come from the server's registry now. */
    public static ItemStack enchantItem(net.minecraft.util.RandomSource random, ItemStack stack, int level, boolean allowTreasure) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return stack;
        var holders = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).holders()
                .<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>>map(h -> h)
                .filter(h -> allowTreasure || !h.is(net.minecraft.tags.EnchantmentTags.TREASURE));
        return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, stack, level, holders);
    }

    /** {@code ParticleTypes.ENTITY_EFFECT} is a colour particle option now; the closest plain particle is the witch swirl. */
    public static net.minecraft.core.particles.SimpleParticleType entityEffectParticle() {
        return net.minecraft.core.particles.ParticleTypes.WITCH;
    }

    /**
     * {@code MethodHandle.invokeExact(CapabilityProvider)} on a handle for {@code CapabilityProvider.serializeCaps()} (AE2's item keys read the stack's capability NBT
     * this way): in 1.20.1 an ItemStack was a CapabilityProvider; here it is not, and it has no persisted capabilities, so the result is null.
     */
    public static net.minecraft.nbt.CompoundTag serializeCapsHandle(java.lang.invoke.MethodHandle handle, Object provider) throws Throwable {
        if (provider instanceof legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.CapabilityProvider<?> cp) return (net.minecraft.nbt.CompoundTag) handle.invoke(cp);
        return null;
    }
}
