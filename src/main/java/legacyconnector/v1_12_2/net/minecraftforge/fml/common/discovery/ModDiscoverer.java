package legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * FML's mod discoverer. The game finds and sorts the mods before any mod code runs, so this only answers what discovery produced: the
 * annotation table built from the game's scan data and the candidates it found per package.
 */
public class ModDiscoverer {
    private final ASMDataTable discoveredTable = ASMDataTable.mcc$get();

    public ASMDataTable getASMTable() {
        return discoveredTable;
    }

    /** Libraries that are no mods: the layer does not tell them from mod jars, so none. */
    public List<ModCandidate> getNonModLibs() {
        return new ArrayList<>();
    }
}
