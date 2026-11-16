package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.lang.reflect.Method;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * BuildCraft 3.1's {@code buildcraft.api.ISpecialInventory} (Forestry, Railcraft, Thermal Expansion, EE2 machines; the interface ships inside the mods)
 * as a NeoForge item handler. A pipe did not use slots on these: {@code addItem(stack, doAdd, from)} offered a stack to the machine's own rules (a
 * sorting apiary, a hopper-like tank) and {@code extractItem(doRemove, from)} took the next product. The handler has one virtual slot that shows
 * the next product, accepts whatever {@code addItem} accepts, and extracts what {@code extractItem} yields. The interface is found by name on the
 * tile's class and called by reflection, like {@link Liquids125}.
 */
public final class SpecialInventory125 {
    private SpecialInventory125() {}

    private static final ClassValue<Api> API = new ClassValue<>() {
        @Override
        protected Api computeValue(Class<?> type) {
            return Api.of(type);
        }
    };

    private static final class Api {
        Class<?> special;
        Class<?> orientation;
        Method add;
        Method extract;

        static Api of(Class<?> type) {
            Api a = new Api();
            for (Class<?> c = type; c != null && a.special == null; c = c.getSuperclass()) a.special = find(c);
            if (a.special == null) return a;
            try {
                a.orientation = Class.forName("buildcraft.api.Orientations", false, type.getClassLoader());
                for (Method m : a.special.getMethods()) {
                    if (m.getName().equals("addItem") && m.getParameterCount() == 3) a.add = m;
                    else if (m.getName().equals("extractItem") && m.getParameterCount() == 2) a.extract = m;
                }
                if (a.add == null || a.extract == null) a.special = null;
            } catch (ReflectiveOperationException | LinkageError e) {
                a.special = null;
            }
            return a;
        }

        private static Class<?> find(Class<?> c) {
            if (c.getName().equals("buildcraft.api.ISpecialInventory")) return c;
            for (Class<?> i : c.getInterfaces()) {
                Class<?> f = find(i);
                if (f != null) return f;
            }
            return null;
        }
    }

    /** The item handler of a tile implementing {@code buildcraft.api.ISpecialInventory}, or null for other tiles. */
    public static IItemHandler forTile(Object legacy, Direction side) {
        if (legacy == null) return null;
        Api api = API.get(legacy.getClass());
        return api.special == null || !api.special.isInstance(legacy) ? null : new Handler(legacy, side, api);
    }

    private record Handler(Object tile, Direction side, Api api) implements IItemHandler {
        private Object from() {
            return Orientations125.of(api.orientation, side);
        }

        /** The next product, without taking it. */
        private net.minecraft.world.item.ItemStack peek() {
            try {
                Object r = api.extract.invoke(tile, false, from());
                return r instanceof ItemStack s ? ItemStack.mcc$toModern(s).copy() : net.minecraft.world.item.ItemStack.EMPTY;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            return peek();
        }

        @Override
        public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            ItemStack offered = ItemStack.mcc$wrap(stack.copy());
            int before = stack.getCount();
            try {
                boolean accepted = (Boolean) api.add.invoke(tile, offered, !simulate, from());
                if (!accepted) return stack;
                // an implementation that consumed part of the stack leaves the rest in the offered copy
                int left = simulate ? 0 : Math.max(0, offered.mcc$count());
                return left > 0 && left < before ? stack.copyWithCount(left) : net.minecraft.world.item.ItemStack.EMPTY;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return stack;
            }
        }

        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            net.minecraft.world.item.ItemStack next = peek();
            if (next.isEmpty() || next.getCount() > amount) return net.minecraft.world.item.ItemStack.EMPTY;
            if (simulate) return next;
            try {
                Object r = api.extract.invoke(tile, true, from());
                return r instanceof ItemStack s ? ItemStack.mcc$toModern(s) : net.minecraft.world.item.ItemStack.EMPTY;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            try {
                return !stack.isEmpty() && (Boolean) api.add.invoke(tile, ItemStack.mcc$wrap(stack.copy()), false, from());
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
    }
}
