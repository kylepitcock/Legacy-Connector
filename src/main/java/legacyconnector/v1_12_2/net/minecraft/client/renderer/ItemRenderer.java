package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;

/**
 * 1.12.2 held-item renderer ({@code Minecraft.getItemRenderer()}): items in an entity's hands are drawn by the modern item renderer at
 * the legacy matrix; the first-person hands are the modern game's (its equip animation is reset through {@link #resetEquippedProgress}).
 */
public class ItemRenderer {
    private static ItemRenderer instance;
    private final Minecraft mc;

    public ItemRenderer(Minecraft mc) {
        this.mc = mc;
    }

    /** The game's ({@code Minecraft.getItemRenderer()}). */
    public static synchronized ItemRenderer mcc$instance() {
        if (instance == null) instance = new ItemRenderer(Minecraft.getMinecraft());
        return instance;
    }

    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform) {
        renderItemSide(entityIn, heldStack, transform, false);
    }

    public void renderItemSide(EntityLivingBase entitylivingbaseIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform, boolean leftHanded) {
        if (heldStack == null || heldStack.isEmpty()) return;
        RenderItem.mcc$instance().renderItem(heldStack, entitylivingbaseIn, transform, leftHanded);
    }

    /** The modern game draws the first-person hands and their items. */
    public void renderItemInFirstPerson(legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer player, float partialTicks, float pitch, EnumHand hand,
            float swingProgress, ItemStack stack, float equippedProgress) {
    }

    public void renderItemInFirstPerson(float partialTicks) {
    }

    /** Replays the item's raise animation (after switching items or using one). */
    public void resetEquippedProgress(EnumHand hand) {
        if (hand != null) net.minecraft.client.Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(hand.mcc$modern());
    }

    /** The modern hand renderer ticks itself. */
    public void updateEquippedItem() {
    }

    /** Phantom: the stack the first-person hand shows in the main hand (the modern hand renderer's). */
    @Deprecated
    public ItemStack itemStackMainHand;

    public static ItemStack mcc$get$itemStackMainHand(ItemRenderer r) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(org.kylepitcock.legacyconnector.v1_12_2.client.Hands1122.shownStack(false));
    }

    public static void mcc$set$itemStackMainHand(ItemRenderer r, ItemStack value) {
    }

    /** Phantom: the stack the first-person hand shows in the off hand (the modern hand renderer's). */
    @Deprecated
    public ItemStack itemStackOffHand;

    public static ItemStack mcc$get$itemStackOffHand(ItemRenderer r) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(org.kylepitcock.legacyconnector.v1_12_2.client.Hands1122.shownStack(true));
    }

    public static void mcc$set$itemStackOffHand(ItemRenderer r, ItemStack value) {
    }

    /** How far a held map is tilted towards the viewer for a pitch (looking down raises it into view). */
    public float getMapAngleFromPitch(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = Math.max(0.0F, Math.min(1.0F, f));
        return -(float) Math.cos(f * (float) Math.PI) * 0.5F + 0.5F;
    }

    /** The local player's bare arm on one side, drawn at the current matrix by the modern hand renderer. */
    public void renderArmFirstPerson(float equipProgress, float swingProgress, legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide side) {
        if (side != null) org.kylepitcock.legacyconnector.v1_12_2.client.Hands1122.renderArm(equipProgress, swingProgress, side.mcc$modern());
    }

    /** Both arms as they hold a map, at the current matrix. */
    public void renderArms() {
        org.kylepitcock.legacyconnector.v1_12_2.client.Hands1122.renderMapArms();
    }
}
