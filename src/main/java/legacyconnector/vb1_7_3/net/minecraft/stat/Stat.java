package legacyconnector.vb1_7_3.net.minecraft.stat;

import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;
import org.kylepitcock.legacyconnector.vb1_7_3.AdvancementsB173;

/** Beta statistic identity; custom achievement registration is exposed through native advancements. */
public class Stat {
    public final int id;
    public final String stringId;
    public boolean localOnly;
    public String uuid;
    public final String mcc$namespace;
    public Stat(int id,String name){this(id,name,null);}
    /** Bridge-owned statistics can retain their namespace even during a mod callback. */
    protected Stat(int id,String name,String namespace){this.id=id;this.stringId=name;var owner=ContentB173.current();mcc$namespace=namespace!=null?namespace:owner==null?"legacyconnector":owner.modernId;}
    public Stat addStat(){AdvancementsB173.register(this);return this;}
    public Stat localOnly(){localOnly=true;return this;}
    public boolean isAchievement(){return false;}
    public String format(int value){return getNumberFormat().format(value);}
    public static java.text.NumberFormat getNumberFormat(){return java.text.NumberFormat.getIntegerInstance();}
    public static java.text.DecimalFormat getDecimalFormat(){return new java.text.DecimalFormat("########0.00");}
}
