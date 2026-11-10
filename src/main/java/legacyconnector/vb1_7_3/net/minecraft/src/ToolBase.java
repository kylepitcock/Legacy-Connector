package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Original r8 family tables, shared and mutable across original tool registrations. */
public class ToolBase {
    public static final ToolBase Pickaxe=new ToolBase(),Shovel=new ToolBase(),Axe=new ToolBase();
    public ArrayList<BlockHarvestPower> mineBlocks=new ArrayList<>();
    public ArrayList<Material> mineMaterials=new ArrayList<>();
    static {
        add(Pickaxe,20,1,4,16,21,22,23,24,43,44,45,48,52,61,62,67,77,79,87,89,93,94);
        add(Pickaxe,40,15,42,71);add(Pickaxe,60,14,41,56,57,73,74);add(Pickaxe,80,49);
        add(Shovel,20,2,3,12,13,78,80,82);
        add(Axe,20,5,17,25,47,53,54,58,63,64,65,66,68,69,81,84,85);
    }
    private static void add(ToolBase family,float power,int... ids){for(int id:ids)family.mineBlocks.add(new BlockHarvestPower(id,power));}
    public boolean canHarvest(Block block,float currentPower){
        for(Material material:mineMaterials)if(material==block.material)return true;
        // The original r8 predicate is deliberately OR, including for unrelated table rows.
        for(BlockHarvestPower row:mineBlocks)if(block.id==row.blockID||currentPower>=row.percentage)return true;
        return false;
    }
}
