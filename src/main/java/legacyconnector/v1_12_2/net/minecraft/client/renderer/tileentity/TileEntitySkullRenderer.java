package legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.level.block.SkullBlock;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 skull renderer ({@link #instance}): mob heads and player heads drawn by the modern skull renderer and models at the legacy
 * matrix (mods draw heads on graves, pedestals and mimics with {@link #renderSkull}). Typed with TileEntity as the shim has no
 * TileEntitySkull class.
 */
public class TileEntitySkullRenderer extends TileEntitySpecialRenderer<TileEntity> {
    public static TileEntitySkullRenderer instance = new TileEntitySkullRenderer();
    private static Map<SkullBlock.Type, SkullModelBase> models;

    @Override
    public void setRendererDispatcher(TileEntityRendererDispatcher rendererDispatcherIn) {
        super.setRendererDispatcher(rendererDispatcherIn);
        instance = this;
    }

    /** 1.12 skull types: 0 skeleton, 1 wither skeleton, 2 zombie, 3 player, 4 creeper, 5 dragon. */
    private static SkullBlock.Type type(int skullType) {
        return switch (skullType) {
            case 1 -> SkullBlock.Types.WITHER_SKELETON;
            case 2 -> SkullBlock.Types.ZOMBIE;
            case 3 -> SkullBlock.Types.PLAYER;
            case 4 -> SkullBlock.Types.CREEPER;
            case 5 -> SkullBlock.Types.DRAGON;
            default -> SkullBlock.Types.SKELETON;
        };
    }

    /**
     * A skull at (x, y, z) (the block corner): on the floor turned by {@code rotationIn} degrees, or on the wall facing {@code facing};
     * {@code animateTicks} opens the dragon's jaw.
     */
    public void renderSkull(float x, float y, float z, EnumFacing facing, float rotationIn, int skullType, GameProfile profile, int destroyStage, float animateTicks) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null) return;
        try {
            if (models == null) models = SkullBlockRenderer.createSkullRenderers(net.minecraft.client.Minecraft.getInstance().getEntityModels());
            SkullBlock.Type type = type(skullType);
            SkullModelBase model = models.get(type);
            if (model == null) return;
            var renderType = SkullBlockRenderer.getRenderType(type, profile == null ? null : new net.minecraft.world.item.component.ResolvableProfile(profile));
            net.minecraft.core.Direction direction = facing == null || facing == EnumFacing.UP || facing == EnumFacing.DOWN ? null : facing.mcc$modern();
            pose.pushPose();
            try {
                pose.translate(x, y, z);
                SkullBlockRenderer.renderSkull(direction, rotationIn, animateTicks, pose, buffers, RenderContext.vertexLight(), model, renderType);
            } finally {
                pose.popPose();
            }
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a skull", e);
        }
    }
}
