package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server snapshots of inventories owned by registered PlayerAPI bases; application only updates a client's existing mirrors. */
public final class PlayerInventoryStateB173 {
    static final int MAX_INVENTORIES = 32, MAX_SLOTS = 512, MAX_BYTES = 65_536;
    record Binding(String key, Inventory inventory) {}
    private PlayerInventoryStateB173() {}

    /** Names from packets only look up this locally derived map; they never select classes to load or fields to reflect. */
    static List<Binding> bindings(List<PlayerBase> bases, Inventory playerInventory) {
        var result = new ArrayList<Binding>();
        Set<Inventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int slots = 0;
        for (PlayerBase base : bases) {
            for (Class<?> type = base.getClass(); type != null && PlayerBase.class.isAssignableFrom(type); type = type.getSuperclass()) {
                Field[] fields = type.getDeclaredFields();
                Arrays.sort(fields, Comparator.comparing(Field::getName));
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) || !Inventory.class.isAssignableFrom(field.getType())) continue;
                    if (!field.trySetAccessible()) throw new IllegalStateException("Inaccessible PlayerAPI inventory field " + field);
                    Inventory inventory;
                    try { inventory = (Inventory) field.get(base); }
                    catch (IllegalAccessException error) { throw new IllegalStateException("Cannot read PlayerAPI inventory field " + field, error); }
                    if (inventory == null || inventory == playerInventory || inventory instanceof CraftingInventory
                            || inventory instanceof CraftingResultInventory || !seen.add(inventory)) continue;
                    int size = inventory.size();
                    if (size < 0 || size > MAX_SLOTS || result.size() >= MAX_INVENTORIES || slots + size > MAX_SLOTS)
                        throw new IllegalStateException("PlayerAPI inventory snapshot exceeds inventory/slot limits");
                    slots += size;
                    result.add(new Binding(base.getClass().getName() + "#" + type.getName() + "#" + field.getName(), inventory));
                }
            }
        }
        return result;
    }

    public static CompoundTag capture(ClientPlayerEntity actor) {
        if (!(actor.mcc$modern instanceof net.minecraft.server.level.ServerPlayer player)) return new CompoundTag();
        var snapshot = new CompoundTag();
        var entries = new ListTag();
        for (Binding binding : bindings(PlayerApiB173.bases(actor), actor.inventory)) {
            var entry = new CompoundTag();
            var stacks = new ListTag();
            var inventory = LegacyInventoryB173.of(binding.inventory());
            entry.putString("binding", binding.key());
            entry.putInt("size", inventory.getContainerSize());
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                stacks.add(inventory.getItem(slot).saveOptional(player.registryAccess()));
            entry.put("stacks", stacks);
            entries.add(entry);
        }
        snapshot.putInt("version", 1);
        snapshot.put("inventories", entries);
        if (!bounded(snapshot)) throw new IllegalStateException("PlayerAPI inventory snapshot exceeds byte limit");
        return snapshot;
    }

    /** Owner inventory protocol retains its existing void application entry point. */
    public static void apply(ClientPlayerEntity actor, CompoundTag snapshot) {
        Runnable prepared=prepare(actor,snapshot);if(prepared!=null)prepared.run();
    }

    static Runnable prepare(ClientPlayerEntity actor,CompoundTag snapshot){
        if(!(actor.mcc$modern instanceof Player player)||!player.level().isClientSide
                ||snapshot==null||snapshot.getInt("version")!=1||!bounded(snapshot))return null;
        return prepare(bindings(PlayerApiB173.bases(actor),actor.inventory),snapshot,
                tag->ItemStack.parseOptional(player.registryAccess(),tag));
    }

    /** Parse every stack and binding before returning a commit that changes any original inventory. */
    static Runnable prepare(List<Binding> bindings,CompoundTag snapshot,
            java.util.function.Function<CompoundTag,ItemStack> decode){
        if(snapshot==null||snapshot.getInt("version")!=1||!bounded(snapshot))return null;
        if (!snapshot.contains("inventories", Tag.TAG_LIST)) return null;
        if(!((ListTag)snapshot.get("inventories")).isEmpty()&&((ListTag)snapshot.get("inventories")).getElementType()!=Tag.TAG_COMPOUND)return null;
        var entries = snapshot.getList("inventories", Tag.TAG_COMPOUND);
        if (entries.size() > MAX_INVENTORIES) return null;
        var local = new HashMap<String, Binding>();
        for (Binding binding : bindings) local.put(binding.key(), binding);
        record Update(Inventory inventory, List<ItemStack> stacks) {}
        var updates = new ArrayList<Update>();
        var seen = new HashSet<String>();
        int total = 0;
        try {
            for (Tag raw : entries) {
                var entry = (CompoundTag) raw;
                String key = entry.getString("binding");
                Binding binding = local.get(key);
                int size = entry.getInt("size");
                if (binding == null || !seen.add(key) || size < 0 || size > MAX_SLOTS
                        || binding.inventory().size() != size || !entry.contains("stacks", Tag.TAG_LIST)) return null;
                total += size;
                if (total > MAX_SLOTS) return null;
                var encoded = entry.getList("stacks", Tag.TAG_COMPOUND);
                if (encoded.size() != size) return null;
                var stacks = new ArrayList<ItemStack>(size);
                for (Tag stack : encoded) {
                    ItemStack parsed = decode.apply((CompoundTag) stack);
                    if (!((CompoundTag) stack).isEmpty() && (parsed.isEmpty() || !StacksB173.canRepresent(parsed))) return null;
                    stacks.add(parsed);
                }
                updates.add(new Update(binding.inventory(), stacks));
            }
        } catch (RuntimeException | LinkageError malformed) { return null; }
        return ()->{
        for (Update update : updates) {
            var inventory = LegacyInventoryB173.of(update.inventory());
            for (int slot = 0; slot < update.stacks().size(); slot++) inventory.setItem(slot, update.stacks().get(slot));
            inventory.setChanged();
        }
        };
    }

    private static boolean bounded(CompoundTag snapshot) {
        try {
            var counter = new java.io.OutputStream() {
                int size;
                @Override public void write(int value) throws IOException { count(1); }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException { count(length); }
                private void count(int amount) throws IOException {
                    if (amount > MAX_BYTES - size) throw new IOException("Snapshot exceeds byte limit");
                    size += amount;
                }
            };
            try (var output = new DataOutputStream(counter)) { NbtIo.write(snapshot, output); }
            return true;
        } catch (IOException | RuntimeException error) { return false; }
    }
}
