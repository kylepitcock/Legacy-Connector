package legacyconnector.v1_6_4.facade.net.minecraftforge.event.terraingen;

/**
 * Facade of the 1.7.10 shim's {@code ChunkProviderEvent.ReplaceBiomeBlocks} for 1.4.7 to 1.6.4 mods: the event's {@code blockArray} was one id per block
 * ({@code byte[]}), 1.7.10's is an array of blocks. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class ChunkProviderEvent$ReplaceBiomeBlocks {
    private ChunkProviderEvent$ReplaceBiomeBlocks() {}

    public static byte[] mcc$get$blockArray(legacyconnector.v1_7_10.net.minecraftforge.event.terraingen.ChunkProviderEvent.ReplaceBiomeBlocks self) {
        return self.mcc$idArray();
    }
}
