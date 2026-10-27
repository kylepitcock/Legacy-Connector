package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

/** A model's display transforms, one per way an item is shown. */
public class ItemCameraTransforms {
    public static final ItemCameraTransforms DEFAULT = new ItemCameraTransforms();
    public static float offsetTranslateX;
    public static float offsetTranslateY;
    public static float offsetTranslateZ;
    public static float offsetRotationX;
    public static float offsetRotationY;
    public static float offsetRotationZ;
    public static float offsetScaleX;
    public static float offsetScaleY;
    public static float offsetScaleZ;
    public final ItemTransformVec3f thirdperson_left;
    public final ItemTransformVec3f thirdperson_right;
    public final ItemTransformVec3f firstperson_left;
    public final ItemTransformVec3f firstperson_right;
    public final ItemTransformVec3f head;
    public final ItemTransformVec3f gui;
    public final ItemTransformVec3f ground;
    public final ItemTransformVec3f fixed;

    private ItemCameraTransforms() {
        this(ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT,
                ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT, ItemTransformVec3f.DEFAULT);
    }

    public ItemCameraTransforms(ItemCameraTransforms transforms) {
        this(transforms.thirdperson_left, transforms.thirdperson_right, transforms.firstperson_left, transforms.firstperson_right, transforms.head,
                transforms.gui, transforms.ground, transforms.fixed);
    }

    public ItemCameraTransforms(ItemTransformVec3f thirdperson_leftIn, ItemTransformVec3f thirdperson_rightIn, ItemTransformVec3f firstperson_leftIn,
            ItemTransformVec3f firstperson_rightIn, ItemTransformVec3f headIn, ItemTransformVec3f guiIn, ItemTransformVec3f groundIn, ItemTransformVec3f fixedIn) {
        thirdperson_left = thirdperson_leftIn;
        thirdperson_right = thirdperson_rightIn;
        firstperson_left = firstperson_leftIn;
        firstperson_right = firstperson_rightIn;
        head = headIn;
        gui = guiIn;
        ground = groundIn;
        fixed = fixedIn;
    }

    public void applyTransform(TransformType type) {
        applyTransformSide(getTransform(type), false);
    }

    public static void applyTransformSide(ItemTransformVec3f vec, boolean leftHand) {
        if (vec != ItemTransformVec3f.DEFAULT) vec.apply(leftHand);
    }

    public ItemTransformVec3f getTransform(TransformType type) {
        return switch (type) {
            case THIRD_PERSON_LEFT_HAND -> thirdperson_left;
            case THIRD_PERSON_RIGHT_HAND -> thirdperson_right;
            case FIRST_PERSON_LEFT_HAND -> firstperson_left;
            case FIRST_PERSON_RIGHT_HAND -> firstperson_right;
            case HEAD -> head;
            case GUI -> gui;
            case GROUND -> ground;
            case FIXED -> fixed;
            default -> ItemTransformVec3f.DEFAULT;
        };
    }

    public boolean hasCustomTransform(TransformType type) {
        return getTransform(type) != ItemTransformVec3f.DEFAULT;
    }

    /** The legacy view of a modern model's transforms. */
    public static ItemCameraTransforms mcc$of(net.minecraft.client.renderer.block.model.ItemTransforms t) {
        if (t == null || t == net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS) return DEFAULT;
        return new ItemCameraTransforms(ItemTransformVec3f.mcc$of(t.thirdPersonLeftHand), ItemTransformVec3f.mcc$of(t.thirdPersonRightHand),
                ItemTransformVec3f.mcc$of(t.firstPersonLeftHand), ItemTransformVec3f.mcc$of(t.firstPersonRightHand), ItemTransformVec3f.mcc$of(t.head),
                ItemTransformVec3f.mcc$of(t.gui), ItemTransformVec3f.mcc$of(t.ground), ItemTransformVec3f.mcc$of(t.fixed));
    }

    public net.minecraft.client.renderer.block.model.ItemTransforms mcc$modern() {
        if (this == DEFAULT) return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
        return new net.minecraft.client.renderer.block.model.ItemTransforms(thirdperson_left.mcc$modern(), thirdperson_right.mcc$modern(),
                firstperson_left.mcc$modern(), firstperson_right.mcc$modern(), head.mcc$modern(), gui.mcc$modern(), ground.mcc$modern(), fixed.mcc$modern());
    }

    public enum TransformType implements legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart {
        NONE,
        THIRD_PERSON_LEFT_HAND,
        THIRD_PERSON_RIGHT_HAND,
        FIRST_PERSON_LEFT_HAND,
        FIRST_PERSON_RIGHT_HAND,
        HEAD,
        GUI,
        GROUND,
        FIXED;

        public net.minecraft.world.item.ItemDisplayContext mcc$modern() {
            return switch (this) {
                case THIRD_PERSON_LEFT_HAND -> net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                case THIRD_PERSON_RIGHT_HAND -> net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                case FIRST_PERSON_LEFT_HAND -> net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                case FIRST_PERSON_RIGHT_HAND -> net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                case HEAD -> net.minecraft.world.item.ItemDisplayContext.HEAD;
                case GUI -> net.minecraft.world.item.ItemDisplayContext.GUI;
                case GROUND -> net.minecraft.world.item.ItemDisplayContext.GROUND;
                case FIXED -> net.minecraft.world.item.ItemDisplayContext.FIXED;
                default -> net.minecraft.world.item.ItemDisplayContext.NONE;
            };
        }

        public static TransformType mcc$of(net.minecraft.world.item.ItemDisplayContext context) {
            return switch (context) {
                case THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_LEFT_HAND;
                case THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_RIGHT_HAND;
                case FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_LEFT_HAND;
                case FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_RIGHT_HAND;
                case HEAD -> HEAD;
                case GUI -> GUI;
                case GROUND -> GROUND;
                case FIXED -> FIXED;
                default -> NONE;
            };
        }
    }
}
