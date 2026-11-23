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
        if (stack == null || stack.isEmpty() || provider == null) return false;
        boolean changed = repair(stack, DataComponents.ENCHANTMENTS, provider);
        changed |= repair(stack, DataComponents.STORED_ENCHANTMENTS, provider);
        if (changed && !logged) {
            logged = true;
            LOG.warn("A stack ({}) carried enchantments of another registry set; they were looked up again by key (logged once)", stack.getItem());
        }
        return changed;
    }

    private static boolean repair(ItemStack stack, DataComponentType<ItemEnchantments> type, HolderLookup.Provider provider) {
        ItemEnchantments enchants = stack.get(type);
        if (enchants == null || enchants.isEmpty()) return false;
        // the owner the save's own codec checks holders against, and the lookup that resolves them again
        var owner = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE).owner(Registries.ENCHANTMENT).orElse(null);
        var lookup = provider.lookup(Registries.ENCHANTMENT).orElse(null);
        if (owner == null || lookup == null) return false;
        boolean foreign = false;
        for (var e : enchants.entrySet()) {
            if (e.getKey() instanceof Holder.Reference<Enchantment> r && !r.canSerializeIn(owner)) {
                foreign = true;
                break;
            }
        }
        if (!foreign) return false;
        var fixed = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var e : enchants.entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            if (holder instanceof Holder.Reference<Enchantment> r && !r.canSerializeIn(owner)) {
                holder = lookup.get(r.key()).orElse(null);
                if (holder == null) continue; // an enchantment this world does not have
            }
            fixed.set(holder, e.getIntValue());
        }
        stack.set(type, fixed.toImmutable());
        return true;
    }
}
