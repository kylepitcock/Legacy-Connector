package legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;

/**
 * Vanilla's spinning-mob renderer of the mob spawner. It draws nothing here (the modern spawner block entity is drawn by the host), but the
 * class has to be a {@link TileEntitySpecialRenderer}: mods bind it to their own spawner tile entities (Twilight Forest's boss and
 * monster spawners), and a synthesized stand-in that is not a renderer failed the verification of the mod's client proxy.
 */
public class TileEntityMobSpawnerRenderer extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
    }
}
