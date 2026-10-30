package legacyconnector.v1_12_2.net.minecraftforge.common.property;

import com.google.common.base.Optional;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;

/** Forge's shared block properties: the animation state of animated (ASM) models, and listed properties as unlisted ones. */
public class Properties {
    /** True for the static part of an animated block's model, false for the animated part. */
    public static final PropertyBool StaticProperty = PropertyBool.create("static");

    /** The model state an animated tile entity's renderer applies. */
    public static final IUnlistedProperty<IModelState> AnimationProperty = new IUnlistedProperty<IModelState>() {
        @Override
        public String getName() {
            return "forge_animation";
        }

        @Override
        public boolean isValid(IModelState state) {
            return true;
        }

        @Override
        public Class<IModelState> getType() {
            return IModelState.class;
        }

        @Override
        public String valueToString(IModelState state) {
            return state.toString();
        }
    };

    public static <V extends Comparable<V>> IUnlistedProperty<V> toUnlisted(IProperty<V> property) {
        return new PropertyAdapter<>(property);
    }

    /** A listed property used as an unlisted one (its values are kept per state of an extended block state). */
    public static class PropertyAdapter<V extends Comparable<V>> implements IUnlistedProperty<V>, IProperty<V> {
        private final IProperty<V> parent;

        public PropertyAdapter(IProperty<V> parent) {
            this.parent = parent;
        }

        @Override
        public String getName() {
            return parent.getName();
        }

        @Override
        public boolean isValid(V value) {
            return parent.getAllowedValues().contains(value);
        }

        @Override
        public Class<V> getType() {
            return parent.getValueClass();
        }

        @Override
        public String valueToString(V value) {
            return parent.getName(value);
        }

        @Override
        public Optional<V> parseValue(String value) {
            return parent.parseValue(value);
        }

        @Override
        public java.util.Collection<V> getAllowedValues() {
            return parent.getAllowedValues();
        }

        @Override
        public Class<V> getValueClass() {
            return parent.getValueClass();
        }

        @Override
        public String getName(V value) {
            return parent.getName(value);
        }
    }
}
