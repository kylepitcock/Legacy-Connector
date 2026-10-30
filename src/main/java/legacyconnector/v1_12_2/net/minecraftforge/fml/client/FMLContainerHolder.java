package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer;

/** A resource pack that belongs to a mod (FML's mod jar and mod folder packs). */
public interface FMLContainerHolder {
    ModContainer getFMLContainer();
}
