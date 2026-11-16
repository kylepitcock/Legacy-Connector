package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import legacyconnector.v1_6_4.net.minecraftforge.liquids.LiquidStack;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The liquid API BuildCraft 3.2 shipped inside Forestry 1.4.8.x for Minecraft 1.2.5 ({@code buildcraft.api.liquids.ITankContainer}, {@code ILiquidTank},
 * {@code LiquidStack(itemID, amount, itemMeta)}; read from the bundled classes with {@code javap}) as a NeoForge fluid handler. It is not Forge's later
 * {@code net.minecraftforge.liquids.ITankContainer} that {@link Liquids164} handles: the classes ship inside the mods, so they are found by name on the tile's
 * class and called by reflection. A liquid is the id of its block or item, mapped to a fluid the way {@link Liquids164} does.
 */
public final class LiquidsBc32 {
    private LiquidsBc32() {}

    private static final ClassValue<Api> API = new ClassValue<>() {
        @Override
        protected Api computeValue(Class<?> type) {
            return Api.of(type);
        }
    };

    private static final class Api {
        Class<?> container;
        Class<?> orientation;
        Class<?> stackType;
        Method fill;
        Method drain;
        Method tanks;
        Method tankLiquid;
        Method tankCapacity;
        Field itemId;
        Field amount;
        Field itemMeta;
        Constructor<?> newStack;

        static Api of(Class<?> type) {
            Api a = new Api();
            for (Class<?> c = type; c != null && a.container == null; c = c.getSuperclass()) a.container = find(c);
            if (a.container == null) return a;
            try {
                ClassLoader loader = a.container.getClassLoader();
                a.orientation = Class.forName("buildcraft.api.Orientations", false, loader);
                a.stackType = Class.forName("buildcraft.api.liquids.LiquidStack", false, loader);
                Class<?> tank = Class.forName("buildcraft.api.liquids.ILiquidTank", false, loader);
                a.fill = a.container.getMethod("fill", a.orientation, a.stackType, boolean.class);
                a.drain = a.container.getMethod("drain", a.orientation, int.class, boolean.class);
                a.tanks = a.container.getMethod("getTanks");
                a.tankLiquid = tank.getMethod("getLiquid");
                a.tankCapacity = tank.getMethod("getCapacity");
                a.itemId = a.stackType.getField("itemID");
                a.amount = a.stackType.getField("amount");
                a.itemMeta = a.stackType.getField("itemMeta");
                a.newStack = a.stackType.getConstructor(int.class, int.class, int.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                a.container = null;
            }
            return a;
        }

        private static Class<?> find(Class<?> c) {
            if (c.getName().equals("buildcraft.api.liquids.ITankContainer")) return c;
            for (Class<?> i : c.getInterfaces()) {
                Class<?> f = find(i);
                if (f != null) return f;
            }
            return null;
        }
    }

    /** The fluid handler of a tile implementing {@code buildcraft.api.liquids.ITankContainer}, or null for other tiles. */
    public static IFluidHandler forTile(Object legacy, Direction side) {
        if (legacy == null) return null;
        Api api = API.get(legacy.getClass());
        return api.container == null || !api.container.isInstance(legacy) ? null : new Handler(legacy, side, api);
    }

    /** The liquid's id, amount and metadata as a 1.5.2-style liquid stack (the form {@link Liquids164} maps to fluids); null for no liquid. */
    static LiquidStack liquid(int itemId, int amount, int meta) {
        return amount <= 0 ? null : new LiquidStack(itemId, amount, meta);
    }

    private record Handler(Object tile, Direction side, Api api) implements IFluidHandler {
        private Object[] tankArray() {
            try {
                Object r = api.tanks.invoke(tile);
                return r instanceof Object[] a ? a : new Object[0];
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return new Object[0];
            }
        }

        @Override
        public int getTanks() {
            return tankArray().length;
        }

        private net.neoforged.neoforge.fluids.FluidStack fromLegacy(Object stack) {
            if (stack == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            try {
                LiquidStack liquid = liquid(api.itemId.getInt(stack), api.amount.getInt(stack), api.itemMeta.getInt(stack));
                if (liquid == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
                net.neoforged.neoforge.fluids.FluidStack modern = org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.toModern(Liquids164.toFluid(liquid));
                return modern == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : modern;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
        }

        private Object toLegacy(net.neoforged.neoforge.fluids.FluidStack stack, int amount) {
            if (stack.isEmpty()) return null;
            FluidStack legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge.fromModern(stack);
            LiquidStack liquid = Liquids164.toLiquid(legacy);
            if (liquid == null) return null;
            try {
                return api.newStack.newInstance(liquid.itemID, amount, liquid.itemMeta);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        private Object tank(int index) {
            Object[] all = tankArray();
            return index >= 0 && index < all.length ? all[index] : null;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            Object t = tank(tank);
            if (t == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            try {
                return fromLegacy(api.tankLiquid.invoke(t));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
        }

        @Override
        public int getTankCapacity(int tank) {
            Object t = tank(tank);
            if (t == null) return 0;
            try {
                return ((Number) api.tankCapacity.invoke(t)).intValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return toLegacy(stack, 1) != null;
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            Object legacy = toLegacy(resource, resource.getAmount());
            if (legacy == null) return 0;
            try {
                return ((Number) api.fill.invoke(tile, Orientations125.of(api.orientation, side), legacy, action.execute())).intValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            for (int i = 0; i < getTanks(); i++) {
                net.neoforged.neoforge.fluids.FluidStack have = getFluidInTank(i);
                if (!have.isEmpty() && net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(have, resource)) return drain(resource.getAmount(), action);
            }
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            try {
                return fromLegacy(api.drain.invoke(tile, Orientations125.of(api.orientation, side), maxDrain, action.execute()));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
        }
    }
}
