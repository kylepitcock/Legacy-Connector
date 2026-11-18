package org.kylepitcock.legacyconnector.v1_12_2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 1.12.2 stacks, fluid stacks and NBT go through Gson without walking into the modern objects the shim classes view. */
class LegacyGson1122Test {
    @BeforeAll
    static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    static class Wrapper {
        int stackSize = 2;
        Object wrappedStack;
    }

    @Test
    void itemStackWithTagRoundTripsAsAnObjectField() {
        Gson gson = new Gson();
        ItemStack stack = new ItemStack(Items.STICK, 4, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", "alex");
        stack.setTagCompound(tag);
        Wrapper w = new Wrapper();
        w.wrappedStack = stack;
        String json = gson.toJson(w);
        assertTrue(json.contains("\"stackSize\":4"), json);
        assertFalse(json.contains("mcc$"), json);
        ItemStack back = gson.fromJson(gson.toJson(stack), ItemStack.class);
        assertSame(Items.STICK, back.getItem());
        assertEquals(4, back.getCount());
        assertNotNull(back.getTagCompound());
        assertEquals("alex", back.getTagCompound().getString("owner"));
    }

    @Test
    void fluidStackRoundTrips() {
        Gson gson = new Gson();
        FluidStack stack = new FluidStack(FluidRegistry.WATER, 1000);
        String json = gson.toJson(stack);
        assertTrue(json.contains("\"amount\":1000"), json);
        FluidStack back = gson.fromJson(json, FluidStack.class);
        assertSame(FluidRegistry.WATER, back.getFluid());
        assertEquals(1000, back.amount);
    }

    @Test
    void nbtTreesDoNotLoopThroughTheirParentLinks() {
        Gson gson = new Gson();
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound child = new NBTTagCompound();
        child.setInteger("n", 9);
        root.setTag("child", child);
        NBTTagCompound back = gson.fromJson(gson.toJson(root), NBTTagCompound.class);
        assertEquals(9, back.getCompoundTag("child").getInteger("n"));
    }
}
