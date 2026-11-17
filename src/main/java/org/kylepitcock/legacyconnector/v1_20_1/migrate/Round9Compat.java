package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Common-side helpers of gap round 9 (1.20.1 mods on 1.21.1): overrides the bridges turn around ({@code dropCustomDeathLoot}, {@code onArmorTick}), the
 * banner pattern API, the cauldron interaction result and the effect blend data. Reached from {@code mc-calls.tsv} and {@code mc-bridges.tsv}.
 */
public final class Round9Compat {
    private Round9Compat() {}

    // ---- dropCustomDeathLoot(DamageSource, int looting, boolean recentlyHit) -------------------------------------------------------------

    /** The level the new {@code dropCustomDeathLoot(ServerLevel, ...)} needs for a super call of the old shape. */
    public static ServerLevel serverLevelOf(LivingEntity entity) {
        return (ServerLevel) entity.level();
    }

    /** The old {@code looting} argument: the looting level of what the damage source's attacker holds (the new method has no such argument). */
    public static int looting(DamageSource source) {
        if (source != null && source.getEntity() instanceof LivingEntity killer && MigrateContext.registries() != null) {
            Holder<Enchantment> looting = MigrateContext.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
            return killer.getMainHandItem().getEnchantmentLevel(looting);
        }
        return 0;
    }

    // ---- onArmorTick(ItemStack, Level, Player) ---------------------------------------------------------------------------------------------

    /**
     * The player wearing {@code stack} when {@code Item.inventoryTick} runs for one of the four armour slots, else null. 1.21.1's inventory ticks the armour
     * list with the index inside that list, so an armour slot is told from a hotbar slot by the identity of the stack there.
     */
    public static Player wornBy(Entity entity, int slot, ItemStack stack) {
        if (entity instanceof Player player && slot >= 0 && slot < 4 && player.getInventory().armor.get(slot) == stack) return player;
        return null;
    }

    /** {@code super.onArmorTick(...)} of Forge's {@code IForgeItem}: a no-op default. */
    public static void noArmorTick(Item item, ItemStack stack, Level level, Player player) {}

    /**
     * {@code stack.onArmorTick(level, player)}: Forge's inventory ran it for the worn armour. APPROXIMATION: nothing here, the item's own override already
     * runs every tick through the {@code inventoryTick} bridge, a second call from a mod's own hook would tick twice.
     */
    public static void stackArmorTick(ItemStack stack, Level level, Player player) {}

    /** {@code new Pack.Info(description, format, features)}: the pack is taken as compatible with this version (APPROXIMATION: the format is not compared). */
    public static net.minecraft.server.packs.repository.Pack.Metadata newPackInfo(net.minecraft.network.chat.Component description, int format,
            net.minecraft.world.flag.FeatureFlagSet features) {
        return new net.minecraft.server.packs.repository.Pack.Metadata(description, net.minecraft.server.packs.repository.PackCompatibility.COMPATIBLE, features, List.of());
    }

    // ---- banners -------------------------------------------------------------------------------------------------------------------------------

    private static final Map<String, String> PATTERN_CODES = Map.ofEntries(
            Map.entry("b", "base"), Map.entry("bl", "square_bottom_left"), Map.entry("br", "square_bottom_right"), Map.entry("tl", "square_top_left"),
            Map.entry("tr", "square_top_right"), Map.entry("bs", "stripe_bottom"), Map.entry("ts", "stripe_top"), Map.entry("ls", "stripe_left"),
            Map.entry("rs", "stripe_right"), Map.entry("cs", "stripe_center"), Map.entry("ms", "stripe_middle"), Map.entry("drs", "stripe_downright"),
            Map.entry("dls", "stripe_downleft"), Map.entry("ss", "small_stripes"), Map.entry("cr", "cross"), Map.entry("sc", "straight_cross"),
            Map.entry("bt", "triangle_bottom"), Map.entry("tt", "triangle_top"), Map.entry("bts", "triangles_bottom"), Map.entry("tts", "triangles_top"),
            Map.entry("ld", "diagonal_left"), Map.entry("rd", "diagonal_right"), Map.entry("lud", "diagonal_up_left"), Map.entry("rud", "diagonal_up_right"),
            Map.entry("mc", "circle"), Map.entry("mr", "rhombus"), Map.entry("vh", "half_vertical"), Map.entry("hh", "half_horizontal"),
            Map.entry("vhr", "half_vertical_right"), Map.entry("hhb", "half_horizontal_bottom"), Map.entry("bo", "border"), Map.entry("cbo", "curly_border"),
            Map.entry("gra", "gradient"), Map.entry("gru", "gradient_up"), Map.entry("bri", "bricks"), Map.entry("glb", "globe"), Map.entry("cre", "creeper"),
            Map.entry("sku", "skull"), Map.entry("flo", "flower"), Map.entry("moj", "mojang"), Map.entry("pig", "piglin"));

