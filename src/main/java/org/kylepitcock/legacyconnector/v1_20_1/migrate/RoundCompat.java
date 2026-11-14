package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Helpers of 1.20.1 calls that 1.21.1 dropped (ATM 9 ranking round of 2026-11-08). */
public final class RoundCompat {
    private RoundCompat() {}

    /** {@code ItemStack.equals(ItemStack, boolean limitTags)}: same item, count and (components of) tags. */
    public static boolean stackEquals(ItemStack a, ItemStack b, boolean limitTags) {
        return ItemStack.matches(a, b);
    }

    /** {@code BuildCreativeModeTabContentsEvent.getEntries()}: the entries of the tab itself (the parent tab list), where mods put and reorder stacks. */
    public static net.neoforged.neoforge.common.util.MutableHashedLinkedMap<ItemStack, net.minecraft.world.item.CreativeModeTab.TabVisibility> tabEntries(
            BuildCreativeModeTabContentsEvent event) {
        return new LegacyTabEntries(event);
    }

    /** {@code IShearable.isShearable(ItemStack, Level, BlockPos)}: 1.21.1 also passes the shearing player (null: not known). */
    public static boolean isShearable(net.neoforged.neoforge.common.IShearable target, ItemStack stack, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos) {
        return target.isShearable(null, stack, level, pos);
    }

    public static java.util.List<ItemStack> onSheared(net.neoforged.neoforge.common.IShearable target, net.minecraft.world.entity.player.Player player,
            ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int fortune) {
        return target.onSheared(player, stack, level, pos);
    }

    /** The fortune level the 1.20.1 {@code onSheared} got as its last argument: read from the shears (the 1.21.1 method has no such argument). */
    public static int shearFortune(ItemStack shears) {
        return HolderCompat.stackEnchantmentLevel(shears, LegacyEnchantments.legacy(ench(Enchantments.FORTUNE)));
    }

    /** {@code BuiltInRegistries.POTION} / {@code PAINTING_VARIANT} were typed {@code DefaultedRegistry}; 1.21.1 types them {@code Registry} (the object still is one). */
    public static net.minecraft.core.DefaultedRegistry<?> defaulted(net.minecraft.core.Registry<?> registry) {
        if (registry instanceof net.minecraft.core.DefaultedRegistry<?> d) return d;
        // 1.21.1's potion and painting variant registries are plain registries (not "the object still is one"): the old defaulted type is a view that
        // delegates to the registry and knows the old default key
        return defaultedView(() -> registry, registry.key(), registry.key().location().getPath().equals("painting_variant") ? "kebab" : "empty");
    }

