package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** 1.12.2 multipart {@code OR}: any of the conditions. */
public class ConditionOr implements ICondition {
    final Iterable<ICondition> conditions;

    public ConditionOr(Iterable<ICondition> conditionsIn) {
        this.conditions = conditionsIn;
    }

    @Override
    public Predicate<IBlockState> getPredicate(BlockStateContainer blockState) {
        List<Predicate<IBlockState>> list = new ArrayList<>();
        for (ICondition c : conditions) list.add(c.getPredicate(blockState));
        return Predicates.or(list);
    }
}
