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
import legacyconnector.v1_12_2.baubles.api.BaubleType;
import legacyconnector.v1_12_2.baubles.api.IBauble;
import legacyconnector.v1_12_2.baubles.api.cap.IBaublesItemHandler;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Baubles 1.12 -> Curios bridge. Items implementing 1.12's {@link IBauble} become curios for the slots of their bauble type, players
 * get Baubles' seven slots (necklace, two rings, belt, head, body, charm) as Curios slots, and {@code BaublesApi.getBaublesHandler}
 * is a view over those slots in Baubles' order. Callers check {@code ModList.isLoaded("curios")} first: this class links against
 * the Curios API.
 */
public final class CuriosBaubles1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Curios");
    /** Baubles slot index (0-6) -> Curios slot id and index within it. */
    private static final String[] INV_ID = {"necklace", "ring", "ring", "belt", "head", "body", "charm"};
    private static final int[] INV_INDEX = {0, 0, 1, 0, 0, 0, 0};
    /** The Curios slots Baubles 1.12 had, each once. */
    private static final String[] SLOTS = {"necklace", "ring", "belt", "head", "body", "charm"};

    private CuriosBaubles1122() {}

    /** The Curios slot ids a bauble type fits (a trinket fits all). */
    static Set<String> slotsOf(BaubleType type) {
        Set<String> out = new LinkedHashSet<>();
        if (type == null) return out;
        for (int slot : type.getValidSlots()) out.add(INV_ID[slot]);
        return out;
    }

    /** After legacy postInit: every IBauble item (all subtypes) becomes a curio. */
    public static void registerItems() {
        int n = 0;
        for (Item legacy : ItemBridge1122.modItems()) {
            if (!(legacy instanceof IBauble bauble)) continue;
            for (net.minecraft.world.item.Item modern : ItemBridge1122.variants(legacy).values()) {
                CuriosApi.registerCurio(modern, new BaubleCurio(bauble));
                n++;
            }
        }
        if (n > 0) LOG.info("Registered {} 1.12.2 bauble item(s) with Curios", n);
    }

    /** Curios slot tags for the bauble items, the second ring slot and the player's slots, for the generated data pack. */
    public static void data(Map<String, Supplier<byte[]>> out) {
        Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (Item legacy : ItemBridge1122.modItems()) {
            if (!(legacy instanceof IBauble bauble)) continue;
            for (var e : ItemBridge1122.variants(legacy).entrySet()) {
                BaubleType type;
                try {
                    type = bauble.getBaubleType(new ItemStack(legacy, 1, e.getKey()));
                } catch (RuntimeException | LinkageError ex) {
                    continue;
                }
                for (String slot : slotsOf(type)) {
                    tags.computeIfAbsent(slot, k -> new LinkedHashSet<>()).add(BuiltInRegistries.ITEM.getKey(e.getValue()).toString());
                }
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
        for (String s : SLOTS) slots.add(s);
        entities.add("slots", slots);
        put(out, "data/legacyconnector/curios/entities/legacy_baubles_1122.json", entities);
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    public static IBaublesItemHandler handler(EntityPlayer player) {
        return new BaublesHandler(player);
    }

    public static IInventory inventory(EntityPlayer player) {
        return new BaublesInventory(new BaublesHandler(player));
    }

    private static EntityLivingBase legacy(LivingEntity entity) {
        return EntityBridge1122.wrap(entity) instanceof EntityLivingBase l ? l : null;
    }

    private record BaubleCurio(IBauble bauble) implements ICurioItem {
        @Override
        public void curioTick(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onWornTick(Stacks1122.fromModern(stack), e);
        }

        @Override
        public void onEquip(SlotContext ctx, net.minecraft.world.item.ItemStack prev, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onEquipped(Stacks1122.fromModern(stack), e);
        }

        @Override
        public void onUnequip(SlotContext ctx, net.minecraft.world.item.ItemStack next, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            if (e != null) bauble.onUnequipped(Stacks1122.fromModern(stack), e);
        }

        @Override
        public boolean canEquip(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            ItemStack legacyStack = Stacks1122.fromModern(stack);
            BaubleType type = bauble.getBaubleType(legacyStack);
            EntityLivingBase e = legacy(ctx.entity());
            return slotsOf(type).contains(ctx.identifier()) && (e == null || bauble.canEquip(legacyStack, e));
        }

        @Override
        public boolean canUnequip(SlotContext ctx, net.minecraft.world.item.ItemStack stack) {
            EntityLivingBase e = legacy(ctx.entity());
            return e == null || bauble.canUnequip(Stacks1122.fromModern(stack), e);
        }
    }

    /** Baubles' seven slots as a live view over the player's Curios slots. */
    private static final class BaublesHandler implements IBaublesItemHandler {
        private EntityPlayer player;
        private boolean blockEvents;
        private final boolean[] changed = new boolean[INV_ID.length];

        BaublesHandler(EntityPlayer player) {
            this.player = player;
        }

        private Optional<ICurioStacksHandler> curios(int slot) {
            if (slot < 0 || slot >= INV_ID.length || player == null || !(player.mcc$entity instanceof LivingEntity living)) return Optional.empty();
            return CuriosApi.getCuriosInventory(living).flatMap(h -> h.getStacksHandler(INV_ID[slot]))
                    .filter(h -> INV_INDEX[slot] < h.getSlots());
        }

        @Override
        public int getSlots() {
            return INV_ID.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return curios(slot).map(h -> Stacks1122.fromModern(h.getStacks().getStackInSlot(INV_INDEX[slot]))).orElse(ItemStack.EMPTY);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            curios(slot).ifPresent(h -> h.getStacks().setStackInSlot(INV_INDEX[slot], Stacks1122.toModern(stack)));
            if (slot >= 0 && slot < changed.length) changed[slot] = true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty() || !isItemValidForSlot(slot, stack, player)) return stack;
            var h = curios(slot);
            if (h.isEmpty()) return stack;
            var rest = h.get().getStacks().insertItem(INV_INDEX[slot], Stacks1122.toModern(stack), simulate);
            return Stacks1122.fromModern(rest);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            var h = curios(slot);
            if (h.isEmpty()) return ItemStack.EMPTY;
            return Stacks1122.fromModern(h.get().getStacks().extractItem(INV_INDEX[slot], amount, simulate));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemValidForSlot(slot, stack, player);
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack, EntityLivingBase entity) {
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IBauble bauble)) return false;
            BaubleType type = bauble.getBaubleType(stack);
            return type != null && type.hasSlot(slot) && (entity == null || bauble.canEquip(stack, entity));
        }

        @Override
        public boolean isEventBlocked() {
            return blockEvents;
        }

        @Override
        public void setEventBlock(boolean blockEvents) {
            this.blockEvents = blockEvents;
        }

        @Override
        public boolean isChanged(int slot) {
            return slot >= 0 && slot < changed.length && changed[slot];
        }

        @Override
        public void setChanged(int slot, boolean changed) {
            if (slot >= 0 && slot < this.changed.length) this.changed[slot] = changed;
        }

        @Override
        public void setPlayer(EntityLivingBase player) {
            if (player instanceof EntityPlayer p) this.player = p;
        }
    }

    /** The deprecated IInventory form of the same slots. */
    private record BaublesInventory(BaublesHandler handler) implements IInventory {
        @Override
        public int getSizeInventory() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return handler.getStackInSlot(index);
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            return handler.extractItem(index, count, false);
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            return handler.extractItem(index, 1, false);
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            handler.setStackInSlot(index, stack);
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return handler.isItemValidForSlot(index, stack, null);
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            for (int i = 0; i < handler.getSlots(); i++) handler.setStackInSlot(i, ItemStack.EMPTY);
        }

        @Override
        public String getName() {
            return "Baubles";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }
    }
}
