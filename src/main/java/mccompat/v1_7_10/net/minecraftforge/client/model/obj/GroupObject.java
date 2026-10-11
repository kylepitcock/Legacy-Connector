package mccompat.v1_7_10.net.minecraftforge.client.model.obj;

import java.util.ArrayList;
import mccompat.v1_7_10.net.minecraft.client.renderer.Tessellator;

public class GroupObject {
    public String name;
    public ArrayList<Face> faces = new ArrayList<>();
    public int glDrawingMode;

    public GroupObject() {
        this("");
    }

    public GroupObject(String name) {
        this(name, -1);
    }

    public GroupObject(String name, int glDrawingMode) {
        this.name = name;
        this.glDrawingMode = glDrawingMode;
    }

    public void render() {
        if (faces.isEmpty()) return;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(glDrawingMode);
        render(tessellator);
        tessellator.draw();
    }

    public void render(Tessellator tessellator) {
        for (Face face : faces) face.addFaceForRender(tessellator);
    }
}
