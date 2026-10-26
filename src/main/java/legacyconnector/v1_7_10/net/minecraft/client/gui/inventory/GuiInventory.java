package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.InventoryEffectRenderer;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's survival inventory. Displaying one opens the modern inventory screen (see ClientGui); mods mostly construct it
 * to open the inventory (inventory tabs) or recognise it, and call {@link #drawEntityOnScreen} to draw an entity in a GUI.
 */
public class GuiInventory extends InventoryEffectRenderer {
    private float xSizeFloat;
    private float ySizeFloat;

    public GuiInventory(EntityPlayer player) {
        super(EntityPlayer.mcc$get$inventoryContainer(player));
        this.allowUserInput = true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.xSizeFloat = mouseX;
        this.ySizeFloat = mouseY;
    }

    /**
     * Draws the entity standing at (x, y) of the screen, {@code scale} pixels per block, turned to look along the offsets
     * {@code lookX}/{@code lookY} (1.7.10 callers pass the GUI anchor minus the mouse position).
     */
    public static void drawEntityOnScreen(int x, int y, int scale, float lookX, float lookY, EntityLivingBase entity) {
        var gui = RenderContext.gui();
        if (gui == null || entity == null || !(entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        float yawAngle = (float) Math.atan(lookX / 40.0F);
        float pitchAngle = (float) Math.atan(lookY / 40.0F);
        org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf camera = new org.joml.Quaternionf().rotateX(pitchAngle * 20.0F * ((float) Math.PI / 180.0F));
        pose.mul(camera);
        float bodyRot = living.yBodyRot, yRot = living.getYRot(), xRot = living.getXRot(), headRotO = living.yHeadRotO, headRot = living.yHeadRot;
        living.yBodyRot = 180.0F + yawAngle * 20.0F;
        living.setYRot(180.0F + yawAngle * 40.0F);
        living.setXRot(-pitchAngle * 20.0F);
        living.yHeadRot = living.getYRot();
        living.yHeadRotO = living.getYRot();
        try {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(gui, x, y, scale / Math.max(0.01F, living.getScale()),
                    new org.joml.Vector3f(), pose, camera, living);
        } finally {
            living.yBodyRot = bodyRot;
            living.setYRot(yRot);
            living.setXRot(xRot);
            living.yHeadRotO = headRotO;
            living.yHeadRot = headRot;
        }
    }
}
