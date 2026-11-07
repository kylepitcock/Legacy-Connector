package legacyconnector.v1_20_1.net.minecraftforge.versions.mcp;

/** Forge 1.20.1's MCP version holder; answers with the running Minecraft's version. */
public final class MCPVersion {
    private MCPVersion() {
    }

    public static String getMCVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().getName();
    }

    public static String getMCPVersion() {
        return getMCVersion();
    }

    public static String getMCPandMCVersion() {
        return getMCVersion();
    }
}
