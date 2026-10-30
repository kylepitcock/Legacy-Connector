package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBehaviorDispenseItem;
import legacyconnector.v1_12_2.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryDefaulted;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

/**
 * 1.12.2's {@code BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY} over the modern dispenser: a behaviour a mod puts for its item is
 * registered on {@link DispenserBlock} for the item's modern items (now, or when the item becomes modern), and a vanilla item without
 * a legacy entry answers the modern behaviour as a legacy one (mods wrap it: FutureMC's glass bottles). Never answers null.
 */
public final class Dispensers1122 {
    private Dispensers1122() {}

    /** Legacy items whose behaviour waits for their modern items. */
    private static final Map<Item, IBehaviorDispenseItem> PENDING = new IdentityHashMap<>();

    /** The registry the shim's BlockDispenser holds. */
    public static final class Registry extends RegistryDefaulted<Item, IBehaviorDispenseItem> {
        private final Map<net.minecraft.world.item.Item, IBehaviorDispenseItem> wrappers = new IdentityHashMap<>();

        public Registry() {
            super(new BehaviorDefaultDispenseItem());
        }

        @Override
        public IBehaviorDispenseItem getObject(Item item) {
            IBehaviorDispenseItem own = registryObjects.get(item);
            if (own != null) return own;
            if (item != null && item.mcc$vanillaModern != null) {
                DispenseItemBehavior modern = DispenserBlock.DISPENSER_REGISTRY.get(item.mcc$vanillaModern);
                if (modern != null && !(modern instanceof net.minecraft.core.dispenser.DefaultDispenseItemBehavior
                        && modern.getClass() == net.minecraft.core.dispenser.DefaultDispenseItemBehavior.class)) {
                    synchronized (wrappers) {
                        return wrappers.computeIfAbsent(item.mcc$vanillaModern, k -> new ModernBehavior(modern));
                    }
                }
            }
            return defaultObject;
        }

        @Override
        public void putObject(Item item, IBehaviorDispenseItem behavior) {
            super.putObject(item, behavior);
            if (item == null || behavior == null) return;
            if (!install(item, behavior)) {
                synchronized (PENDING) {
                    PENDING.put(item, behavior);
                }
            }
        }
    }

    /** Called when a legacy item gets its modern items: installs a behaviour registered before that. */
    public static void onItemRegistered(Item item) {
        IBehaviorDispenseItem behavior;
        synchronized (PENDING) {
            behavior = PENDING.remove(item);
        }
        if (behavior != null) install(item, behavior);
    }

    /** Registers the behaviour on the modern dispenser for every modern item of the legacy one; false when it has none yet. */
    private static boolean install(Item item, IBehaviorDispenseItem behavior) {
        Set<net.minecraft.world.item.Item> targets = new LinkedHashSet<>(ItemBridge1122.variants(item).values());
        if (item.mcc$vanillaModern != null) {
            targets.add(item.mcc$vanillaModern);
            if (item.getHasSubtypes()) {
                for (int meta = 1; meta < 16; meta++) {
                    try {
                        var v = Vanilla1122.modernVariant(item, meta);
                        if (v != null) targets.add(v);
                    } catch (RuntimeException e) {
                        // no variant for this metadata
                    }
                }
            }
        }
        targets.remove(net.minecraft.world.item.Items.AIR);
        if (targets.isEmpty()) return false;
        DispenseItemBehavior modern = behavior instanceof ModernBehavior m ? m.modern : IBehaviorDispenseItem.mcc$modern(behavior);
        for (var t : targets) DispenserBlock.registerBehavior(t, modern);
        return true;
    }

    /** A modern dispenser behaviour as a legacy one: runs on the real dispenser at the source's position. */
    public static final class ModernBehavior implements IBehaviorDispenseItem {
        final DispenseItemBehavior modern;

        ModernBehavior(DispenseItemBehavior modern) {
            this.modern = modern;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            BlockSource modernSource = modernSource(source);
            if (modernSource == null || stack == null || stack.isEmpty()) return stack;
            try {
                return Stacks1122.fromModern(modern.dispense(modernSource, Stacks1122.toModern(stack)));
            } catch (RuntimeException e) {
                // a behaviour that needs a real dispenser block entity (buckets fill into it): drop the item as the default does
                return new BehaviorDefaultDispenseItem().dispense(source, stack);
            }
        }
    }

    /** The modern dispenser source at a legacy source's position (its block entity null unless it is a real dispenser), or null. */
    public static BlockSource modernSource(IBlockSource source) {
        var level = source == null || source.getWorld() == null ? null : source.getWorld().mcc$level();
        if (!(level instanceof ServerLevel server)) return null;
        var p = source.getBlockPos();
        var pos = new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
        DispenserBlockEntity be = server.getBlockEntity(pos) instanceof DispenserBlockEntity d ? d : null;
        return new BlockSource(server, pos, server.getBlockState(pos), be);
    }
}
