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
        return (net.minecraft.core.DefaultedRegistry<?>) registry;
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

    private static Holder<Enchantment> ench(ResourceKey<Enchantment> key) {
        return MigrateContext.registries().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
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
