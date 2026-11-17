package legacyconnector.v1_6_4.facade.net.minecraftforge.fluids;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code BlockFluidBase} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class BlockFluidBase {
    private BlockFluidBase() {}

    /** The id-keyed view of a fluid block's displacement table (the 1.7.10 table is keyed by block). */
    private static final class ById extends java.util.AbstractMap<Integer, Boolean> {
        private final java.util.Map<legacyconnector.v1_7_10.net.minecraft.block.Block, Boolean> table;

        ById(java.util.Map<legacyconnector.v1_7_10.net.minecraft.block.Block, Boolean> table) {
            this.table = table;
        }

        @Override
        public Boolean put(Integer id, Boolean value) {
            legacyconnector.v1_7_10.net.minecraft.block.Block b = Ids.block(id);
            return b == null ? null : table.put(b, value);
        }

        @Override
        public Boolean get(Object id) {
            legacyconnector.v1_7_10.net.minecraft.block.Block b = id instanceof Integer i ? Ids.block(i) : null;
            return b == null ? null : table.get(b);
        }

        @Override
        public boolean containsKey(Object id) {
            return get(id) != null;
        }

        @Override
        public Boolean remove(Object id) {
            legacyconnector.v1_7_10.net.minecraft.block.Block b = id instanceof Integer i ? Ids.block(i) : null;
            return b == null ? null : table.remove(b);
        }

        @Override
        public java.util.Set<java.util.Map.Entry<Integer, Boolean>> entrySet() {
            java.util.Set<java.util.Map.Entry<Integer, Boolean>> out = new java.util.LinkedHashSet<>();
            for (java.util.Map.Entry<legacyconnector.v1_7_10.net.minecraft.block.Block, Boolean> e : table.entrySet()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(Ids.blockId(e.getKey()), e.getValue()));
            }
            return out;
        }
    }

    /** 1.6.4 {@code displacementIds}: block id to whether this fluid displaces it, a live view of the fluid's own table. */
    public static java.util.Map<Integer, Boolean> mcc$get$displacementIds(legacyconnector.v1_7_10.net.minecraftforge.fluids.BlockFluidBase self) {
        return new ById(self.displacements);
    }
}
