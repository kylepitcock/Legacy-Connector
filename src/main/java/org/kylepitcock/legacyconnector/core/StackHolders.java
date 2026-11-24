package org.kylepitcock.legacyconnector.core;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps a stack's enchantment holders in the registry set it is saved with. A holder belongs to the registry instance that made it (the
 * client's synced registries, a previous world's, a lookup built for a command or a data generator are different instances with the same
 * keys); encoding a stack that carries such a holder throws "Element ... is not valid in current registry set" and fails the whole player
 * or chunk save. {@link #repair} looks every enchantment up again by its key in the registry set of the save, which is what vanilla would
 * have stored; shared by every hosted legacy version (called from the ItemStack save mixin).
 */
public final class StackHolders {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/StackHolders");
    private static volatile boolean logged;

    private StackHolders() {}

    /** Re-resolves the enchantment holders of the stack against the provider when one is foreign to it. @return true when something was replaced. */
    public static boolean repair(ItemStack stack, HolderLookup.Provider provider) {
        return repair(stack, provider, false);
    }

    /**
     * @param force also replaces holders the owner check accepts when they are not the very holder the provider's registry has for the key (used
     *              after a save failed all the same)
     */
    public static boolean repair(ItemStack stack, HolderLookup.Provider provider, boolean force) {
        if (stack == null || stack.isEmpty() || provider == null) return false;
        boolean changed = repair(stack, DataComponents.ENCHANTMENTS, provider, force);
        changed |= repair(stack, DataComponents.STORED_ENCHANTMENTS, provider, force);
        if (changed && !logged) {
            logged = true;
            LOG.warn("A stack ({}) carried enchantments of another registry set; they were looked up again by key (logged once)", stack.getItem());
        }
        return changed;
    }

    private static boolean repair(ItemStack stack, DataComponentType<ItemEnchantments> type, HolderLookup.Provider provider, boolean force) {
        ItemEnchantments enchants = stack.get(type);
        if (enchants == null || enchants.isEmpty()) return false;
        // the owner the save's own codec checks holders against, and the lookup that resolves them again
        var owner = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE).owner(Registries.ENCHANTMENT).orElse(null);
        var lookup = provider.lookup(Registries.ENCHANTMENT).orElse(null);
        if (owner == null || lookup == null) return false;
        boolean foreign = false;
        for (var e : enchants.entrySet()) {
            if (e.getKey() instanceof Holder.Reference<Enchantment> r && (!r.canSerializeIn(owner) || force && lookup.get(r.key()).orElse(null) != r)) {
                foreign = true;
                break;
            }
        }
        if (!foreign) return false;
        var fixed = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var e : enchants.entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            if (holder instanceof Holder.Reference<Enchantment> r && (!r.canSerializeIn(owner) || force && lookup.get(r.key()).orElse(null) != r)) {
                holder = lookup.get(r.key()).orElse(null);
                if (holder == null) continue; // an enchantment this world does not have
            }
            fixed.set(holder, e.getIntValue());
        }
        stack.set(type, fixed.toImmutable());
        return true;
    }

    private static volatile boolean diagnosed;

    /**
     * A save of the stack failed with "not valid in current registry set" although {@link #repair} found nothing foreign: says what it can
     * (once), looks the enchantments up again unconditionally and, when the save still fails, leaves the enchantments out of the saved copy
     * instead of failing the whole player or chunk save. @return the saved tag
     */
    public static net.minecraft.nbt.Tag recover(ItemStack stack, HolderLookup.Provider provider, RuntimeException failure,
            java.util.function.Function<ItemStack, net.minecraft.nbt.Tag> save) {
        if (!diagnosed) {
            diagnosed = true;
            try {
                var owner = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE).owner(Registries.ENCHANTMENT).orElse(null);
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                StringBuilder sb = new StringBuilder("Saving a stack failed (" + failure.getMessage() + "); thread " + Thread.currentThread().getName()
                        + ", provider " + System.identityHashCode(provider) + " " + provider.getClass().getName()
                        + ", server access " + (server == null ? "none" : System.identityHashCode(server.registryAccess()))
                        + ", ops owner " + (owner == null ? "none" : System.identityHashCode(owner) + " " + owner.getClass().getName()));
                for (var t : new DataComponentType[] {DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS}) {
                    var en = (ItemEnchantments) stack.get(t);
                    if (en == null) continue;
                    for (var e : en.entrySet()) {
                        sb.append("; ").append(t).append(' ').append(e.getKey()).append(" holder ").append(System.identityHashCode(e.getKey())).append(' ')
                                .append(e.getKey().getClass().getName());
                        if (e.getKey() instanceof Holder.Reference<Enchantment> r && owner != null) sb.append(" canSerializeIn ").append(r.canSerializeIn(owner));
                    }
                }
                LOG.error(sb.toString());
            } catch (RuntimeException | LinkageError e) {
                LOG.error("Saving a stack failed ({}); diagnostics failed: {}", failure.getMessage(), e.toString());
            }
        }
        repair(stack, provider, true);
        try {
            return save.apply(stack);
        } catch (RuntimeException again) {
            ItemStack copy = stack.copy();
            copy.remove(DataComponents.ENCHANTMENTS);
            copy.remove(DataComponents.STORED_ENCHANTMENTS);
            LOG.error("The enchantments of a {} could not be saved and were left out of the save", stack.getItem());
            return save.apply(copy);
        }
    }
}