    private static Holder<BannerPattern> pattern(String code) {
        String path = PATTERN_CODES.getOrDefault(code, code);
        ResourceLocation id = path.contains(":") ? ResourceLocation.parse(path) : ResourceLocation.withDefaultNamespace(path);
        return MigrateContext.registries().lookupOrThrow(Registries.BANNER_PATTERN).get(ResourceKey.create(Registries.BANNER_PATTERN, id)).orElse(null);
    }

    private static String code(Holder<BannerPattern> pattern) {
        String path = pattern.unwrapKey().map(k -> k.location().getPath()).orElse("base");
        for (Map.Entry<String, String> e : PATTERN_CODES.entrySet()) if (e.getValue().equals(path)) return e.getKey();
        return path;
    }

    /** {@code BannerBlockEntity.createPatterns(base, listTag)}: the layers of the old {@code [{Pattern, Color}]} list, the base colour first. */
    public static List<Pair<Holder<BannerPattern>, DyeColor>> createPatterns(DyeColor base, ListTag tags) {
        List<Pair<Holder<BannerPattern>, DyeColor>> out = new ArrayList<>();
        Holder<BannerPattern> baseHolder = pattern("b");
        if (baseHolder != null) out.add(Pair.of(baseHolder, base == null ? DyeColor.WHITE : base));
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                CompoundTag layer = tags.getCompound(i);
                Holder<BannerPattern> holder = pattern(layer.getString("Pattern"));
                if (holder != null) out.add(Pair.of(holder, DyeColor.byId(layer.getInt("Color"))));
            }
        }
        return out;
    }

    /** {@code BannerBlockEntity.getItemPatterns(stack)}: the layers of the banner item as the old {@code [{Pattern, Color}]} list (or null without layers). */
    public static ListTag getItemPatterns(ItemStack stack) {
        BannerPatternLayers layers = stack.get(DataComponents.BANNER_PATTERNS);
        if (layers == null || layers.layers().isEmpty()) return null;
        ListTag out = new ListTag();
        for (BannerPatternLayers.Layer layer : layers.layers()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Pattern", code(layer.pattern()));
            tag.putInt("Color", layer.color().getId());
            out.add((Tag) tag);
        }
        return out;
    }

    /** {@code BannerBlockEntity.getPatternCount(stack)}. */
    public static int getPatternCount(ItemStack stack) {
        BannerPatternLayers layers = stack.get(DataComponents.BANNER_PATTERNS);
        return layers == null ? 0 : layers.layers().size();
    }

    /** {@code BannerBlockEntity.removeLastPattern(stack)}. */
    public static void removeLastPattern(ItemStack stack) {
        BannerPatternLayers layers = stack.get(DataComponents.BANNER_PATTERNS);
        if (layers == null || layers.layers().isEmpty()) return;
        BannerPatternLayers rest = layers.removeLast();
        if (rest.layers().isEmpty()) stack.remove(DataComponents.BANNER_PATTERNS);
        else stack.set(DataComponents.BANNER_PATTERNS, rest);
    }

    /** {@code bannerBlockEntity.getPatterns()}: the old list of (pattern, colour) pairs, the base colour as the first layer. */
    public static List<Pair<Holder<BannerPattern>, DyeColor>> getPatternsList(BannerBlockEntity banner) {
        List<Pair<Holder<BannerPattern>, DyeColor>> out = new ArrayList<>();
        Holder<BannerPattern> base = pattern("b");
        if (base != null) out.add(Pair.of(base, banner.getBaseColor()));
        for (BannerPatternLayers.Layer layer : banner.getPatterns().layers()) out.add(Pair.of(layer.pattern(), layer.color()));
        return out;
    }

    // ---- cauldron ---------------------------------------------------------------------------------------------------------------------------

    /**
     * {@code CauldronInteraction.interact(...)} answered an {@code InteractionResult}; 1.21.1 answers an {@code ItemInteractionResult}. APPROXIMATION: the
     * item-only results ({@code PASS_TO_DEFAULT_BLOCK_INTERACTION}, {@code SKIP_DEFAULT_BLOCK_INTERACTION}) become PASS and FAIL. An interaction a MOD implements
     * with the 1.20.1 return type is not bridged here (the interface method changed its return type).
     */
    public static InteractionResult cauldronInteract(net.minecraft.core.cauldron.CauldronInteraction interaction, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, ItemStack stack) {
        ItemInteractionResult result = interaction.interact(state, level, pos, player, hand, stack);
        return switch (result) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
            case FAIL -> InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    // ---- effect blend data ------------------------------------------------------------------------------------------------------------------

    /** {@code MobEffectInstance.getFactorData()}: 1.21.1 blends through {@code getBlendFactor}; the 1.20.1 data object is not kept (empty). */
    public static Optional<LegacyFactorData> getFactorData(MobEffectInstance instance) {
        return Optional.empty();
    }

    /** {@code MobEffect.createFactorData()}. */
    public static Optional<LegacyFactorData> createFactorData(MobEffect effect) {
        return Optional.empty();
    }
}
