package buildcraft.api.power;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

public interface IPowerReceptor {
    IPowerProvider getPowerProvider();

    int powerRequest(ForgeDirection from);
}
