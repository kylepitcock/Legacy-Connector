package legacyconnector.v1_7_10.codechicken.nei;

/** Which parts of NEI's overlay a GUI wants shown (the JEI bridge draws its own, so these only record the request). */
public class VisiblityData {
    public boolean showNEI = true;
    public boolean showItemPanel = true;
    public boolean showItemSection = true;
    public boolean showSearchSection = true;
    public boolean showStateButtons = true;
    public boolean showUtilityButtons = true;
    public boolean showWidgets = true;
    public boolean enableDeleteMode = true;

    public VisiblityData() {
    }

    public VisiblityData(boolean showNEI) {
        this.showNEI = showNEI;
    }
}
