package legacyconnector.vb1_7_3.net.minecraft.client.render.item;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Hand attachments retain original transforms and use the native item model. */
public class HeldItemRenderer {
    public HeldItemRenderer(){}
    public void renderItem(LivingEntity entity,ItemStack stack){if(stack!=null)RendererContextB173.item(entity,stack);}
}
