package org.kylepitcock.legacyconnector.v1_7_10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Mods serialize legacy stacks with Gson (a wrapper class holding an Object field, a config library): reflection used to walk into
 * the modern stack the shim object views (registries, threads) and throw. Shared by the 1.6.4 forward layer.
 */
class LegacyGson1710Test {
    @BeforeAll
    static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        // the legacy-meta component is registered by the mod at load; a unit test binds a stand-in so stacks can read their damage
        var holder = org.kylepitcock.legacyconnector.core.data.CompatComponents.LEGACY_META;
        var field = net.neoforged.neoforge.registries.DeferredHolder.class.getDeclaredField("holder");
        field.setAccessible(true);
        field.set(holder, net.minecraft.core.Holder.direct(net.minecraft.core.component.DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).build()));
    }

    /** What a mod's data class looks like: an Object field that holds a stack. */
    static class Wrapper {
        String className = "ItemStack";
        int stackSize = 3;
        Object wrappedStack;
    }

    @Test
    void itemStackSurvivesGsonAsAnObjectField() {
        Gson gson = new GsonBuilder().create();
        ItemStack stack = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.stick, 5, 0);
        Wrapper w = new Wrapper();
        w.wrappedStack = stack;
        String json = gson.toJson(w);
        assertTrue(json.contains("\"stackSize\":5"), json);
        assertTrue(json.contains("\"itemID\""), json);
        assertTrue(json.contains("\"itemDamage\":0"), json);
        ItemStack back = gson.fromJson(gson.toJsonTree(stack), ItemStack.class);
        assertSame(stack.getItem(), back.getItem());
        assertEquals(5, back.mcc$count());
        assertEquals(0, back.getItemDamage());
    }

    @Test
    void itemStackKeepsItsTag() {
        Gson gson = new Gson();
        ItemStack stack = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.stick, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", "steve");
        NBTTagList list = new NBTTagList();
        list.appendTag(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagInt(7));
        tag.setTag("values", list);
        stack.setTagCompound(tag);
        ItemStack back = gson.fromJson(gson.toJson(stack), ItemStack.class);
        assertNotNull(back.getTagCompound());
        assertEquals("steve", back.getTagCompound().getString("owner"));
        assertEquals(1, back.getTagCompound().getTagList("values", 3).tagCount());
    }

    @Test
    void nbtTreesDoNotLoopThroughTheirParentLinks() {
        Gson gson = new Gson();
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound child = new NBTTagCompound();
        child.setInteger("n", 4);
        root.setTag("child", child);
        NBTTagCompound back = gson.fromJson(gson.toJson(root), NBTTagCompound.class);
        assertEquals(4, back.getCompoundTag("child").getInteger("n"));
        assertFalse(gson.toJson(root).contains("mcc$"));
    }

    @Test
    void fluidStackRoundTrips() {
        Gson gson = new Gson();
        FluidStack stack = new FluidStack(FluidRegistry.WATER, 750);
        String json = gson.toJson(stack);
        assertTrue(json.contains("\"amount\":750"), json);
        assertTrue(json.contains("\"fluidID\""), json);
        FluidStack back = gson.fromJson(json, FluidStack.class);
        assertSame(FluidRegistry.WATER, back.fluid);
        assertEquals(750, back.amount);
    }

    @Test
    void itemFromTheRegistryNameResolvesLazily() {
        Gson gson = new Gson();
        String name = Item.itemRegistry.getNameForObject(legacyconnector.v1_7_10.net.minecraft.init.Items.stick);
        ItemStack back = gson.fromJson("{\"stackSize\":2,\"item\":\"" + name + "\",\"itemDamage\":0}", ItemStack.class);
        assertSame(legacyconnector.v1_7_10.net.minecraft.init.Items.stick, back.getItem());
        assertEquals(2, back.mcc$count());
    }
}
