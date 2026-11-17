package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * BuildCraft 3.1's {@code buildcraft.api.IPipeEntry} (a pipe or conveyor that takes items from outside): items a hopper or modern pipe pushes into
 * the tile enter as an {@code EntityPassiveItem} at the tile's position via {@code entityEntering(item, from)}, as BuildCraft's own pipes handed them
 * on. The interface and the passive item class ship inside the mods, so they are found by name and called by reflection like {@link Liquids125}.
 */
public final class PipeEntry125 {
    private PipeEntry125() {}

    private static final ClassValue<Api> API = new ClassValue<>() {
        @Override
        protected Api computeValue(Class<?> type) {
            return Api.of(type);
        }
    };

    private static final class Api {
        Class<?> entry;
        Class<?> orientation;
        Method entering;
        Method accepts;
        Constructor<?> passive;

        static Api of(Class<?> type) {
            Api a = new Api();
            for (Class<?> c = type; c != null && a.entry == null; c = c.getSuperclass()) a.entry = find(c);
            if (a.entry == null) return a;
            try {
                ClassLoader loader = type.getClassLoader();
                a.orientation = Class.forName("buildcraft.api.Orientations", false, loader);
                Class<?> item = Class.forName("buildcraft.api.EntityPassiveItem", false, loader);
                a.passive = item.getConstructor(legacyconnector.v1_7_10.net.minecraft.world.World.class, double.class, double.class, double.class, ItemStack.class);
                a.entering = a.entry.getMethod("entityEntering", item, a.orientation);
                a.accepts = a.entry.getMethod("acceptItems");
            } catch (ReflectiveOperationException | LinkageError e) {
                a.entry = null;
            }
            return a;
        }

        private static Class<?> find(Class<?> c) {
            if (c.getName().equals("buildcraft.api.IPipeEntry")) return c;
            for (Class<?> i : c.getInterfaces()) {
                Class<?> f = find(i);
                if (f != null) return f;
            }
            return null;
        }
    }

    /** The item handler of a tile implementing {@code buildcraft.api.IPipeEntry}, or null for other tiles. */
    public static IItemHandler forTile(Object legacy, Direction side) {
        if (!(legacy instanceof TileEntity tile)) return null;
        Api api = API.get(legacy.getClass());
        return api.entry == null || !api.entry.isInstance(legacy) ? null : new Handler(tile, side, api);
    }

    private record Handler(TileEntity tile, Direction side, Api api) implements IItemHandler {
        private boolean open() {
            try {
                return (Boolean) api.accepts.invoke(tile);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override
        public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || tile.worldObj == null || !open()) return stack;
            if (simulate) return net.minecraft.world.item.ItemStack.EMPTY;
            try {
                Object from = Orientations125.of(api.orientation, side);
                Object passive = api.passive.newInstance(tile.worldObj, tile.xCoord + 0.5, tile.yCoord + 0.25, tile.zCoord + 0.5, ItemStack.mcc$wrap(stack.copy()));
                api.entering.invoke(tile, passive, from);
                return net.minecraft.world.item.ItemStack.EMPTY;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return stack;
            }
        }

        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return !stack.isEmpty() && open();
        }
    }
}
