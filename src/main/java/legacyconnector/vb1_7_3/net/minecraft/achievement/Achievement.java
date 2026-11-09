package legacyconnector.vb1_7_3.net.minecraft.achievement;

import legacyconnector.vb1_7_3.net.minecraft.stat.Stat;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/** Beta achievement constructors and fluent registration, retaining original parent and icon objects. */
public class Achievement extends Stat {
    public final int column,row;
    public final Achievement parent;
    public final ItemStack icon;
    public final String translationKey;
    public String mcc$title,mcc$description;
    public boolean mcc$vanilla;
    private boolean challenge;
    public Achievement(int id,String name,int column,int row,Item item,Achievement parent){this(id,name,column,row,new ItemStack(item),parent);}
    public Achievement(int id,String name,int column,int row,Block block,Achievement parent){this(id,name,column,row,new ItemStack(block),parent);}
    public Achievement(int id,String name,int column,int row,ItemStack icon,Achievement parent){
        this(id,name,column,row,icon,parent,null);
    }
    Achievement(int id,String name,int column,int row,ItemStack icon,Achievement parent,String namespace){
        super(5242880+id,"achievement."+name,namespace);this.column=column;this.row=row;this.icon=icon;this.parent=parent;
        translationKey="achievement."+name+".desc";
        Achievements.minColumn=Math.min(Achievements.minColumn,column);Achievements.maxColumn=Math.max(Achievements.maxColumn,column);
        Achievements.minRow=Math.min(Achievements.minRow,row);Achievements.maxRow=Math.max(Achievements.maxRow,row);
    }
    @Override public Achievement localOnly(){super.localOnly();return this;}
    public Achievement challenge(){challenge=true;return this;}
    public boolean isChallenge(){return challenge;}
    @Override public boolean isAchievement(){return true;}
    public String getTranslatedDescription(){return mcc$description==null?ContentB173.translate(translationKey):mcc$description;}
    @Override public Achievement addStat(){super.addStat();if(!Achievements.ACHIEVEMENTS.contains(this))Achievements.ACHIEVEMENTS.add(this);return this;}
}
