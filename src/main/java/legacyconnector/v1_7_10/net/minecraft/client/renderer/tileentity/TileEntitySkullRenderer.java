package legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelSkeletonHead;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySkull;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's mob head renderer. Mods draw heads with it on their own models (player heads on robots and mannequins, trophies)
 * through {@link #field_147536_b}, and extend it for their own skulls. Types: 0 skeleton, 1 wither skeleton, 2 zombie,
 * 3 player (skin of the profile), 4 creeper.
 */
public class TileEntitySkullRenderer extends TileEntitySpecialRenderer {
    private static final ResourceLocation field_147537_c = new ResourceLocation("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation field_147532_f = new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");
    private static final ResourceLocation field_147535_e = new ResourceLocation("textures/entity/zombie/zombie.png");
    private static final ResourceLocation field_147534_d = new ResourceLocation("textures/entity/creeper/creeper.png");
    private final ModelSkeletonHead field_147533_g = new ModelSkeletonHead(0, 0, 64, 32);
    private final ModelSkeletonHead field_147538_h = new ModelSkeletonHead(0, 0, 64, 64);
    /**
     * The registered instance: vanilla's until a mod binds its own skull renderer (the dispatcher hands it the dispatcher,
     * see {@link #func_147497_a}).
     */
    public static TileEntitySkullRenderer field_147536_b = new TileEntitySkullRenderer();

    public void renderTileEntityAt(TileEntitySkull skull, double x, double y, double z, float partialTicks) {
        func_152674_a((float) x, (float) y, (float) z, skull.getBlockMetadata() & 7, skull.func_145906_b() * 360 / 16.0F, skull.getSkullType(),
                mcc$profile(skull.func_145907_c()));
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (te instanceof TileEntitySkull skull) renderTileEntityAt(skull, x, y, z, partialTicks);
    }

    @Override
    public void func_147497_a(TileEntityRendererDispatcher dispatcher) {
        super.func_147497_a(dispatcher);
        field_147536_b = this;
    }

    /**
     * Draws a head with its bottom centre at (x, y, z) + (0.5, 0, 0.5) on the floor ({@code face} 1, turned by {@code rotation}
     * degrees), or against the wall of side {@code face} 2-5.
     */
    public void func_152674_a(float x, float y, float z, int face, float rotation, int type, GameProfile profile) {
        ModelSkeletonHead model = field_147533_g;
        switch (type) {
            case 1 -> bindTexture(field_147532_f);
            case 2 -> {
                bindTexture(field_147535_e);
                model = field_147538_h;
            }
            case 3 -> {
                model = field_147538_h;
                RenderContext.bindTexture(mcc$skin(profile));
            }
            case 4 -> bindTexture(field_147534_d);
            default -> bindTexture(field_147537_c);
        }
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (face != 1) {
            switch (face) {
                case 2 -> GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.74F);
                case 3 -> {
                    GL11.glTranslatef(x + 0.5F, y + 0.25F, z + 0.26F);
                    rotation = 180.0F;
                }
                case 4 -> {
                    GL11.glTranslatef(x + 0.74F, y + 0.25F, z + 0.5F);
                    rotation = 270.0F;
                }
                default -> {
                    GL11.glTranslatef(x + 0.26F, y + 0.25F, z + 0.5F);
                    rotation = 90.0F;
                }
            }
        } else {
            GL11.glTranslatef(x + 0.5F, y, z + 0.5F);
        }
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        model.render(null, 0.0F, 0.0F, 0.0F, rotation, 0.0F, 0.0625F);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    /** The skin of a profile (the default skin until the modern skin manager has it). */
    private static net.minecraft.resources.ResourceLocation mcc$skin(GameProfile profile) {
        if (profile != null) {
            try {
                return net.minecraft.client.Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
            } catch (RuntimeException e) {
                // fall back to the default skin
            }
        }
        return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }

    /** A head with an owner name: the profile of that player as the client knows it (null when unknown). */
    private static GameProfile mcc$profile(String owner) {
        if (owner == null || owner.isEmpty()) return null;
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) {
            for (var info : connection.getOnlinePlayers()) {
                if (owner.equalsIgnoreCase(info.getProfile().getName())) return info.getProfile();
            }
        }
        return null;
    }
}
