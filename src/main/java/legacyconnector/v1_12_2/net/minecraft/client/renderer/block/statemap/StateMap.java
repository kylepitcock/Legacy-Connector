package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;

public class StateMap extends StateMapperBase {
    private final IProperty<?> name;
    private final String suffix;
    private final List<IProperty<?>> ignored;

    private StateMap(IProperty<?> name, String suffix, List<IProperty<?>> ignored) {
        this.name = name;
        this.suffix = suffix;
        this.ignored = ignored;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        Map<IProperty<?>, Comparable<?>> map = new LinkedHashMap<>(state.getProperties());
        String path;
        if (name == null) {
            path = state.getBlock().getRegistryName().toString();
        } else {
            path = ((IProperty) name).getName(map.remove(name));
        }
        if (suffix != null) path += suffix;
        for (IProperty<?> p : ignored) map.remove(p);
        return new ModelResourceLocation(path, getPropertyString(map));
    }

    public static class Builder {
        private IProperty<?> name;
        private String suffix;
        private final List<IProperty<?>> ignored = new java.util.ArrayList<>();

        public Builder withName(IProperty<?> builderPropertyIn) {
            this.name = builderPropertyIn;
            return this;
        }

        public Builder withSuffix(String builderSuffixIn) {
            this.suffix = builderSuffixIn;
            return this;
        }

        public Builder ignore(IProperty<?>... ignoreIn) {
            java.util.Collections.addAll(ignored, ignoreIn);
            return this;
        }

        public StateMap build() {
            return new StateMap(name, suffix, ignored);
        }
    }
}
