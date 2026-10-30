package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.StitcherException;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * 1.12.2's sprite packer, for mods that build atlases of their own (the block atlas is the modern one): sprites sorted largest first are
 * placed into power-of-two growing slots, rotated when that fits, and get their uv range from the final size.
 */
public class Stitcher {
    private final int mipmapLevelStitcher;
    private final Set<Holder> setStitchHolders = new HashSet<>(256);
    private final List<Slot> stitchSlots = new ArrayList<>(256);
    private int currentWidth;
    private int currentHeight;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxTileDimension;

    public Stitcher(int maxWidthIn, int maxHeightIn, int maxTileDimensionIn, int mipmapLevelStitcherIn) {
        mipmapLevelStitcher = mipmapLevelStitcherIn;
        maxWidth = maxWidthIn;
        maxHeight = maxHeightIn;
        maxTileDimension = maxTileDimensionIn;
    }

    public int getCurrentWidth() {
        return currentWidth;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }

    public void addSprite(TextureAtlasSprite texture) {
        Holder holder = new Holder(texture, mipmapLevelStitcher);
        if (maxTileDimension > 0) holder.setNewDimension(maxTileDimension);
        setStitchHolders.add(holder);
    }

    public void doStitch() {
        Holder[] holders = setStitchHolders.toArray(new Holder[0]);
        Arrays.sort(holders);
        for (Holder holder : holders) {
            if (!allocateSlot(holder)) {
                String s = String.format("Unable to fit: %s - size: %dx%d - Maybe try a lowerresolution resourcepack?", holder.getAtlasSprite().getIconName(),
                        holder.getAtlasSprite().getIconWidth(), holder.getAtlasSprite().getIconHeight());
                throw new StitcherException(holder, s);
            }
        }
        currentWidth = MathHelper.smallestEncompassingPowerOfTwo(currentWidth);
        currentHeight = MathHelper.smallestEncompassingPowerOfTwo(currentHeight);
    }

