package legacyconnector.v1_12_2.net.minecraft.block.state.pattern;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockWorldState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3i;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A three-dimensional block pattern (golems, withers, mods' multiblocks): aisles of rows of predicates, matched at a position in every
 * orientation (a "forwards" and an "up" direction).
 */
public class BlockPattern {
    private final Predicate<BlockWorldState>[][][] blockMatches;
    private final int fingerLength;
    private final int thumbLength;
    private final int palmLength;

    public BlockPattern(Predicate<BlockWorldState>[][][] predicatesIn) {
        this.blockMatches = predicatesIn;
        this.fingerLength = predicatesIn.length;
        if (fingerLength > 0) {
            this.thumbLength = predicatesIn[0].length;
            this.palmLength = thumbLength > 0 ? predicatesIn[0][0].length : 0;
        } else {
            this.thumbLength = 0;
            this.palmLength = 0;
        }
    }

    public int getFingerLength() {
        return fingerLength;
    }

    public int getThumbLength() {
        return thumbLength;
    }

    public int getPalmLength() {
        return palmLength;
    }

    private PatternHelper checkPatternAt(BlockPos pos, EnumFacing finger, EnumFacing thumb, LoadingCache<BlockPos, BlockWorldState> lcache) {
        for (int i = 0; i < palmLength; ++i) {
            for (int j = 0; j < thumbLength; ++j) {
                for (int k = 0; k < fingerLength; ++k) {
                    if (!blockMatches[k][j][i].apply(lcache.getUnchecked(translateOffset(pos, finger, thumb, i, j, k)))) return null;
                }
            }
        }
        return new PatternHelper(pos, finger, thumb, lcache, palmLength, thumbLength, fingerLength);
    }

    /** The pattern found with one of its blocks at the position, in any orientation, or null. */
    public PatternHelper match(World worldIn, BlockPos pos) {
        LoadingCache<BlockPos, BlockWorldState> cache = createLoadingCache(worldIn, false);
        int i = Math.max(Math.max(palmLength, thumbLength), fingerLength);
        for (BlockPos p : BlockPos.getAllInBox(pos, pos.add(i - 1, i - 1, i - 1))) {
            for (EnumFacing finger : EnumFacing.values()) {
                for (EnumFacing thumb : EnumFacing.values()) {
                    if (thumb != finger && thumb != finger.getOpposite()) {
                        PatternHelper helper = checkPatternAt(p, finger, thumb, cache);
                        if (helper != null) return helper;
                    }
                }
            }
        }
        return null;
    }

    public static LoadingCache<BlockPos, BlockWorldState> createLoadingCache(World worldIn, boolean forceLoadIn) {
        return CacheBuilder.newBuilder().build(new CacheLoader<BlockPos, BlockWorldState>() {
            @Override
            public BlockWorldState load(BlockPos key) {
                return new BlockWorldState(worldIn, key, forceLoadIn);
            }
        });
    }

    /** The position {@code palm/thumb/finger} steps from the corner of a pattern oriented by {@code finger} and {@code thumb}. */
    protected static BlockPos translateOffset(BlockPos pos, EnumFacing finger, EnumFacing thumb, int palmOffset, int thumbOffset, int fingerOffset) {
        if (finger != thumb && finger != thumb.getOpposite()) {
            Vec3i f = new Vec3i(finger.getXOffset(), finger.getYOffset(), finger.getZOffset());
            Vec3i t = new Vec3i(thumb.getXOffset(), thumb.getYOffset(), thumb.getZOffset());
            Vec3i palm = f.crossProduct(t);
            return pos.add(t.getX() * -thumbOffset + palm.getX() * palmOffset + f.getX() * fingerOffset,
                    t.getY() * -thumbOffset + palm.getY() * palmOffset + f.getY() * fingerOffset,
                    t.getZ() * -thumbOffset + palm.getZ() * palmOffset + f.getZ() * fingerOffset);
        }
        throw new IllegalArgumentException("Invalid forwards & up combination");
    }

    /** A match: its corner, orientation and size, and the blocks it covers. */
    public static class PatternHelper {
        private final BlockPos frontTopLeft;
        private final EnumFacing forwards;
        private final EnumFacing up;
        private final LoadingCache<BlockPos, BlockWorldState> lcache;
        private final int width;
        private final int height;
        private final int depth;

        public PatternHelper(BlockPos posIn, EnumFacing fingerIn, EnumFacing thumbIn, LoadingCache<BlockPos, BlockWorldState> lcacheIn, int widthIn,
                int heightIn, int depthIn) {
            this.frontTopLeft = posIn;
            this.forwards = fingerIn;
            this.up = thumbIn;
            this.lcache = lcacheIn;
            this.width = widthIn;
            this.height = heightIn;
            this.depth = depthIn;
        }

        public BlockPos getFrontTopLeft() {
            return frontTopLeft;
        }

        public EnumFacing getForwards() {
            return forwards;
        }

        public EnumFacing getUp() {
            return up;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        /** The block of the match at palm/thumb/finger offsets. */
        public BlockWorldState translateOffset(int palmOffset, int thumbOffset, int fingerOffset) {
            return lcache.getUnchecked(BlockPattern.translateOffset(frontTopLeft, getForwards(), getUp(), palmOffset, thumbOffset, fingerOffset));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("up", up).add("forwards", forwards).add("frontTopLeft", frontTopLeft).toString();
        }
    }
}
