package mccompat.v1_7_10.cpw.mods.fml.common.event;

/** Sent to {@code @Mod.EventHandler} methods right after the mod object is created, before preInit. */
public class FMLConstructionEvent extends FMLStateEvent {
    @Override
    public mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.CONSTRUCTED;
    }

    public mccompat.v1_7_10.cpw.mods.fml.common.discovery.ASMDataTable getASMHarvestedData() {
        return mccompat.v1_7_10.cpw.mods.fml.common.discovery.ASMDataTable.mcc$get();
    }
}
