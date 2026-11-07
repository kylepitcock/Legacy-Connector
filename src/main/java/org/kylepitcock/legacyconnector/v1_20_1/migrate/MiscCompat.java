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

    public static String toJson(Component c) {
        return Component.Serializer.toJson(c, MigrateContext.provider());
    }

    public static MutableComponent fromJson(String json) {
        return Component.Serializer.fromJson(json, MigrateContext.provider());
    }

    public static MutableComponent fromJson(JsonElement json) {
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
}
