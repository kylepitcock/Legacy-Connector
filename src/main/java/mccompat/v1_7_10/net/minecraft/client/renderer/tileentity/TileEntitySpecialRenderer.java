package mccompat.v1_7_10.net.minecraft.client.renderer.tileentity;

import mccompat.v1_7_10.net.minecraft.client.gui.FontRenderer;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;
import org.kylepitcock.mccompat.v1_7_10.client.TextureLocations;

public abstract class TileEntitySpecialRenderer {
    protected TileEntityRendererDispatcher field_147501_a;

    public abstract void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks);

    protected void bindTexture(ResourceLocation location) {
        RenderContext.bindTexture(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
    }

    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        this.field_147501_a = dispatcher;
    }

    public void onWorldChange(World world) {
    }

    public FontRenderer func_147498_b() {
        return field_147501_a == null ? FontRenderer.INSTANCE : field_147501_a.getFontRenderer();
    }
}
