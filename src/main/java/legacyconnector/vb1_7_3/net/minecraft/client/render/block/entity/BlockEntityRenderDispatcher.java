package legacyconnector.vb1_7_3.net.minecraft.client.render.block.entity;

/**
 * Beta 1.7.3 {@code TileEntityRenderer} ({@code ll}): its {@link #renderers} map (field #0, which mods reach through
 * {@code ModLoader.getPrivateValue(ll.class, ll.a, 0)}) and its single {@link #INSTANCE}. A renderer put into the map is registered the way
 * {@code ModLoader.RegisterTileEntity(class, id, renderer)} registers one; the client renderer bridge draws it.
 */
public class BlockEntityRenderDispatcher {
    @SuppressWarnings({"rawtypes"})
    public java.util.Map renderers = new java.util.HashMap<Object, Object>() {
        @Override
        public Object put(Object type, Object renderer) {
            if (type instanceof Class<?> c && renderer instanceof BlockEntityRenderer r) org.kylepitcock.legacyconnector.vb1_7_3.ContentB173.registerTileEntityRenderer(c, r);
            return super.put(type, renderer);
        }
    };
    public static BlockEntityRenderDispatcher INSTANCE = new BlockEntityRenderDispatcher();
}