    public List<TextureAtlasSprite> getStichSlots() {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : stitchSlots) slot.getAllStitchSlots(slots);
        List<TextureAtlasSprite> sprites = new ArrayList<>();
        for (Slot slot : slots) {
            Holder holder = slot.getStitchHolder();
            TextureAtlasSprite sprite = holder.getAtlasSprite();
            sprite.initSprite(currentWidth, currentHeight, slot.getOriginX(), slot.getOriginY(), holder.isRotated());
            sprites.add(sprite);
        }
        return sprites;
    }

    private static int getMipmapDimension(int size, int levels) {
        return (size >> levels) + ((size & (1 << levels) - 1) == 0 ? 0 : 1) << levels;
    }

    private boolean allocateSlot(Holder holder) {
        TextureAtlasSprite sprite = holder.getAtlasSprite();
        boolean nonSquare = sprite.getIconWidth() != sprite.getIconHeight();
        for (Slot slot : stitchSlots) {
            if (slot.addSlot(holder)) return true;
            if (nonSquare) {
                holder.rotate();
                if (slot.addSlot(holder)) return true;
                holder.rotate();
            }
        }
        return expandAndAllocateSlot(holder);
    }

    private boolean expandAndAllocateSlot(Holder holder) {
        int i = Math.min(holder.getWidth(), holder.getHeight());
        int k = MathHelper.smallestEncompassingPowerOfTwo(currentWidth);
        int l = MathHelper.smallestEncompassingPowerOfTwo(currentHeight);
        int i1 = MathHelper.smallestEncompassingPowerOfTwo(currentWidth + i);
        int j1 = MathHelper.smallestEncompassingPowerOfTwo(currentHeight + i);
        boolean widthFits = i1 <= maxWidth;
        boolean heightFits = j1 <= maxHeight;
        if (!widthFits && !heightFits) return false;
        boolean widthGrows = widthFits && k != i1;
        boolean heightGrows = heightFits && l != j1;
        boolean growWidth = widthGrows ^ heightGrows ? widthGrows : widthFits && k <= l;
        Slot slot;
        if (growWidth) {
            if (holder.getWidth() > holder.getHeight()) holder.rotate();
            if (currentHeight == 0) currentHeight = holder.getHeight();
            slot = new Slot(currentWidth, 0, holder.getWidth(), currentHeight);
            currentWidth += holder.getWidth();
        } else {
            slot = new Slot(0, currentHeight, currentWidth, holder.getHeight());
            currentHeight += holder.getHeight();
        }
        slot.addSlot(holder);
        stitchSlots.add(slot);
        return true;
    }

    public static class Holder implements Comparable<Holder> {
        private final TextureAtlasSprite sprite;
        private final int width;
        private final int height;
        private final int mipmapLevelHolder;
        private boolean rotated;
        private float scaleFactor = 1.0F;

        public Holder(TextureAtlasSprite theTextureIn, int mipmapLevelHolderIn) {
            sprite = theTextureIn;
            width = theTextureIn.getIconWidth();
            height = theTextureIn.getIconHeight();
            mipmapLevelHolder = mipmapLevelHolderIn;
            rotated = getMipmapDimension(height, mipmapLevelHolderIn) > getMipmapDimension(width, mipmapLevelHolderIn);
        }

        public TextureAtlasSprite getAtlasSprite() {
            return sprite;
        }

        public int getWidth() {
            int i = rotated ? height : width;
            return getMipmapDimension((int) ((float) i * scaleFactor), mipmapLevelHolder);
        }

        public int getHeight() {
            int i = rotated ? width : height;
            return getMipmapDimension((int) ((float) i * scaleFactor), mipmapLevelHolder);
        }

        public void rotate() {
            rotated = !rotated;
        }

        public boolean isRotated() {
            return rotated;
        }

        public void setNewDimension(int maxDimension) {
            if (width > maxDimension && height > maxDimension) scaleFactor = (float) maxDimension / (float) Math.min(width, height);
        }

        @Override
        public String toString() {
            return "Holder{width=" + width + ", height=" + height + '}';
        }

        @Override
        public int compareTo(Holder other) {
            if (getHeight() == other.getHeight()) {
                if (getWidth() == other.getWidth()) {
                    if (sprite.getIconName() == null) return other.sprite.getIconName() == null ? 0 : -1;
                    return other.sprite.getIconName() == null ? 1 : sprite.getIconName().compareTo(other.sprite.getIconName());
                }
                return getWidth() < other.getWidth() ? 1 : -1;
            }
            return getHeight() < other.getHeight() ? 1 : -1;
        }
    }

    public static class Slot {
        private final int originX;
        private final int originY;
        private final int width;
        private final int height;
        private List<Slot> subSlots;
        private Holder holder;

        public Slot(int originXIn, int originYIn, int widthIn, int heightIn) {
            originX = originXIn;
            originY = originYIn;
            width = widthIn;
            height = heightIn;
        }

        public Holder getStitchHolder() {
            return holder;
        }

        public int getOriginX() {
            return originX;
        }

        public int getOriginY() {
            return originY;
        }

        public boolean addSlot(Holder holderIn) {
            if (holder != null) return false;
            int i = holderIn.getWidth();
            int j = holderIn.getHeight();
            if (i > width || j > height) return false;
            if (i == width && j == height) {
                holder = holderIn;
                return true;
            }
            if (subSlots == null) {
                subSlots = new ArrayList<>(1);
                subSlots.add(new Slot(originX, originY, i, j));
                int k = width - i;
                int l = height - j;
                if (l > 0 && k > 0) {
                    int i1 = Math.max(height, k);
                    int j1 = Math.max(width, l);
                    if (i1 >= j1) {
                        subSlots.add(new Slot(originX, originY + j, i, l));
                        subSlots.add(new Slot(originX + i, originY, k, height));
                    } else {
                        subSlots.add(new Slot(originX + i, originY, k, j));
                        subSlots.add(new Slot(originX, originY + j, width, l));
                    }
                } else if (k == 0) {
                    subSlots.add(new Slot(originX, originY + j, i, l));
                } else if (l == 0) {
                    subSlots.add(new Slot(originX + i, originY, k, j));
                }
            }
            for (Slot slot : subSlots) {
                if (slot.addSlot(holderIn)) return true;
            }
            return false;
        }

        public void getAllStitchSlots(List<Slot> out) {
            if (holder != null) {
                out.add(this);
            } else if (subSlots != null) {
                for (Slot slot : subSlots) slot.getAllStitchSlots(out);
            }
        }

        @Override
        public String toString() {
            return "Slot{originX=" + originX + ", originY=" + originY + ", width=" + width + ", height=" + height + ", texture=" + holder + ", subSlots="
                    + subSlots + '}';
        }
    }
}
