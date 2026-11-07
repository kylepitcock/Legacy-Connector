package legacyconnector.v1_6_4.net.minecraft.src;

import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Mod;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLInitializationEvent;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLPostInitializationEvent;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.Render;
import legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * ModLoader's mod base class (Forge 1.4.7's {@code net.minecraft.src.BaseMod}); a mod is a {@code mod_*} subclass in the default package of a
 * reobfuscated jar (the loader maps that name here). FML ran a ModLoader mod's {@code load()} in its init phase and {@code modsLoaded()} in
 * post-init; the two handlers below are those phases, found by the platform's lifecycle dispatch like a mod's own {@code @Mod.EventHandler}s.
 * What the hooks do is in {@link ModLoader}.
 */
public abstract class BaseMod {
    public BaseMod() {}

    @Mod.EventHandler
    public void mcc$modLoaderInit(FMLInitializationEvent event) {
        ModLoader.mcc$load(this);
    }

    @Mod.EventHandler
    public void mcc$modLoaderPostInit(FMLPostInitializationEvent event) {
        ModLoader.mcc$modsLoaded(this);
    }

    public abstract String getVersion();

    public abstract void load();

    public void modsLoaded() {}

    public String getName() {
        return getClass().getSimpleName();
    }

    public String getPriorities() {
        return "";
    }

    public int addFuel(int id, int metadata) {
        return 0;
    }

    public void addRenderer(Map<Class<? extends Entity>, Render> renderers) {}

    public void generateNether(World world, Random random, int chunkX, int chunkZ) {}

    public void generateSurface(World world, Random random, int chunkX, int chunkZ) {}

    public GuiContainer getContainerGUI(EntityClientPlayerMP player, int containerID, int x, int y, int z) {
        return null;
    }

    public void keyboardEvent(KeyBinding event) {}

    public void onItemPickup(EntityPlayer player, ItemStack item) {}

    public boolean onTickInGame(float time, Minecraft minecraftInstance) {
        return false;
    }

    public boolean onTickInGame(MinecraftServer minecraftServer) {
        return false;
    }

    public boolean onTickInGUI(float tick, Minecraft game, GuiScreen gui) {
        return false;
    }

    public void registerAnimation(Minecraft game) {}

    public void renderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {}

    public boolean renderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
        return false;
    }

    public void takenFromCrafting(EntityPlayer player, ItemStack item, IInventory matrix) {}

    public void takenFromFurnace(EntityPlayer player, ItemStack item) {}

    public Entity spawnEntity(int entityId, World world, double scaledX, double scaledY, double scaledZ) {
        return null;
    }

    @Override
    public String toString() {
        return getName() + " " + getVersion();
    }
}
