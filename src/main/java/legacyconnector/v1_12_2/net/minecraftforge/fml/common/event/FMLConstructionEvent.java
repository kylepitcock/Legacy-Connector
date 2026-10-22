package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

/** Sent to {@code @Mod.EventHandler} methods right after the mod object is created, before preInit. */
public class FMLConstructionEvent extends FMLStateEvent {
    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.CONSTRUCTED;
    }

    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable getASMHarvestedData() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable.mcc$get();
    }
}
