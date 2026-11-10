package legacyconnector.vb1_7_3.net.minecraft.stat;

public class ItemOrBlockStat extends Stat {
    private final int itemOrBlockId;
    public ItemOrBlockStat(int id,String name,int itemOrBlockId){super(id,name);this.itemOrBlockId=itemOrBlockId;}
    public ItemOrBlockStat(int id,String name,int itemOrBlockId,String namespace){super(id,name,namespace);this.itemOrBlockId=itemOrBlockId;}
    public int getItemOrBlockId(){return itemOrBlockId;}
}
