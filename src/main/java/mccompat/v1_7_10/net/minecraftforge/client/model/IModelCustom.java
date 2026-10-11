package mccompat.v1_7_10.net.minecraftforge.client.model;

/** A model loaded by {@link AdvancedModelLoader}, drawn with the Tessellator in the current GL state. */
public interface IModelCustom {
    String getType();

    void renderAll();

    void renderOnly(String... groupNames);

    void renderPart(String partName);

    void renderAllExcept(String... excludedGroupNames);
}
