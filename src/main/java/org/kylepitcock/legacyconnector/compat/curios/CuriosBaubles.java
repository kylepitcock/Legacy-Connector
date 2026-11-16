package org.kylepitcock.legacyconnector.compat.curios;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.baubles.api.BaubleType;
import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.baubles.api.expanded.IBaubleExpanded;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Baubles -> Curios bridge. Legacy items implementing {@link IBauble} become curios for the slot matching their
 * bauble type, players get the slots Baubles had (amulet, two rings, belt), and {@code BaublesApi.getBaubles} is a
 * view over those Curios slots. Callers must check {@code ModList.isLoaded("curios")} before touching this class:
 * loading it links against the Curios API.
 */
public final class CuriosBaubles {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Curios");
    /** By BaubleType ordinal. */
    private static final String[] SLOT = {"ring", "necklace", "belt", "curio", "head", "body", "charm"};
    /** Legacy Baubles inventory slot -> Curios slot id and index (Baubles' four slots, then Baubles Expanded's head, body, charm, trinket). */
    private static final String[] INV_ID = {"necklace", "ring", "ring", "belt", "head", "body", "charm", "curio"};
    private static final int[] INV_INDEX = {0, 0, 1, 0, 0, 0, 0, 0};

    /** The Curios slot ids an item may be worn in: its Baubles Expanded type names when it has them, else its {@link BaubleType}; {@code UNIVERSAL} takes any. */
    static Set<String> slotsOf(IBauble bauble, ItemStack stack) {
        Set<String> out = new LinkedHashSet<>();
        if (bauble instanceof IBaubleExpanded expanded) {
            String[] names = expanded.getBaubleTypes(stack);
            if (names != null) for (String n : names) addName(out, n);
        }
        if (out.isEmpty()) {
            BaubleType type = bauble.getBaubleType(stack);
            if (type == BaubleType.UNIVERSAL) out.addAll(java.util.List.of(SLOT));
            else if (type != null && type.ordinal() < SLOT.length) out.add(SLOT[type.ordinal()]);
        }
        return out;
    }

    private static void addName(Set<String> out, String name) {
        if (name == null) return;
        switch (name) {
            case "amulet" -> out.add("necklace");
            case "universal" -> out.addAll(java.util.List.of(SLOT));
            case "ring", "belt", "head", "body", "charm" -> out.add(name);
            case "trinket" -> out.add("curio");
            default -> { }
        }
    }

    private CuriosBaubles() {}

    /** After legacy postInit: every IBauble item (all metadata variants) becomes a curio. */
    public static void registerItems() {
        int n = 0;
        for (Item legacy : ItemBridge.modItems()) {
            if (!(legacy instanceof IBauble bauble)) continue;
            for (net.minecraft.world.item.Item modern : legacy.mcc$variants.values()) {
                CuriosApi.registerCurio(modern, new BaubleCurio(bauble));
                n++;
            }
        }
        if (n > 0) LOG.info("Registered {} legacy bauble item(s) with Curios", n);
    }

    /** Slot definitions and slot tags for the generated data pack. */
    public static void data(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Item legacy : ItemBridge.modItems()) {
            if (!(legacy instanceof IBauble bauble)) continue;
            for (var e : legacy.mcc$variants.entrySet()) {
                Set<String> slots;
                try {
                    slots = slotsOf(bauble, new ItemStack(legacy, 1, e.getKey()));
                } catch (RuntimeException ex) {
                    continue;
                }
                for (String slot : slots) tags.computeIfAbsent(slot, k -> new LinkedHashSet<>()).add(BuiltInRegistries.ITEM.getKey(e.getValue()).toString());
            }
        }
        if (tags.isEmpty()) return;
        for (var e : tags.entrySet()) {
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            JsonArray values = new JsonArray();
            e.getValue().forEach(values::add);
            tag.add("values", values);
            put(out, "data/curios/tags/item/" + e.getKey() + ".json", tag);
        }
        JsonObject ring = new JsonObject();
        ring.addProperty("size", 2); // Baubles had two ring slots
        put(out, "data/legacyconnector/curios/slots/ring.json", ring);
        JsonObject entities = new JsonObject();
        JsonArray ents = new JsonArray();
        ents.add("player");
        entities.add("entities", ents);
        JsonArray slots = new JsonArray();
        for (String s : SLOT) slots.add(s);
        entities.add("slots", slots);
        put(out, "data/legacyconnector/curios/entities/legacy_baubles.json", entities);
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    public static IInventory inventory(EntityPlayer player) {
        return new BaublesInventory(player);
    }

    private static EntityLivingBase legacy(LivingEntity entity) {
        return EntityBridge.wrap(entity) instanceof EntityLivingBase l ? l : null;
    }

    private record BaubleCurio(IBauble bauble) implements ICurioItem {
        @Override
        public void curioTick(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onWornTick(ItemStack.mcc$wrap(stack), e);
        }

        @Override
        public void onEquip(SlotContext ctx, net.minecraft.world.item.ItemStack prev, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onEquipped(ItemStack.mcc$wrap(stack), e);
        }

        @Override
        public void onUnequip(SlotContext ctx, net.minecraft.world.item.ItemStack next, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onUnequipped(ItemStack.mcc$wrap(stack), e);
        }

        @Override
        public boolean canEquip(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            ItemStack legacyStack = ItemStack.mcc$wrap(stack);
            EntityLivingBase e = legacy(ctx.entity());
            return slotsOf(bauble, legacyStack).contains(ctx.identifier()) && (e == null || bauble.canEquip(legacyStack, e));
        }

        @Override
        public boolean canUnequip(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            return e == null || bauble.canUnequip(ItemStack.mcc$wrap(stack), e);
        }
    }

    /** Baubles' 4-slot player inventory as a view over the Curios slots. */
    private static final class BaublesInventory implements IInventory {
        private final EntityPlayer player;

        BaublesInventory(EntityPlayer player) {
            this.player = player;
        }

        private Optional<ICurioStacksHandler> handler(int slot) {
            if (slot < 0 || slot >= INV_ID.length || !(player.mcc$entity() instanceof LivingEntity living)) return Optional.empty();
            return CuriosApi.getCuriosInventory(living).flatMap(h -> h.getStacksHandler(INV_ID[slot]))
                    .filter(h -> INV_INDEX[slot] < h.getSlots());
        }

        @Override
        public int getSizeInventory() {
            return INV_ID.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler(slot).map(h -> ItemStack.mcc$wrap(h.getStacks().getStackInSlot(INV_INDEX[slot]))).orElse(null);
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            var h = handler(slot);
            if (h.isEmpty()) return null;
            net.minecraft.world.item.ItemStack taken = h.get().getStacks().extractItem(INV_INDEX[slot], amount, false);
            return ItemStack.mcc$wrap(taken);
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            handler(slot).ifPresent(h -> h.getStacks().setStackInSlot(INV_INDEX[slot], ItemStack.mcc$toModern(stack)));
        }

        @Override
        public String getInventoryName() {
            return "Baubles";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer p) {
            return true;
        }

        @Override
        public void openInventory() {
        }

        @Override
        public void closeInventory() {
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return slot >= 0 && slot < INV_ID.length && stack != null && stack.getItem() instanceof IBauble b && slotsOf(b, stack).contains(INV_ID[slot]);
        }
    }
}
