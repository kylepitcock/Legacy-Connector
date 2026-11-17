package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import legacyconnector.v1_12_2.net.minecraft.block.Block;

/**
 * 1.12's {@code EntityEnderman.CARRIABLE_BLOCKS} as a live set over the carriable-block registry that {@code setCarriable} fills
 * (vanilla's blocks included): mods add and remove blocks from the set directly. Touches {@link RuntimeApis1122} only when used, so the
 * enderman class can hold it while the shim loads.
 */
public final class CarriableView1122 extends AbstractSet<Block> {
    @Override
    public boolean contains(Object o) {
        return o instanceof Block b && RuntimeApis1122.mcc$isCarriable(b);
    }

    @Override
    public boolean add(Block block) {
        if (block == null || contains(block)) return false;
        RuntimeApis1122.mcc$setCarriable(block, true);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!contains(o)) return false;
        RuntimeApis1122.mcc$setCarriable((Block) o, false);
        return true;
    }

    @Override
    public Iterator<Block> iterator() {
        var snapshot = new ArrayList<Block>();
        for (Block block : Block.REGISTRY) if (RuntimeApis1122.mcc$isCarriable(block)) snapshot.add(block);
        var it = snapshot.iterator();
        return new Iterator<>() {
            private Block last;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Block next() {
                return last = it.next();
            }

            @Override
            public void remove() {
                if (last != null) RuntimeApis1122.mcc$setCarriable(last, false);
            }
        };
    }

    @Override
    public int size() {
        int n = 0;
        for (Block block : Block.REGISTRY) if (RuntimeApis1122.mcc$isCarriable(block)) n++;
        return n;
    }
}