    /**
     * {@code IEnvironment.Keys.NAMING} (a supplier of the launcher key that names the mappings, "srg" or "mcp"): gone in the newer ModLauncher. A key nobody ever
     * sets is handed out, so a lookup finds no value (the mod then takes its non-development path), which is what a production game gives.
     */
    public static java.util.function.Supplier<?> launcherNamingKey() {
        return () -> {
            try {
                Class<?> launcher = Class.forName("cpw.mods.modlauncher.Launcher");
                Object board = launcher.getMethod("blackboard").invoke(launcher.getField("INSTANCE").get(null));
                Class<?> map = Class.forName("cpw.mods.modlauncher.api.TypesafeMap");
                return Class.forName("cpw.mods.modlauncher.api.TypesafeMap$Key").getMethod("getOrCreate", map, String.class, Class.class).invoke(null, board, "legacyconnector.naming", String.class);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    /** {@code BuiltInRegistries.PAINTING_VARIANT}: a data registry in 1.21.1 (no static field); read before a world exists, the view asks the registry on each call. */
    public static net.minecraft.core.DefaultedRegistry<?> paintingVariants() {
        return defaultedView(() -> MigrateContext.registries().registryOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT),
                net.minecraft.core.registries.Registries.PAINTING_VARIANT, "kebab");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.core.DefaultedRegistry<?> defaultedView(java.util.function.Supplier<net.minecraft.core.Registry<?>> registry,
            net.minecraft.resources.ResourceKey<?> key, String defaultName) {
        net.minecraft.resources.ResourceLocation def = net.minecraft.resources.ResourceLocation.withDefaultNamespace(defaultName);
        return (net.minecraft.core.DefaultedRegistry<?>) java.lang.reflect.Proxy.newProxyInstance(net.minecraft.core.DefaultedRegistry.class.getClassLoader(),
                new Class<?>[] {net.minecraft.core.DefaultedRegistry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getDefaultKey") && method.getParameterCount() == 0) return def;
                    if (method.getName().equals("key") && method.getParameterCount() == 0) return key;
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "registry view " + key.location();
                        };
                    }
                    try {
                        return method.invoke(registry.get(), args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /** A crafting grid in the 1.21.1 shape ({@code CraftingInput}) for the methods of {@code ShapedRecipe}/{@code ShapelessRecipe} that take one. */
    public static net.minecraft.world.item.crafting.CraftingInput craftInput(net.minecraft.world.Container container) {
        net.minecraft.world.item.crafting.RecipeInput in = BridgeHelpers.input(container);
        if (in instanceof net.minecraft.world.item.crafting.CraftingInput ci) return ci;
        int n = in.size();
        java.util.List<ItemStack> items = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) items.add(in.getItem(i));
        return net.minecraft.world.item.crafting.CraftingInput.of(n, 1, items);
    }

    /** Same, for a call whose argument is typed {@code CraftingContainer} (the descriptor of the rewritten call names that type). */
    public static net.minecraft.world.item.crafting.CraftingInput craftInput(net.minecraft.world.inventory.CraftingContainer container) {
        return craftInput((net.minecraft.world.Container) container);
    }

    /** The {@code CraftingContainer} a 1.20.1 recipe override expects, from the 1.21.1 grid. */
    public static net.minecraft.world.inventory.CraftingContainer craftingContainer(net.minecraft.world.item.crafting.CraftingInput input) {
        return (net.minecraft.world.inventory.CraftingContainer) BridgeHelpers.container(input);
    }

    /** The {@code scale} argument 1.21.1 added to {@code LivingEntityRenderer.setupRotations}: the 1.20.1 call had none, so it is 1. */
    public static float defaultScale(float partialTick) {
        return 1.0F;
    }

    /** {@code new ClientboundAddEntityPacket(Entity)}: 1.21.1 builds it from the entity's tracker entry; a detached one is enough for the packet data. */
    public static net.minecraft.network.protocol.game.ClientboundAddEntityPacket addEntityPacket(net.minecraft.world.entity.Entity entity) {
        net.minecraft.server.level.ServerEntity tracker = new net.minecraft.server.level.ServerEntity((net.minecraft.server.level.ServerLevel) entity.level(), entity,
                entity.getType().updateInterval(), entity.getType().trackDeltas(), p -> { });
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity, tracker);
    }

    /** {@code NbtUtils.writeGameProfile}: name, id and the properties (value and signature) in the 1.20.1 layout. */
    public static CompoundTag writeGameProfile(CompoundTag tag, com.mojang.authlib.GameProfile profile) {
        if (profile.getName() != null && !profile.getName().isEmpty()) tag.putString("Name", profile.getName());
        if (profile.getId() != null) tag.putUUID("Id", profile.getId());
        if (!profile.getProperties().isEmpty()) {
            CompoundTag props = new CompoundTag();
            for (String key : profile.getProperties().keySet()) {
                ListTag list = new ListTag();
                for (com.mojang.authlib.properties.Property p : profile.getProperties().get(key)) {
                    CompoundTag e = new CompoundTag();
                    e.putString("Value", p.value());
                    if (p.hasSignature()) e.putString("Signature", p.signature());
                    list.add(e);
                }
                props.put(key, list);
            }
            tag.put("Properties", props);
        }
        return tag;
    }

    /** {@code NbtUtils.readGameProfile}: null when the tag has neither name nor id. */
    public static com.mojang.authlib.GameProfile readGameProfile(CompoundTag tag) {
        String name = tag.contains("Name", 8) ? tag.getString("Name") : "";
        java.util.UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name);
        if (name.isEmpty() && !tag.hasUUID("Id")) return null;
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(id, name);
        if (tag.contains("Properties", 10)) {
            CompoundTag props = tag.getCompound("Properties");
            for (String key : props.getAllKeys()) {
                for (Tag t : props.getList(key, 10)) {
                    CompoundTag e = (CompoundTag) t;
                    profile.getProperties().put(key, e.contains("Signature", 8)
                            ? new com.mojang.authlib.properties.Property(key, e.getString("Value"), e.getString("Signature"))
                            : new com.mojang.authlib.properties.Property(key, e.getString("Value")));
                }
            }
        }
        return profile;
    }

    // ------------------------------------------------------------------ ingredients (1.21.1: Ingredient is final, NBT ingredients are data component ingredients)

    /** {@code StrictNBTIngredient.of(ItemStack)}: the item and all of its data (the NBT of 1.20.1 is the custom data component here). */
    public static net.minecraft.world.item.crafting.Ingredient strictIngredient(ItemStack stack) {
        return net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true, stack);
    }

    /** {@code PartialNBTIngredient.of(Item, CompoundTag)}: the item whose NBT contains the given tag. */
    public static net.minecraft.world.item.crafting.Ingredient partialIngredient(net.minecraft.world.item.Item item, CompoundTag nbt) {
        return net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(nbt), item);
    }

    public static net.minecraft.world.item.crafting.Ingredient partialIngredient(CompoundTag nbt, net.minecraft.world.level.ItemLike... items) {
        return net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(nbt), items);
    }

    // ------------------------------------------------------------------ recipe holders (1.21.1 hands out RecipeHolder: id plus recipe)

    private static final ResourceLocation UNKNOWN_RECIPE = ResourceLocation.fromNamespaceAndPath("legacyconnector", "unknown");

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static net.minecraft.world.item.crafting.RecipeHolder holderOf(net.minecraft.world.item.crafting.Recipe recipe) {
        if (recipe == null) return null;
        ResourceLocation id = RecipeCompat.getId(recipe);
        return new net.minecraft.world.item.crafting.RecipeHolder(id != null ? id : UNKNOWN_RECIPE, recipe);
    }

    /** {@code RecipeManager.byType(type)}: recipe id to recipe. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe> byType(net.minecraft.world.item.crafting.RecipeManager manager,
            net.minecraft.world.item.crafting.RecipeType type) {
        Map<ResourceLocation, net.minecraft.world.item.crafting.Recipe> out = new LinkedHashMap<>();
        for (Object o : manager.getAllRecipesFor(type)) {
            net.minecraft.world.item.crafting.RecipeHolder h = (net.minecraft.world.item.crafting.RecipeHolder) o;
            out.put(h.id(), h.value());
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static java.util.Optional cachedGetRecipeFor(net.minecraft.world.item.crafting.RecipeManager.CachedCheck check, net.minecraft.world.Container container,
            net.minecraft.world.level.Level level) {
        return ((java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder>) check.getRecipeFor(BridgeHelpers.input(container), level))
                .map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static net.minecraft.core.NonNullList<ItemStack> getRemainingItemsFor(net.minecraft.world.item.crafting.RecipeManager manager,
            net.minecraft.world.item.crafting.RecipeType type, net.minecraft.world.Container container, net.minecraft.world.level.Level level) {
        return manager.getRemainingItemsFor(type, BridgeHelpers.input(container), level);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setRecipeUsed(net.minecraft.world.inventory.RecipeCraftingHolder holder, net.minecraft.world.item.crafting.Recipe recipe) {
        holder.setRecipeUsed(holderOf(recipe));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean setRecipeUsed(net.minecraft.world.inventory.RecipeCraftingHolder holder, net.minecraft.world.level.Level level,
            net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.crafting.Recipe recipe) {
        return holder.setRecipeUsed(level, player, holderOf(recipe));
    }

    public static net.minecraft.world.item.crafting.Recipe<?> getRecipeUsed(net.minecraft.world.inventory.RecipeCraftingHolder holder) {
        net.minecraft.world.item.crafting.RecipeHolder<?> h = holder.getRecipeUsed();
        return h == null ? null : h.value();
    }

    // ------------------------------------------------------------------ small signature changes

    /** {@code new Explosion(level, source, damageSource, calculator, x, y, z, radius, fire, interaction)}: 1.21.1 also takes the particles and the sound. */
    public static net.minecraft.world.level.Explosion newExplosion(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity source,
            net.minecraft.world.damagesource.DamageSource damage, net.minecraft.world.level.ExplosionDamageCalculator calculator, double x, double y, double z,
            float radius, boolean fire, net.minecraft.world.level.Explosion.BlockInteraction interaction) {
        return new net.minecraft.world.level.Explosion(level, source, damage, calculator, x, y, z, radius, fire, interaction,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);
    }

