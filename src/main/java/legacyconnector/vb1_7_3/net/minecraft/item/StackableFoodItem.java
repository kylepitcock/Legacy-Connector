package legacyconnector.vb1_7_3.net.minecraft.item;

/** Beta cookies: the constructor supplies their stack limit. */
public class StackableFoodItem extends FoodItem {
    public StackableFoodItem(int id,int health,boolean meat,int maxCount){super(id,health,meat);this.maxCount=maxCount;}
}
