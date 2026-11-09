package org.kylepitcock.legacyconnector.vb1_7_3;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;

/** Beta recipe metadata compares the old integer only; unrelated native stack components remain compatible. */
public record BetaMetadataIngredient(Item item,int metadata) implements ICustomIngredient {
    public static final MapCodec<BetaMetadataIngredient> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(BetaMetadataIngredient::item),
            Codec.INT.fieldOf("metadata").forGetter(BetaMetadataIngredient::metadata)).apply(i,BetaMetadataIngredient::new));
    public static final DeferredRegister<IngredientType<?>> REGISTER=DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,"legacyconnector");
    public static final DeferredHolder<IngredientType<?>,IngredientType<BetaMetadataIngredient>> TYPE=REGISTER.register("beta_metadata",()->new IngredientType<>(CODEC));
    @Override public boolean test(ItemStack stack){
        if(!stack.is(item))return false;
        var state=stack.get(DataComponents.BLOCK_STATE);
        int actual;
        if(stack.getItem() instanceof net.minecraft.world.item.BlockItem block&&block.getBlock() instanceof LegacyB173Block)
            actual=state==null||state.get(LegacyB173Block.META)==null?0:state.get(LegacyB173Block.META);
        else if(stack.isDamageableItem())actual=stack.getDamageValue();
        else actual=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("b173_meta");
        return actual==metadata;
    }
    @Override public Stream<ItemStack> getItems(){
        var stack=new ItemStack(item);if(stack.isDamageableItem())stack.setDamageValue(metadata);
        else if(stack.getItem() instanceof net.minecraft.world.item.BlockItem block&&block.getBlock() instanceof LegacyB173Block)
            stack.set(DataComponents.BLOCK_STATE,net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(LegacyB173Block.META,metadata&15));
        else if(metadata!=0){var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("b173_meta",metadata);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
        return Stream.of(stack);
    }
    @Override public boolean isSimple(){return false;}
    @Override public IngredientType<?> getType(){return TYPE.get();}
}