    /** {@code new Explosion(level, source, x, y, z, radius, positions)}. */
    public static net.minecraft.world.level.Explosion newExplosion(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity source,
            double x, double y, double z, float radius, java.util.List<net.minecraft.core.BlockPos> positions) {
        return new net.minecraft.world.level.Explosion(level, source, x, y, z, radius, false,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY, positions);
    }

    /** {@code ItemEntity.setThrower(UUID)}: 1.21.1 takes the entity (looked up when it is loaded). */
    public static void setThrower(net.minecraft.world.entity.item.ItemEntity item, java.util.UUID uuid) {
        if (uuid == null) {
            item.setThrower((net.minecraft.world.entity.Entity) null);
            return;
        }
        net.minecraft.world.level.Level level = item.level();
        net.minecraft.world.entity.Entity thrower = level instanceof net.minecraft.server.level.ServerLevel sl ? sl.getEntity(uuid) : level.getPlayerByUUID(uuid);
        if (thrower != null) item.setThrower(thrower);
    }

    /** {@code new BlockSetType(name, canOpenByHand, sounds...)}: the flags 1.21.1 added take the vanilla wooden defaults. */
    public static net.minecraft.world.level.block.state.properties.BlockSetType newBlockSetType(String name, boolean canOpenByHand,
            net.minecraft.world.level.block.SoundType soundType, net.minecraft.sounds.SoundEvent doorClose, net.minecraft.sounds.SoundEvent doorOpen,
            net.minecraft.sounds.SoundEvent trapdoorClose, net.minecraft.sounds.SoundEvent trapdoorOpen, net.minecraft.sounds.SoundEvent plateOff,
            net.minecraft.sounds.SoundEvent plateOn, net.minecraft.sounds.SoundEvent buttonOff, net.minecraft.sounds.SoundEvent buttonOn) {
        return new net.minecraft.world.level.block.state.properties.BlockSetType(name, canOpenByHand, canOpenByHand, true,
                net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity.EVERYTHING, soundType, doorClose, doorOpen,
                trapdoorClose, trapdoorOpen, plateOff, plateOn, buttonOff, buttonOn);
    }

    /** {@code new PaintingVariant(width, height)}: the texture is named after the variant by 1.21.1; unknown at construction, so the vanilla placeholder. */
    public static net.minecraft.world.entity.decoration.PaintingVariant newPaintingVariant(int width, int height) {
        return new net.minecraft.world.entity.decoration.PaintingVariant(width, height, ResourceLocation.withDefaultNamespace("kebab"));
    }

    /** {@code new BannerPattern(hashname)}: the asset id is the hash name in the layer's namespace. */
    public static net.minecraft.world.level.block.entity.BannerPattern newBannerPattern(String hashName) {
        return new net.minecraft.world.level.block.entity.BannerPattern(ResourceLocation.fromNamespaceAndPath("legacyconnector", hashName),
                "block.legacyconnector.banner." + hashName);
    }

    /** {@code Rarity.create(name, color)}: enum constants cannot be added at runtime here, the nearest vanilla rarity stands in. */
    public static net.minecraft.world.item.Rarity createRarity(String name, net.minecraft.ChatFormatting color) {
        return switch (color) {
            case AQUA, BLUE, DARK_AQUA, DARK_BLUE -> net.minecraft.world.item.Rarity.RARE;
            case LIGHT_PURPLE, DARK_PURPLE, RED, DARK_RED -> net.minecraft.world.item.Rarity.EPIC;
            case YELLOW, GOLD, GREEN, DARK_GREEN -> net.minecraft.world.item.Rarity.UNCOMMON;
            default -> net.minecraft.world.item.Rarity.COMMON;
        };
    }

