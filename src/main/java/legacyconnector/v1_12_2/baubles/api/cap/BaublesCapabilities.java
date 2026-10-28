package legacyconnector.v1_12_2.baubles.api.cap;

import legacyconnector.v1_12_2.baubles.api.BaubleType;
import legacyconnector.v1_12_2.baubles.api.IBauble;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager;

/**
 * Baubles' capabilities: a player's Baubles slots ({@code player.getCapability(CAPABILITY_BAUBLES, null)}, answered by the player from
 * Curios) and an item's bauble behaviour.
 */
public class BaublesCapabilities {
    @CapabilityInject(IBaublesItemHandler.class)
    public static Capability<IBaublesItemHandler> CAPABILITY_BAUBLES = null;

    @CapabilityInject(IBauble.class)
    public static Capability<IBauble> CAPABILITY_ITEM_BAUBLE = null;

    static {
        CapabilityManager.INSTANCE.register(IBaublesItemHandler.class, new CapabilityBaubles<>(), () -> null);
        CapabilityManager.INSTANCE.register(IBauble.class, new CapabilityItemBaubleStorage(), () -> new BaubleItem(BaubleType.TRINKET));
        // the shim is not scanned for @CapabilityInject (mods' own injected fields are filled by the manager)
        CAPABILITY_BAUBLES = CapabilityManager.INSTANCE.mcc$capability(IBaublesItemHandler.class);
        CAPABILITY_ITEM_BAUBLE = CapabilityManager.INSTANCE.mcc$capability(IBauble.class);
    }

    /** Ensures the capabilities exist (the static initialiser registers them). */
    public static void mcc$init() {
    }

    public static class CapabilityBaubles<T extends IBaublesItemHandler> implements Capability.IStorage<IBaublesItemHandler> {
        @Override
        public NBTBase writeNBT(Capability<IBaublesItemHandler> capability, IBaublesItemHandler instance, EnumFacing side) {
            return null;
        }

        @Override
        public void readNBT(Capability<IBaublesItemHandler> capability, IBaublesItemHandler instance, EnumFacing side, NBTBase nbt) {
        }
    }

    public static class CapabilityItemBaubleStorage implements Capability.IStorage<IBauble> {
        @Override
        public NBTBase writeNBT(Capability<IBauble> capability, IBauble instance, EnumFacing side) {
            return null;
        }

        @Override
        public void readNBT(Capability<IBauble> capability, IBauble instance, EnumFacing side, NBTBase nbt) {
        }
    }
}
