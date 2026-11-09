package legacyconnector.vb1_7_3.net.minecraft.item;

/** Beta food heals immediately, even at full health; there was no hunger/use-duration mechanic. */
public class FoodItem extends Item {
    private int healthRestored;
    private boolean meat;
    public FoodItem(int id,int healthRestored,boolean meat){super(id);this.healthRestored=healthRestored;this.meat=meat;maxCount=1;}
    @Override public ItemStack use(ItemStack stack,legacyconnector.vb1_7_3.net.minecraft.world.World world,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){
        stack.count--;player.heal(healthRestored);return stack;
    }
    public int getHealthRestored(){return healthRestored;}
    public boolean isMeat(){return meat;}
}
