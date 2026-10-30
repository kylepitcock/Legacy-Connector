package legacyconnector.v1_12_2.net.minecraftforge.event.village;

import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * The offers a merchant shows a player (handlers may change the list); posted by {@code ForgeEventFactory.listTradeOffers} when a legacy
 * merchant lists its offers.
 */
public class MerchantTradeOffersEvent extends PlayerEvent {
    private final IMerchant merchant;
    private MerchantRecipeList list;

    public MerchantTradeOffersEvent(IMerchant merchant, EntityPlayer player, MerchantRecipeList list) {
        super(player);
        this.merchant = merchant;
        this.list = list;
    }

    public EntityPlayer getPlayer() {
        return getEntityPlayer();
    }

    public IMerchant getMerchant() {
        return merchant;
    }

    public MerchantRecipeList getList() {
        return list;
    }

    public void setList(MerchantRecipeList list) {
        this.list = list;
    }
}
