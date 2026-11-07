package legacyconnector.v1_12_2.mcp.mobius.waila.api;

/** Custom tooltip widget. Accepted and ignored: Jade draws its own tooltip. */
public interface IWailaTooltipRenderer {
    java.awt.Dimension getSize(String[] params, IWailaCommonAccessor accessor);

    void draw(String[] params, IWailaCommonAccessor accessor);
}