    /** {@code LootTableReference.lootTableReference(ResourceLocation)}: 1.21.1 names loot tables by resource key ({@code NestedLootTable}). */
    public static net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceLocation id) {
        return net.minecraft.world.level.storage.loot.entries.NestedLootTable.lootTableReference(
                net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, id));
    }

    /** Registration-time type constructors that took a {@code Codec} (trunk placers, tree decorators): 1.21.1 wants the map form. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static com.mojang.serialization.MapCodec mapCodec(com.mojang.serialization.Codec codec) {
        return com.mojang.serialization.MapCodec.assumeMapUnsafe(codec);
    }

    /** {@code new SmithingTemplateItem(5 components, 2 icon lists)}: 1.21.1 only adds the (empty) feature flag varargs. */
    public static net.minecraft.world.item.SmithingTemplateItem newSmithingTemplate(net.minecraft.network.chat.Component a, net.minecraft.network.chat.Component b,
            net.minecraft.network.chat.Component c, net.minecraft.network.chat.Component d, net.minecraft.network.chat.Component e,
            java.util.List<ResourceLocation> base, java.util.List<ResourceLocation> additions) {
        return new net.minecraft.world.item.SmithingTemplateItem(a, b, c, d, e, base, additions);
    }

    /** {@code new ServerGamePacketListenerImpl(server, connection, player)}: 1.21.1 also takes the listener cookie. */
    public static net.minecraft.server.network.ServerGamePacketListenerImpl newGameListener(net.minecraft.server.MinecraftServer server,
            net.minecraft.network.Connection connection, net.minecraft.server.level.ServerPlayer player) {
        return new net.minecraft.server.network.ServerGamePacketListenerImpl(server, connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    /** {@code TextColor.parseColor(String)}: a {@code DataResult} now; null for text that is no colour, as before. */
    public static net.minecraft.network.chat.TextColor parseColor(String text) {
        return net.minecraft.network.chat.TextColor.parseColor(text).result().orElse(null);
    }

    /** {@code EntityType.updateCustomEntityTag(level, player, entity, tag)}: the tag is custom data now. */
    public static void updateCustomEntityTag(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity entity, CompoundTag tag) {
        net.minecraft.world.entity.EntityType.updateCustomEntityTag(level, player, entity, net.minecraft.world.item.component.CustomData.of(tag));
    }

    /** {@code GoalSelector.getRunningGoals()}. */
    public static java.util.stream.Stream<net.minecraft.world.entity.ai.goal.WrappedGoal> getRunningGoals(net.minecraft.world.entity.ai.goal.GoalSelector selector) {
        return selector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning);
    }

    /** {@code EnchantedBookItem.addEnchantment(book, instance)}: stored enchantments, keeping the higher level. */
    public static void addBookEnchantment(ItemStack book, EnchantmentInstance instance) {
        var stored = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
                book.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));
        stored.upgrade(instance.enchantment, instance.level);
        book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
    }

    /** {@code SkullBlockEntity.updateGameprofile(profile, callback)}: fetched through the profile cache by id, answering the given profile when none is found. */
    public static void updateGameprofile(com.mojang.authlib.GameProfile profile, java.util.function.Consumer<com.mojang.authlib.GameProfile> callback) {
        if (profile == null || profile.getId() == null) {
            callback.accept(profile);
            return;
        }
        net.minecraft.world.level.block.entity.SkullBlockEntity.fetchGameProfile(profile.getId())
                .thenAccept(found -> callback.accept(found.orElse(profile)));
    }

    // ---- loot, spawning, capabilities, arrows ----

    /** {@code LootContext.Builder.create(ResourceLocation)}: the random sequence is optional now. */
    public static net.minecraft.world.level.storage.loot.LootContext createLootContext(net.minecraft.world.level.storage.loot.LootContext.Builder builder, ResourceLocation sequence) {
        return builder.create(java.util.Optional.ofNullable(sequence));
    }

    /** {@code ItemStack.areCapsCompatible(provider)}: capabilities are not attached to stacks here, so two stacks always agree. */
    public static boolean areCapsCompatible(ItemStack stack, Object other) {
        return true;
    }

    /** {@code NaturalSpawner.isSpawnPositionOk(type, level, pos, entityType)}: a method of the placement type now. */
    public static boolean isSpawnPositionOk(net.minecraft.world.entity.SpawnPlacementType type, net.minecraft.world.level.LevelReader level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.EntityType<?> entityType) {
        return type.isSpawnPositionOk(level, pos, entityType);
    }

    /** {@code ForgeEventFactory.onFinalizeSpawn(mob, level, difficulty, reason, data, tag)}: the tag moved into the spawn data of the mob. */
    public static net.minecraft.world.entity.SpawnGroupData onFinalizeSpawn(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data,
            CompoundTag tag) {
        return net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, difficulty, reason, data);
    }

    public static ItemStack noStack(net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        return ItemStack.EMPTY;
    }

    // ---- advancement predicates ----

    /** {@code MinMaxBounds.Doubles.fromJson(element)}: the codec decodes it now; no element is "any". */
    public static net.minecraft.advancements.critereon.MinMaxBounds.Doubles doublesFromJson(com.google.gson.JsonElement json) {
        if (json == null || json.isJsonNull()) return net.minecraft.advancements.critereon.MinMaxBounds.Doubles.ANY;
        return net.minecraft.advancements.critereon.MinMaxBounds.Doubles.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(com.google.gson.JsonSyntaxException::new);
    }

    public static net.minecraft.advancements.critereon.MinMaxBounds.Ints intsFromJson(com.google.gson.JsonElement json) {
        if (json == null || json.isJsonNull()) return net.minecraft.advancements.critereon.MinMaxBounds.Ints.ANY;
        return net.minecraft.advancements.critereon.MinMaxBounds.Ints.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(com.google.gson.JsonSyntaxException::new);
    }


    /** {@code new EnchantmentPredicate(enchantment, levels)}: a set of enchantments in 1.21.1. */
    public static net.minecraft.advancements.critereon.EnchantmentPredicate newEnchantmentPredicate(LegacyEnchantment enchantment,
            net.minecraft.advancements.critereon.MinMaxBounds.Ints levels) {
        return new net.minecraft.advancements.critereon.EnchantmentPredicate(
                java.util.Optional.of(net.minecraft.core.HolderSet.direct(HolderCompat.holder(enchantment))), levels);
    }

    /** {@code ItemPredicate.Builder.hasEnchantment(predicate)}: a sub predicate on the item's enchantments. */
    public static net.minecraft.advancements.critereon.ItemPredicate.Builder hasEnchantment(net.minecraft.advancements.critereon.ItemPredicate.Builder builder,
            net.minecraft.advancements.critereon.EnchantmentPredicate predicate) {
        return builder.withSubPredicate(net.minecraft.advancements.critereon.ItemSubPredicates.ENCHANTMENTS,
                net.minecraft.advancements.critereon.ItemEnchantmentsPredicate.enchantments(java.util.List.of(predicate)));
    }

    /** {@code CraftingBookCategory.CODEC} is typed as the plain codec now (the object is still the enum codec). */
    @SuppressWarnings("rawtypes")
    public static net.minecraft.util.StringRepresentable.EnumCodec craftingCategoryCodec(com.mojang.serialization.Codec codec) {
        return (net.minecraft.util.StringRepresentable.EnumCodec) codec;
    }

    // ---- text, saved data, brewing, small queries ----

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.level.saveddata.SavedData> T getSavedData(net.minecraft.world.level.storage.DimensionDataStorage storage,
            java.util.function.Function<CompoundTag, T> loader, String name) {
        return storage.get(new net.minecraft.world.level.saveddata.SavedData.Factory<>(() -> {
            throw new IllegalStateException("saved data " + name + " is only read here");
        }, (tag, provider) -> {
            MigrateContext.remember(provider);
            return loader.apply(tag);
        }, null), name);
    }

    public static void saveSavedData(net.minecraft.world.level.saveddata.SavedData data, java.io.File file) {
        data.save(file, MigrateContext.provider());
    }

    public static net.minecraft.network.chat.MutableComponent fromJsonLenient(String json) {
        return net.minecraft.network.chat.Component.Serializer.fromJsonLenient(json, MigrateContext.provider());
    }

    public static String toStableJson(net.minecraft.network.chat.Component component) {
        return net.minecraft.network.chat.Component.Serializer.toJson(component, MigrateContext.provider());
    }

    public static com.google.gson.JsonElement toJsonTree(net.minecraft.network.chat.Component component) {
        return com.google.gson.JsonParser.parseString(net.minecraft.network.chat.Component.Serializer.toJson(component, MigrateContext.provider()));
    }

    /** {@code FoodProperties.isMeat()}: the flag is gone (wolves ask the item tag), so no food answers true here. */
    public static boolean isMeat(net.minecraft.world.food.FoodProperties food) {
        return false;
    }

    private static final java.util.List<Object[]> PENDING_MIXES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile boolean brewingHooked;

    /** {@code PotionBrewing.addMix(from, ingredient, to)} (static in 1.20.1): queued, and added when NeoForge asks for brewing recipes. */
    public static void addPotionMix(net.minecraft.world.item.alchemy.Potion from, net.minecraft.world.item.Item ingredient, net.minecraft.world.item.alchemy.Potion to) {
        PENDING_MIXES.add(new Object[] {from, ingredient, to});
        if (!brewingHooked) {
            brewingHooked = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent.class, event -> {
                for (Object[] m : PENDING_MIXES) {
                    event.getBuilder().addMix(net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder((net.minecraft.world.item.alchemy.Potion) m[0]), (net.minecraft.world.item.Item) m[1],
                            net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder((net.minecraft.world.item.alchemy.Potion) m[2]));
                }
            });
        }
    }

    // ---- argument producers for in-place constructor rewrites (=args rows; usable in super(...) calls) ----

    public static net.minecraft.world.phys.Vec3 vec3(double x, double y, double z) {
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    public static net.minecraft.util.ColorRGBA colorRgba(int color) {
        return new net.minecraft.util.ColorRGBA(color);
    }

    public static net.minecraft.core.particles.ParticleOptions explosionParticle(net.minecraft.world.level.Level level) {
        return net.minecraft.core.particles.ParticleTypes.EXPLOSION;
    }

    public static net.minecraft.core.particles.ParticleOptions emitterParticle(net.minecraft.world.level.Level level) {
        return net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER;
    }

    public static Holder<net.minecraft.sounds.SoundEvent> explodeSound(net.minecraft.world.level.Level level) {
        return net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE;
    }

    public static boolean noFire(net.minecraft.world.level.Level level) {
        return false;
    }

    public static net.minecraft.world.level.Explosion.BlockInteraction decay(net.minecraft.world.level.Level level) {
        return net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY;
    }

    public static net.minecraft.server.network.CommonListenerCookie cookie(net.minecraft.server.level.ServerPlayer player) {
        return net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false);
    }

    /** {@code Rarity.create(name, styleModifier)}: the colour the modifier gives a plain style picks the nearest vanilla rarity. */
    public static net.minecraft.world.item.Rarity createRarityStyled(String name, java.util.function.UnaryOperator<net.minecraft.network.chat.Style> modifier) {
        net.minecraft.network.chat.TextColor color = modifier.apply(net.minecraft.network.chat.Style.EMPTY).getColor();
        net.minecraft.ChatFormatting nearest = net.minecraft.ChatFormatting.WHITE;
        if (color != null) {
            int best = Integer.MAX_VALUE;
            for (net.minecraft.ChatFormatting f : net.minecraft.ChatFormatting.values()) {
                if (!f.isColor() || f.getColor() == null) continue;
                int d = Math.abs(((color.getValue() >> 16) & 255) - ((f.getColor() >> 16) & 255)) + Math.abs(((color.getValue() >> 8) & 255) - ((f.getColor() >> 8) & 255))
                        + Math.abs((color.getValue() & 255) - (f.getColor() & 255));
                if (d < best) {
                    best = d;
                    nearest = f;
                }
            }
        }
        return createRarity(name, nearest);
    }

    // ---- Forge enum extension ({@code Enum.create(...)}): constants cannot be added at runtime, the nearest vanilla one stands in ----

    /** {@code MobCategory.create(name, key, max, friendly, persistent, despawn)}: friendly persistent mobs count as creatures, friendly ones as ambient, the rest as monsters. */
    public static net.minecraft.world.entity.MobCategory createMobCategory(String name, String key, int max, boolean friendly, boolean persistent, int despawn) {
        return friendly ? (persistent ? net.minecraft.world.entity.MobCategory.CREATURE : net.minecraft.world.entity.MobCategory.AMBIENT)
                : net.minecraft.world.entity.MobCategory.MONSTER;
    }

    /** {@code SpawnPlacements.Type}-style custom placement: a placement type that asks the predicate. */
    public static net.minecraft.world.entity.SpawnPlacementType createSpawnPlacementType(String name,
            net.neoforged.neoforge.common.util.TriPredicate<net.minecraft.world.level.LevelReader, net.minecraft.core.BlockPos, net.minecraft.world.entity.EntityType<?>> predicate) {
        return predicate::test;
    }

    public static net.minecraft.world.entity.raid.Raid.RaiderType createRaiderType(String name, net.minecraft.world.entity.EntityType<?> type, int[] counts) {
        return net.minecraft.world.entity.raid.Raid.RaiderType.VINDICATOR;
    }

    public static net.minecraft.world.level.pathfinder.PathType createPathType(String name, float malus) {
        return malus < 0 ? net.minecraft.world.level.pathfinder.PathType.BLOCKED : net.minecraft.world.level.pathfinder.PathType.OPEN;
    }

    public static net.minecraft.world.item.ItemDisplayContext createDisplayContext(String name, ResourceLocation id, net.minecraft.world.item.ItemDisplayContext parent) {
        return parent != null ? parent : net.minecraft.world.item.ItemDisplayContext.NONE;
    }

    /** The (empty) feature flag varargs 1.21.1 added to {@code SmithingTemplateItem.createArmorTrimTemplate}. */
    public static net.minecraft.world.flag.FeatureFlag[] noFlags(ResourceLocation ignored) {
        return new net.minecraft.world.flag.FeatureFlag[0];
    }

    /** {@code new SimpleCookingSerializer(baker, cookingTime)}: the factory of 1.21.1 has no id; the recipe is made with the unknown id. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.item.crafting.SimpleCookingSerializer newCookingSerializer(LegacyCookieBaker baker, int cookingTime) {
        return new net.minecraft.world.item.crafting.SimpleCookingSerializer(
                (net.minecraft.world.item.crafting.AbstractCookingRecipe.Factory) (group, category, ingredient, result, xp, time) ->
                        baker.create(UNKNOWN_RECIPE, (String) group, (net.minecraft.world.item.crafting.CookingBookCategory) category,
                                (net.minecraft.world.item.crafting.Ingredient) ingredient, (ItemStack) result, (Float) xp, (Integer) time), cookingTime);
    }

    /** {@code BlockParticleOption.codec(type)}: a {@code MapCodec} now; the 1.20.1 form is the plain codec of it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static com.mojang.serialization.Codec blockParticleCodec(net.minecraft.core.particles.ParticleType type) {
        return net.minecraft.core.particles.BlockParticleOption.codec(type).codec();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static com.mojang.serialization.Codec itemParticleCodec(net.minecraft.core.particles.ParticleType type) {
        return net.minecraft.core.particles.ItemParticleOption.codec(type).codec();
    }

    /** {@code BlockParticleOption.DESERIALIZER}: the field is gone; particle types drop their deserializer on construction anyway. */
    public static LegacyParticleDeserializer noParticleDeserializer() {
        return null;
    }

    /** {@code Frog.getVariant()} is a holder now. */
    public static net.minecraft.world.entity.animal.FrogVariant frogVariant(net.minecraft.world.entity.animal.frog.Frog frog) {
        return frog.getVariant().value();
    }

    /** {@code AreaEffectCloud.setFixedColor(int)}: the colour is part of the potion contents. */
    public static void setFixedColor(net.minecraft.world.entity.AreaEffectCloud cloud, int color) {
        cloud.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(), java.util.Optional.of(color), java.util.List.of()));
    }

    /** {@code new EntityDimensions(width, height, fixed)}: the constructor is private now, the factories make the same sizes. */
    public static net.minecraft.world.entity.EntityDimensions newEntityDimensions(float width, float height, boolean fixed) {
        return fixed ? net.minecraft.world.entity.EntityDimensions.fixed(width, height) : net.minecraft.world.entity.EntityDimensions.scalable(width, height);
    }

    /** {@code new SimpleCraftingRecipeSerializer(factory)}: the 1.21.1 factory takes the category only. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer newCraftingSerializer(LegacyCraftingFactory factory) {
        return new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer(
                (net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer.Factory) category ->
                        factory.create(UNKNOWN_RECIPE, (net.minecraft.world.item.crafting.CraftingBookCategory) category));
    }

    /** {@code LootItemConditions.orConditions(predicates)}: true when any predicate is. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.function.Predicate orConditions(java.util.function.Predicate[] predicates) {
        return t -> {
            for (java.util.function.Predicate p : predicates) if (p.test(t)) return true;
            return false;
        };
    }

    /** {@code LootItemConditions.andConditions(predicates)}: true when all predicates are. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.function.Predicate andConditions(java.util.function.Predicate[] predicates) {
        return t -> {
            for (java.util.function.Predicate p : predicates) if (!p.test(t)) return false;
            return true;
        };
    }

    // ---- food and tempt ingredients of animals (static fields in 1.20.1; tags and lists inside the entities now) ----

    private static net.minecraft.world.item.crafting.Ingredient ingredient(net.minecraft.world.level.ItemLike... items) {
        return net.minecraft.world.item.crafting.Ingredient.of(items);
    }

    public static net.minecraft.world.item.crafting.Ingredient chickenFood() {
        return ingredient(net.minecraft.world.item.Items.WHEAT_SEEDS, net.minecraft.world.item.Items.MELON_SEEDS, net.minecraft.world.item.Items.PUMPKIN_SEEDS,
                net.minecraft.world.item.Items.BEETROOT_SEEDS, net.minecraft.world.item.Items.TORCHFLOWER_SEEDS, net.minecraft.world.item.Items.PITCHER_POD);
    }

    /** {@code Parrot.TAME_FOOD}: a set mods add their seeds to; 1.21.1 reads the item tag instead, so additions have no effect. */
    public static java.util.Set<net.minecraft.world.item.Item> parrotTameFood() {
        return new java.util.HashSet<>(java.util.List.of(net.minecraft.world.item.Items.WHEAT_SEEDS, net.minecraft.world.item.Items.MELON_SEEDS,
                net.minecraft.world.item.Items.PUMPKIN_SEEDS, net.minecraft.world.item.Items.BEETROOT_SEEDS, net.minecraft.world.item.Items.TORCHFLOWER_SEEDS,
                net.minecraft.world.item.Items.PITCHER_POD));
    }

    public static net.minecraft.world.item.crafting.Ingredient catTempt() {
        return ingredient(net.minecraft.world.item.Items.COD, net.minecraft.world.item.Items.SALMON);
    }

    public static net.minecraft.world.item.crafting.Ingredient pigFood() {
        return ingredient(net.minecraft.world.item.Items.CARROT, net.minecraft.world.item.Items.POTATO, net.minecraft.world.item.Items.BEETROOT);
    }

    public static net.minecraft.world.item.crafting.Ingredient horseFood() {
        return ingredient(net.minecraft.world.item.Items.WHEAT, net.minecraft.world.item.Items.SUGAR, net.minecraft.world.level.block.Blocks.HAY_BLOCK,
                net.minecraft.world.item.Items.APPLE, net.minecraft.world.item.Items.GOLDEN_CARROT, net.minecraft.world.item.Items.GOLDEN_APPLE,
                net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
    }

    public static net.minecraft.world.item.crafting.Ingredient striderFood() {
        return ingredient(net.minecraft.world.item.Items.WARPED_FUNGUS);
    }

    public static net.minecraft.world.item.crafting.Ingredient striderTempt() {
        return ingredient(net.minecraft.world.item.Items.WARPED_FUNGUS, net.minecraft.world.item.Items.WARPED_FUNGUS_ON_A_STICK);
    }

    // ---- Architectury 9's interface-injected members of registry objects (added by its mixins to the 1.20.1 classes) ----

    public static ResourceLocation archRegistryName(net.minecraft.world.entity.EntityType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static ResourceLocation archRegistryName(net.minecraft.world.level.material.Fluid fluid) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
    }

    public static ResourceLocation archRegistryName(net.minecraft.world.item.Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    }

    public static ResourceLocation archRegistryName(net.minecraft.world.level.block.Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
    }

    public static net.minecraft.world.level.material.Fluid archGetFluid(net.minecraft.world.item.BucketItem bucket) {
        return bucket.content;
    }

    /** {@code Item.Properties.arch$tab(...)}: the creative tab is chosen by the tab's own contents event here; the properties are returned unchanged. */
    public static net.minecraft.world.item.Item.Properties archTab(net.minecraft.world.item.Item.Properties properties, Object tab) {
        return properties;
    }

    /** {@code SynchedEntityData.hasItem(accessor)} (gone in 1.21.1): whether the entity has data for the accessor. */
    public static boolean hasItem(net.minecraft.network.syncher.SynchedEntityData data, net.minecraft.network.syncher.EntityDataAccessor<?> accessor) {
        try {
            data.get(accessor);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** {@code EntityDataSerializer.simpleEnum(Class)}: an enum value written by ordinal. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.network.syncher.EntityDataSerializer simpleEnum(Class enumClass) {
        return net.minecraft.network.syncher.EntityDataSerializer.forValueType(net.minecraft.network.codec.StreamCodec.<net.minecraft.network.RegistryFriendlyByteBuf, Enum>of(
                (buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(enumClass)));
    }

    /** {@code EntityDataSerializer.simple(writer, reader)}. */
    public static <T> net.minecraft.network.syncher.EntityDataSerializer<T> simpleSerializer(
            net.minecraft.network.codec.StreamEncoder<net.minecraft.network.FriendlyByteBuf, T> writer,
            net.minecraft.network.codec.StreamDecoder<net.minecraft.network.FriendlyByteBuf, T> reader) {
        return net.minecraft.network.syncher.EntityDataSerializer.forValueType(net.minecraft.network.codec.StreamCodec.<net.minecraft.network.RegistryFriendlyByteBuf, T>of(
                (buf, value) -> writer.encode(buf, value), buf -> reader.decode(buf)));
    }

    /** {@code EntityDataSerializer.optional(writer, reader)} of 1.20.1: an optional value written as a presence flag and the value. */
    public static <T> net.minecraft.network.syncher.EntityDataSerializer<java.util.Optional<T>> optionalSerializer(
            net.minecraft.network.codec.StreamEncoder<net.minecraft.network.FriendlyByteBuf, T> writer,
            net.minecraft.network.codec.StreamDecoder<net.minecraft.network.FriendlyByteBuf, T> reader) {
        return net.minecraft.network.syncher.EntityDataSerializer.forValueType(net.minecraft.network.codec.StreamCodec.<net.minecraft.network.RegistryFriendlyByteBuf, java.util.Optional<T>>of(
                (buf, value) -> {
                    buf.writeBoolean(value.isPresent());
                    value.ifPresent(v -> writer.encode(buf, v));
                }, buf -> buf.readBoolean() ? java.util.Optional.of(reader.decode(buf)) : java.util.Optional.empty()));
    }

    /** {@code ComponentContents.EMPTY} moved to {@code PlainTextContents.EMPTY}. */
    public static net.minecraft.network.chat.ComponentContents emptyContents() {
        return net.minecraft.network.chat.contents.PlainTextContents.EMPTY;
    }

    // ------------------------------------------------------------------ blocks and entities (signatures that lost or gained arguments)

    /** The constant {@code true}, as an argument built from an old one (1.21.1 methods that gained an {@code includeSideEffects} flag). */
    public static boolean yes(boolean ignored) {
        return true;
    }

    /** {@code TamableAnimal.setTame(boolean)}: 1.21.1 adds the side effects flag (the 1.20.1 method always applied them). */
    public static void setTame(net.minecraft.world.entity.TamableAnimal animal, boolean tame) {
        animal.setTame(tame, true);
    }

    /** {@code BlockState.isPathfindable(level, pos, type)}: the state alone decides in 1.21.1. */
    public static boolean isPathfindable(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return state.isPathfindable(type);
    }

    /** The plant a 1.20.1 {@code IPlantable} stands for, as 1.21.1 passes it. */
    public static legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable(net.minecraft.world.level.block.state.BlockState plant) {
        return (level, pos) -> plant;
    }

    public static net.neoforged.neoforge.common.util.TriState triState(boolean value) {
        return value ? net.neoforged.neoforge.common.util.TriState.TRUE : net.neoforged.neoforge.common.util.TriState.FALSE;
    }

    /** {@code BlockState.canSustainPlant(level, pos, facing, plantable)}: asks the block; a block without an opinion lets the plant's own survival check decide. */
    public static boolean canSustainPlant(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.core.Direction facing, legacyconnector.v1_20_1.net.minecraftforge.common.IPlantable plantable) {
        net.minecraft.world.level.block.state.BlockState plant = plantable.getPlant(level, pos.relative(facing));
        net.neoforged.neoforge.common.util.TriState r = state.canSustainPlant(level, pos, facing, plant);
        if (r != net.neoforged.neoforge.common.util.TriState.DEFAULT) return r.isTrue();
        return level instanceof net.minecraft.world.level.LevelReader reader && plant.canSurvive(reader, pos.relative(facing));
    }

    // ------------------------------------------------------------------ mob effects

    /** {@code MobEffect.getAttributeModifiers()}: attribute to the modifier at amplifier 0 (1.21.1 keeps templates private and builds modifiers per level). */
    public static Map<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            net.minecraft.world.effect.MobEffect effect) {
        Map<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> out = new LinkedHashMap<>();
        effect.createModifiers(0, (attribute, modifier) -> out.put(attribute.value(), modifier));
        return out;
    }

    /** {@code MobEffect.getAttributeModifierValue(amplifier, modifier)}: the amount scaled by the level. */
    public static double getAttributeModifierValue(net.minecraft.world.effect.MobEffect effect, int amplifier,
            net.minecraft.world.entity.ai.attributes.AttributeModifier modifier) {
        return modifier.amount() * (amplifier + 1);
    }

    // ------------------------------------------------------------------ mod configs (NeoForge's ModConfig constructor is not public: the tracker makes and tracks it)

    /** {@code new ModConfig(type, spec, container, fileName)}. */
    public static net.neoforged.fml.config.ModConfig newModConfig(net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec spec,
            net.neoforged.fml.ModContainer container, String fileName) {
        return net.neoforged.fml.config.ConfigTracker.INSTANCE.registerConfig(type, spec, container, fileName);
    }

    public static net.neoforged.fml.config.ModConfig newModConfig(net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec spec,
            net.neoforged.fml.ModContainer container) {
        return net.neoforged.fml.config.ConfigTracker.INSTANCE.registerConfig(type, spec, container);
    }

    /** {@code ModContainer.addConfig(ModConfig)}: the config is tracked already when it is made. */
    public static void addConfig(net.neoforged.fml.ModContainer container, net.neoforged.fml.config.ModConfig config) {
    }

    public static com.electronwill.nightconfig.core.CommentedConfig getConfigData(net.neoforged.fml.config.ModConfig config) {
        var loaded = config.getLoadedConfig();
        return loaded == null ? null : loaded.config();
    }

    public static void saveConfig(net.neoforged.fml.config.ModConfig config) {
        var loaded = config.getLoadedConfig();
        if (loaded != null) loaded.save();
    }

    // ------------------------------------------------------------------ packs

    private static net.minecraft.server.packs.PackLocationInfo packInfo(String id, net.minecraft.network.chat.Component title,
            net.minecraft.server.packs.repository.PackSource source) {
        return new net.minecraft.server.packs.PackLocationInfo(id, title, source, java.util.Optional.empty());
    }

    /** Adapts the 1.20.1 one-method supplier to the two-method 1.21.1 interface. */
    public static net.minecraft.server.packs.repository.Pack.ResourcesSupplier packSupplier(LegacyResourcesSupplier legacy) {
        return new net.minecraft.server.packs.repository.Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info) {
                return legacy.open(info.id());
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,
                    net.minecraft.server.packs.repository.Pack.Metadata metadata) {
                return legacy.open(info.id());
            }
        };
    }

    /** {@code Pack.readMetaAndCreate(id, title, required, supplier, type, position, source)} of 1.20.1. */
    public static net.minecraft.server.packs.repository.Pack readMetaAndCreate(String id, net.minecraft.network.chat.Component title, boolean required,
            LegacyResourcesSupplier supplier, net.minecraft.server.packs.PackType type, net.minecraft.server.packs.repository.Pack.Position position,
            net.minecraft.server.packs.repository.PackSource source) {
        return net.minecraft.server.packs.repository.Pack.readMetaAndCreate(packInfo(id, title, source), packSupplier(supplier), type,
                new net.minecraft.server.packs.PackSelectionConfig(required, position, false));
    }

    /** {@code new PathPackResources(String name, Path root, boolean isBuiltin)}. */
    public static net.minecraft.server.packs.PathPackResources newPathPack(String name, java.nio.file.Path root, boolean builtin) {
        return new net.minecraft.server.packs.PathPackResources(packInfo(name, net.minecraft.network.chat.Component.literal(name),
                builtin ? net.minecraft.server.packs.repository.PackSource.BUILT_IN : net.minecraft.server.packs.repository.PackSource.DEFAULT), root);
    }

    private static Holder<Enchantment> ench(ResourceKey<Enchantment> key) {
        return MigrateContext.provider().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static boolean hasFrostWalker(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.FROST_WALKER), entity) > 0;
    }

    public static int getFireAspect(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.FIRE_ASPECT), entity);
    }

    public static int getKnockbackBonus(LivingEntity entity) {
        return EnchantmentHelper.getEnchantmentLevel(ench(Enchantments.KNOCKBACK), entity);
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ench(Enchantments.BINDING_CURSE), stack) > 0;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ench(Enchantments.VANISHING_CURSE), stack) > 0;
    }

    /** {@code EnchantmentHelper.getTagEnchantmentLevel(Enchantment, ItemStack)}: the level stored on the stack. */
    public static int getTagEnchantmentLevel(LegacyEnchantment e, ItemStack stack) {
        return HolderCompat.stackEnchantmentLevel(stack, e);
    }

    /** {@code EnchantmentInstance.enchantment} (a holder in 1.21.1). */
    public static LegacyEnchantment instanceEnchantment(EnchantmentInstance instance) {
        return LegacyEnchantments.legacy(instance.enchantment);
    }

    /** {@code EnchantmentHelper.deserializeEnchantments(ListTag)}: entries {@code {id:"ns:name", lvl:N}} as a map from enchantment to level. */
    public static Map<LegacyEnchantment, Integer> deserializeEnchantments(ListTag list) {
        Map<LegacyEnchantment, Integer> out = new LinkedHashMap<>();
        for (Tag t : list) {
            if (!(t instanceof CompoundTag c)) continue;
            ResourceLocation id = ResourceLocation.tryParse(c.getString("id"));
            if (id == null) continue;
            var holder = MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT).getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (holder.isPresent()) out.put(LegacyEnchantments.legacy(holder.get()), (int) c.getShort("lvl"));
        }
        return out;
    }
}
