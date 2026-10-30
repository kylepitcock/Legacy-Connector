package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.ICriterionTrigger;
import legacyconnector.v1_12_2.net.minecraft.advancements.PlayerAdvancements;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Vanilla's inventory_changed trigger. Firing it fires the game's trigger (whose listeners are the game's advancements); its instances test
 * a player inventory (occupied, full and empty slot counts and items that must be in it) as 1.12 did.
 */
public class InventoryChangeTrigger implements ICriterionTrigger<InventoryChangeTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("inventory_changed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void addListener(PlayerAdvancements playerAdvancementsIn, ICriterionTrigger.Listener<Instance> listener) {
    }

    @Override
    public void removeListener(PlayerAdvancements playerAdvancementsIn, ICriterionTrigger.Listener<Instance> listener) {
    }

    @Override
    public void removeAllListeners(PlayerAdvancements playerAdvancementsIn) {
    }

    @Override
    public Instance deserializeInstance(com.google.gson.JsonObject json, com.google.gson.JsonDeserializationContext context) {
        com.google.gson.JsonObject slots = json.has("slots") ? json.getAsJsonObject("slots") : new com.google.gson.JsonObject();
        return new Instance(MinMaxBounds.deserialize(slots.get("occupied")), MinMaxBounds.deserialize(slots.get("full")),
                MinMaxBounds.deserialize(slots.get("empty")), ItemPredicate.deserializeArray(json.get("items")));
    }

    public void trigger(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player,
            legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer inventory) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        if (sp == null) return;
        net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(sp, sp.getInventory(), net.minecraft.world.item.ItemStack.EMPTY);
    }

    public static class Instance extends AbstractCriterionInstance {
        private final MinMaxBounds occupied;
        private final MinMaxBounds full;
        private final MinMaxBounds empty;
        private final ItemPredicate[] items;

        public Instance(MinMaxBounds occupied, MinMaxBounds full, MinMaxBounds empty, ItemPredicate[] items) {
            super(ID);
            this.occupied = occupied;
            this.full = full;
            this.empty = empty;
            this.items = items;
        }

        /** Whether the inventory meets the slot counts and holds every listed item. */
        public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer inventory) {
            int fullSlots = 0, emptySlots = 0, occupiedSlots = 0;
            java.util.List<ItemPredicate> missing = new java.util.ArrayList<>(java.util.Arrays.asList(items));
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) {
                    emptySlots++;
                } else {
                    occupiedSlots++;
                    if (stack.getCount() >= stack.getMaxStackSize()) fullSlots++;
                    missing.removeIf(p -> p.test(stack));
                }
            }
            return full.test(fullSlots) && empty.test(emptySlots) && occupied.test(occupiedSlots) && missing.isEmpty();
        }
    }
}
