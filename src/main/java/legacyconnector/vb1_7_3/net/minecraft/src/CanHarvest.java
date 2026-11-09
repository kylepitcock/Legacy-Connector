package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/** Shared custom minimum-harvest declarations from the Beta ScotTools API. */
public class CanHarvest {
    private static final Map<Block,BitSet> LEVELS=new IdentityHashMap<>();
    private static final CanHarvest INSTANCE=new CanHarvest();
    public static final CanHarvest getInstance(){return INSTANCE;}
    /** The original public constructor replaces the shared declaration lists. */
    public CanHarvest(){synchronized(CanHarvest.class){LEVELS.clear();}}
    public static synchronized void addBlocks(Block[] blocks,int level){
        if(level<1||level>6)return;
        for(Block block:blocks){
            if(block==null)throw new RuntimeException("Invalid block minimum harvesting level declared!");
            LEVELS.computeIfAbsent(block,ignored->new BitSet(7)).set(level);
        }
    }
    public static synchronized int getBlockLevel(Block block){
        var levels=LEVELS.get(block);if(levels==null)return 0;
        // v10.1 declares six lists, but its lookup loop visits levels 1 through 5 only.
        for(int level=5;level>=1;level--)if(levels.get(level))return level;
        return 0;
    }
    public static synchronized boolean isDeclared(Block block){return LEVELS.containsKey(block);}
}
