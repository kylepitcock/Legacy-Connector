package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.VariantList;

/** 1.12.2 multipart blockstate: its parts, evaluated against the block's state container. */
public class Multipart {
    private final List<Selector> selectors;
    private BlockStateContainer stateContainer;

    public Multipart(List<Selector> selectorsIn) {
        this.selectors = selectorsIn;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public Set<VariantList> getVariants() {
        Set<VariantList> set = new LinkedHashSet<>();
        for (Selector s : selectors) set.add(s.getVariantList());
        return set;
    }

    public void setStateContainer(BlockStateContainer stateContainerIn) {
        this.stateContainer = stateContainerIn;
    }

    public BlockStateContainer getStateContainer() {
        return stateContainer;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Multipart other && selectors.equals(other.selectors)
                && (stateContainer == null ? other.stateContainer == null : stateContainer.equals(other.stateContainer));
    }

    @Override
    public int hashCode() {
        return 31 * selectors.hashCode() + (stateContainer == null ? 0 : stateContainer.hashCode());
    }

    /** The {@code multipart} array of a blockstate file. */
    public static Multipart mcc$fromJson(JsonArray json) {
        List<Selector> list = new ArrayList<>();
        for (JsonElement e : json) list.add(Selector.mcc$fromJson(e.getAsJsonObject()));
        return new Multipart(list);
    }
}
