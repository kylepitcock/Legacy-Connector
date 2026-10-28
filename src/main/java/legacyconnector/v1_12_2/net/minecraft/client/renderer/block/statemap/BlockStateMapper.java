package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader;

/** 1.12.2 block state mapper registry: each block's model location per state, from its custom mapper or the default one. */
public class BlockStateMapper {
    private final Set<Block> setBuiltInBlocks = new java.util.HashSet<>();

    public void registerBlockStateMapper(Block blockIn, IStateMapper stateMapper) {
        ModelLoader.setCustomStateMapper(blockIn, stateMapper);
    }

    public void registerBuiltInBlocks(Block... blockIn) {
        java.util.Collections.addAll(setBuiltInBlocks, blockIn);
    }

    public Map<IBlockState, ModelResourceLocation> putAllStateModelLocations() {
        Map<IBlockState, ModelResourceLocation> map = Maps.newIdentityHashMap();
        for (Block block : legacyconnector.v1_12_2.net.minecraft.block.Block.REGISTRY) map.putAll(getVariants(block));
        return map;
    }

    /** The blockstate files a block's states come from. */
    public Set<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation> getBlockstateLocations(Block blockIn) {
        Set<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation> out = new java.util.LinkedHashSet<>();
        for (ModelResourceLocation mrl : getVariants(blockIn).values()) out.add(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(mrl.getNamespace(), mrl.getPath()));
        return out;
    }

    public Map<IBlockState, ModelResourceLocation> getVariants(Block blockIn) {
        if (setBuiltInBlocks.contains(blockIn)) return java.util.Collections.emptyMap();
        IStateMapper mapper = ModelLoader.mcc$stateMapper(blockIn);
        if (mapper == null) mapper = new DefaultStateMapper();
        try {
            return mapper.putStateModelLocations(blockIn);
        } catch (RuntimeException | LinkageError e) {
            return java.util.Collections.emptyMap();
        }
    }
}
