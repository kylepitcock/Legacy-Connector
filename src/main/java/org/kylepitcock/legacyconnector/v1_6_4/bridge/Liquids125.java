package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.lang.reflect.Method;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.LiquidStack;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Minecraft 1.2.x's liquid API (BuildCraft 3.1's flat {@code buildcraft.api.ILiquidContainer}: {@code fill(Orientations, quantity, liquidId, doFill)},
 * {@code empty(quantity, doEmpty)}, {@code getLiquidQuantity}, {@code getLiquidId}) as a NeoForge fluid handler. A liquid is a block id (8/9 water,
 * 10/11 lava, a mod's own block), mapped to a fluid the way {@link Liquids164} maps the 1.5.2 liquid stacks. The interface ships inside the
 * mods, so it is found by name on the tile's class and called by reflection.
 */
public final class Liquids125 {
    private Liquids125() {}

    private static final ClassValue<Api> API = new ClassValue<>() {
        @Override
        protected Api computeValue(Class<?> type) {
            return Api.of(type);
        }
    };

    private static final class Api {
        Class<?> container;
        Class<?> orientation;
        Method fill;
        Method empty;
        Method quantity;
        Method id;

        static Api of(Class<?> type) {
            Api a = new Api();
            for (Class<?> c = type; c != null && a.container == null; c = c.getSuperclass()) a.container = find(c);
            if (a.container == null) return a;
            try {
                a.orientation = Class.forName("buildcraft.api.Orientations", false, type.getClassLoader());
                a.fill = a.container.getMethod("fill", a.orientation, int.class, int.class, boolean.class);
                a.empty = a.container.getMethod("empty", int.class, boolean.class);
                a.quantity = a.container.getMethod("getLiquidQuantity");
                a.id = a.container.getMethod("getLiquidId");
            } catch (ReflectiveOperationException | LinkageError e) {
                a.container = null;
            }
            return a;
        }

        private static Class<?> find(Class<?> c) {
            if (c.getName().equals("buildcraft.api.ILiquidContainer")) return c;
            for (Class<?> i : c.getInterfaces()) {
                Class<?> f = find(i);
                if (f != null) return f;
            }
            return null;
        }

        Object orientation(Direction side) {
            return Orientations125.of(orientation, side);
        }
    }

    /** The fluid handler of a tile implementing {@code buildcraft.api.ILiquidContainer}, or null for other tiles. */
    public static IFluidHandler forTile(Object legacy, Direction side) {
        if (legacy == null) return null;
        Api api = API.get(legacy.getClass());
        return api.container == null || !api.container.isInstance(legacy) ? null : new Handler(legacy, side, api);
    }

    private record Handler(Object tile, Direction side, Api api) implements IFluidHandler {
        private int call(Method m, Object... args) {
            try {
                return ((Number) m.invoke(tile, args)).intValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            int amount = call(api.quantity);
            if (amount <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            net.neoforged.neoforge.fluids.FluidStack stack = org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.toModern(
                    Liquids164.toFluid(new LiquidStack(call(api.id), amount, 0)));
            return stack == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : stack;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Math.max(1000, call(api.quantity) + 1000);
        }

        @Override
        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return liquidId(stack) >= 0;
        }

        private int liquidId(net.neoforged.neoforge.fluids.FluidStack stack) {
            FluidStack legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.fromModern(stack);
            LiquidStack liquid = Liquids164.toLiquid(legacy);
            return liquid == null ? -1 : liquid.itemID;
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            int id = liquidId(resource);
            if (id < 0 || resource.isEmpty()) return 0;
            return call(api.fill, api.orientation(side), resource.getAmount(), id, action.execute());
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            net.neoforged.neoforge.fluids.FluidStack have = getFluidInTank(0);
            if (have.isEmpty() || !net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(have, resource)) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            net.neoforged.neoforge.fluids.FluidStack have = getFluidInTank(0);
            if (have.isEmpty() || maxDrain <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            int taken = call(api.empty, maxDrain, action.execute());
            return taken <= 0 ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : have.copyWithAmount(taken);
        }
    }
}
