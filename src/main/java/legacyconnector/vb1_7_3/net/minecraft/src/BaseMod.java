package legacyconnector.vb1_7_3.net.minecraft.src;

/**
 * Risugami's {@code BaseMod} for Beta 1.7.3. A mod is constructed once ({@code PlatformB173}) and registers its content from its
 * constructor; {@link #ModsLoaded()} follows once every mod is constructed. World generation overrides become native chunk features.
 * Other hooks that take game objects (ticks,
 * rendering, crafting callbacks) are not declared here yet: the mods' own implementations are found by name by the runtime as it gains
 * them, and their {@code super} calls reach synthesized defaults.
 */
public abstract class BaseMod {
    public BaseMod() {}

    public int AddFuel(int id) {
        return 0;
    }

    public void AddRenderer(java.util.Map<?, ?> renderers) {}
    public void RegisterAnimation(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft){}
    public void KeyboardEvent(legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding key){}
    public boolean OnTickInGame(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft){return false;}
    public boolean OnTickInGUI(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,
            legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen screen){return false;}
    public boolean RenderWorldBlock(legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager renderer,
            legacyconnector.vb1_7_3.net.minecraft.world.BlockView world,int x,int y,int z,
            legacyconnector.vb1_7_3.net.minecraft.block.Block block,int model){return false;}

    /** Draws a block of a mod's own render type as an item (inventory, hand, ground); the original draws around the origin. */
    public void RenderInvBlock(legacyconnector.vb1_7_3.net.minecraft.client.render.block.BlockRenderManager renderer,
            legacyconnector.vb1_7_3.net.minecraft.block.Block block,int metadata,int model){}

    public void ModsLoaded() {}
    public void TakenFromCrafting(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){}
    public void TakenFromFurnace(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){}

    public void GenerateSurface(legacyconnector.vb1_7_3.net.minecraft.world.World world,java.util.Random random,int blockX,int blockZ) {}
    public void GenerateNether(legacyconnector.vb1_7_3.net.minecraft.world.World world,java.util.Random random,int blockX,int blockZ) {}

    public abstract String Version();

    @Override
    public String toString() {
        return getClass().getName() + " " + Version();
    }
}
