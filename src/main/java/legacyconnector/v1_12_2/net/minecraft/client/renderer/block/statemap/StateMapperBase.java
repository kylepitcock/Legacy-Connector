package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;

public abstract class StateMapperBase implements IStateMapper {
    protected Map<IBlockState, ModelResourceLocation> mapStateModelLocations = new LinkedHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public String getPropertyString(Map<IProperty<?>, Comparable<?>> values) {
        StringBuilder sb = new StringBuilder();
        java.util.List<Map.Entry<IProperty<?>, Comparable<?>>> entries = new java.util.ArrayList<>(values.entrySet());
        entries.sort((a, b) -> a.getKey().getName().compareTo(b.getKey().getName()));
        for (var e : entries) {
            if (sb.length() != 0) sb.append(',');
            sb.append(e.getKey().getName()).append('=').append(((IProperty) e.getKey()).getName((Comparable) e.getValue()));
        }
        if (sb.length() == 0) sb.append("normal");
        return sb.toString();
    }

    @Override
    public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block blockIn) {
        for (IBlockState state : blockIn.getBlockState().getValidStates()) mapStateModelLocations.put(state, getModelResourceLocation(state));
        return mapStateModelLocations;
    }

    protected abstract ModelResourceLocation getModelResourceLocation(IBlockState state);
}
