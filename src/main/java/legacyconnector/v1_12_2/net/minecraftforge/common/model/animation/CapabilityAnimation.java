package legacyconnector.v1_12_2.net.minecraftforge.common.model.animation;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;

/** The capability through which tile entities and items give their animation state machine to the animated model renderer. */
public class CapabilityAnimation {
    @CapabilityInject(IAnimationStateMachine.class)
    public static Capability<IAnimationStateMachine> ANIMATION_CAPABILITY = null;

    static {
        register();
    }

    public static synchronized void register() {
        if (ANIMATION_CAPABILITY != null) return;
        CapabilityManager.INSTANCE.register(IAnimationStateMachine.class, new Capability.IStorage<IAnimationStateMachine>() {
            @Override
            public NBTBase writeNBT(Capability<IAnimationStateMachine> capability, IAnimationStateMachine instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IAnimationStateMachine> capability, IAnimationStateMachine instance, EnumFacing side, NBTBase nbt) {
            }
        }, () -> {
            throw new UnsupportedOperationException("An animation state machine is loaded from its model's json");
        });
        ANIMATION_CAPABILITY = CapabilityManager.INSTANCE.mcc$capability(IAnimationStateMachine.class);
    }

    /** An item's provider of its state machine. */
    public static class DefaultItemAnimationCapabilityProvider implements ICapabilityProvider {
        private final IAnimationStateMachine asm;

        public DefaultItemAnimationCapabilityProvider(IAnimationStateMachine asm) {
            this.asm = asm;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == ANIMATION_CAPABILITY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == ANIMATION_CAPABILITY ? ANIMATION_CAPABILITY.cast(asm) : null;
        }
    }
}
