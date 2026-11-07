package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import legacyconnector.v1_20_1.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_20_1.net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

/**
 * Curios 5.x (Minecraft 1.20.1) against the installed Curios 9.x (1.21.1), called from the rewritten code of a translated Forge 1.20.1 mod
 * (rows of {@code forward/1.20.1/lib-*.tsv}). The API kept its shape; what changed is {@code LazyOptional} to {@code Optional}, capabilities to
 * the NeoForge keys, attribute modifier UUIDs to {@code ResourceLocation} ids and attribute keys to {@code Holder}s.
 */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    private static Capability<ICuriosItemHandler> inventory;
    private static Capability<ICurio> item;

    /** {@code CuriosCapability.INVENTORY}: the facade key that carries the NeoForge entity capability. */
    public static synchronized Capability<ICuriosItemHandler> inventoryCapability() {
        if (inventory == null) inventory = new Capability<>("curios:inventory", null, CuriosCapability.INVENTORY, null);
        return inventory;
    }

    /** {@code CuriosCapability.ITEM}. */
    public static synchronized Capability<ICurio> itemCapability() {
        if (item == null) item = new Capability<>("curios:item", null, null, CuriosCapability.ITEM);
        return item;
    }

    public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity entity) {
        return LazyOptional.ofNullable(CuriosApi.getCuriosInventory(entity).orElse(null));
    }

    public static LazyOptional<ICurio> getCurio(ItemStack stack) {
        return LazyOptional.ofNullable(CuriosApi.getCurio(stack).orElse(null));
    }

    /** The 1.20.1 per-slot UUID: stable name based UUID of the slot id. */
    public static UUID slotUuid(SlotContext context) {
        return uuid(CuriosApi.getSlotId(context));
    }

    /** A modifier id as the UUID the legacy code expects (and gets back from {@link #id}). */
    public static UUID uuid(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** The modifier id for a legacy UUID. */
    public static ResourceLocation id(UUID uuid) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", uuid.toString());
    }

    /** Multimap keys to {@code Holder<Attribute>} (the legacy code put raw attributes in; Curios' own entries already are holders). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Multimap holderKeys(Multimap legacy) {
        if (legacy == null) return null;
        Multimap out = LinkedHashMultimap.create();
        for (Object o : legacy.entries()) {
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey();
            out.put(k instanceof Attribute a ? BuiltInRegistries.ATTRIBUTE.wrapAsHolder(a) : k, e.getValue());
        }
        return out;
    }

    /** The reverse for what Curios hands to legacy code. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Multimap rawKeys(Multimap modern) {
        if (modern == null) return null;
        Multimap out = LinkedHashMultimap.create();
        for (Object o : modern.entries()) {
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey();
            out.put(k instanceof Holder<?> h ? h.value() : k, e.getValue());
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Multimap getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        return rawKeys(CuriosApi.getAttributeModifiers(context, id(uuid), stack));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addSlotModifier(Multimap legacy, String identifier, UUID uuid, double amount, AttributeModifier.Operation operation) {
        Multimap tmp = LinkedHashMultimap.create();
        CuriosApi.addSlotModifier(tmp, identifier, id(uuid), amount, operation);
        legacy.putAll(rawKeys(tmp));
    }

    // ---- the helper object (CuriosApi.getCuriosHelper()) of 5.x: its methods became static CuriosApi ones

    public static LazyOptional<ICuriosItemHandler> getCuriosHandler(ICuriosHelper helper, LivingEntity entity) {
        return getCuriosInventory(entity);
    }

    public static LazyOptional<net.neoforged.neoforge.items.IItemHandlerModifiable> getEquippedCurios(ICuriosHelper helper, LivingEntity entity) {
        return LazyOptional.ofNullable(CuriosApi.getCuriosInventory(entity).map(ICuriosItemHandler::getEquippedCurios).orElse(null));
    }

    public static void addSlotModifier(ICuriosHelper helper, Multimap legacy, String identifier, UUID uuid, double amount, AttributeModifier.Operation operation) {
        addSlotModifier(legacy, identifier, uuid, amount, operation);
    }

    /** {@code ICuriosItemHandler.getLootingLevel(source, entity, base)}: 9 asks for the loot context and applies the bonus itself. */
    public static int lootingLevel(ICuriosItemHandler handler, DamageSource source, LivingEntity entity, int base) {
        return base;
    }

    /** {@code ICurioItem.playRightClickEquipSound(entity, stack)} no longer exists (the item names its equip sound): the generic one. */
    public static void playRightClickEquipSound(ICurioItem item, LivingEntity entity, ItemStack stack) {
        entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    // ---- arguments of the 1.21.1 hooks the bridge methods hand to the mod's 1.20.1 ones

    public static DamageSource damageSource(LootContext context) {
        return context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
    }

    public static LivingEntity looter(LootContext context) {
        return context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity l ? l : null;
    }

    public static int zero(LootContext context) {
        return 0;
    }
}
