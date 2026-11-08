package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Small 1.20.1 calls that 1.21.1 changed the shape of (registry-aware NBT, {@code NbtIo} taking {@code Path}s and an accounter, hooks that moved between NeoForge
 * classes), as static helpers the loader redirects to ({@code mc-calls.tsv}). Everything keeps the 1.20.1 meaning; where 1.21.1 needs a registry access it
 * comes from {@link MigrateContext}.
 */
public final class ApiCompat {
    private ApiCompat() {}

    // ------------------------------------------------------------------ NBT

    /** {@code ItemStack.serializeNBT()} (Forge's {@code IForgeItemStack}): the 1.20.1 {@code save(new CompoundTag())}. */
    public static CompoundTag serializeNBT(ItemStack stack) {
        return ItemStackNbt.save(stack, new CompoundTag());
    }

    /** {@code BlockEntity.saveWithoutMetadata()}: the registry access of the block entity's level, else the running game's. */
    public static CompoundTag saveWithoutMetadata(BlockEntity be) {
        return be.saveWithoutMetadata(provider(be));
    }

    private static net.minecraft.core.HolderLookup.Provider provider(BlockEntity be) {
        Level level = be.getLevel();
        return level != null ? level.registryAccess() : MigrateContext.provider();
    }

    /** {@code DataResult.getOrThrow(boolean allowPartial, Consumer<String> onError)}. */
    public static Object getOrThrow(DataResult<?> result, boolean allowPartial, Consumer<String> onError) {
        if (allowPartial) {
            var partial = result.resultOrPartial(onError);
            return partial.orElseThrow(() -> new IllegalStateException("No result"));
        }
        return result.getOrThrow(message -> {
            onError.accept(message);
            return new IllegalStateException(message);
        });
    }

    public static CompoundTag readCompressed(InputStream in) throws IOException {
        return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
    }

    public static CompoundTag readCompressed(File file) throws IOException {
        return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
    }

    public static void writeCompressed(CompoundTag tag, File file) throws IOException {
        NbtIo.writeCompressed(tag, file.toPath());
    }

    public static CompoundTag read(File file) throws IOException {
        return NbtIo.read(file.toPath());
    }

    public static void write(CompoundTag tag, File file) throws IOException {
        NbtIo.write(tag, file.toPath());
    }

    // ------------------------------------------------------------------ buffers and fluids

    /** {@code FriendlyByteBuf.writeItemStack(stack, limitedTag)} of Forge: the same wire form as {@code writeItem}. */
    public static FriendlyByteBuf writeItemStack(FriendlyByteBuf buf, ItemStack stack, boolean limitedTag) {
        return MiscCompat.writeItem(buf, stack);
    }

    public static FriendlyByteBuf writeFluidStack(FriendlyByteBuf buf, FluidStack stack) {
        buf.writeNbt((Tag) stack.saveOptional(MigrateContext.provider()));
        return buf;
    }

    public static FluidStack readFluidStack(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return tag == null ? FluidStack.EMPTY : FluidStack.parseOptional(MigrateContext.provider(), tag);
    }

    /** {@code new FluidStack(FluidStack, int)}. */
    public static FluidStack fluidCopy(FluidStack stack, int amount) {
        return stack.copyWithAmount(amount);
    }

    /** {@code new FluidStack(Fluid, int, CompoundTag)}: the tag becomes the custom data component. */
    public static FluidStack fluidWithTag(Fluid fluid, int amount, CompoundTag tag) {
        FluidStack stack = new FluidStack(fluid, amount);
        if (tag != null && !stack.isEmpty()) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        return stack;
    }

    // ------------------------------------------------------------------ blocks and block entities

    /** {@code Block.getLootTable()}: the id (1.21.1 answers a resource key). */
    public static ResourceLocation blockLootTable(Block block) {
        return block.getLootTable().location();
    }

    /** {@code ClientboundBlockEntityDataPacket.create(be, Function<BlockEntity, CompoundTag>)}. */
    @SuppressWarnings("unchecked")
    public static ClientboundBlockEntityDataPacket blockEntityPacket(BlockEntity be, Function<BlockEntity, CompoundTag> writer) {
        return ClientboundBlockEntityDataPacket.create(be, (b, access) -> writer.apply(b));
    }

    /** {@code BlockEntity.invalidateCaps()}: capabilities are queried live now; the cache of NeoForge is told to forget the position. */
    public static void invalidateCaps(BlockEntity be) {
        Level level = be.getLevel();
        if (level != null && !level.isClientSide) be.invalidateCapabilities();
    }

    /** {@code BlockEntity.reviveCaps()}, {@code Entity.invalidateCaps()}, {@code Entity.reviveCaps()}: capabilities have no lifecycle of their own any more. */
    public static void noCapsLifecycle(Object receiver) {}

    // ------------------------------------------------------------------ hooks that moved

    public static int getBurnTime(ItemStack stack, RecipeType<?> type) {
        return stack.getBurnTime(type);
    }

    public static boolean onCropsGrowPre(Level level, BlockPos pos, BlockState state, boolean def) {
        return CommonHooks.canCropGrow(level, pos, state, def);
    }

    public static void onCropsGrowPost(Level level, BlockPos pos, BlockState state) {
        CommonHooks.fireCropGrowPost(level, pos, state);
    }

    /** {@code ForgeHooks.onBlockBreakEvent}: the experience to drop (always 0: it is a separate event now), or -1 when an event handler cancelled the break. */
    public static int onBlockBreakEvent(Level level, GameType mode, ServerPlayer player, BlockPos pos) {
        BlockEvent.BreakEvent event = CommonHooks.fireBlockBreak(level, mode, player, pos, level.getBlockState(pos));
        return event.isCanceled() ? -1 : 0;
    }

    public static boolean isCorrectToolForDrops(BlockState state, Player player) {
        return player.hasCorrectToolForDrops(state);
    }

    public static net.neoforged.neoforge.event.entity.player.CriticalHitEvent getCriticalHit(Player player, Entity target, boolean vanillaCritical, float damageModifier) {
        return CommonHooks.fireCriticalHit(player, target, vanillaCritical, damageModifier);
    }

    public static PlayerInteractEvent.LeftClickBlock onLeftClickBlock(Player player, BlockPos pos, net.minecraft.core.Direction face) {
        return CommonHooks.onLeftClickBlock(player, pos, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
    }

    /** The looting level argument went away from the event ({@code onLivingDrops} reads it from the damage source). */
    public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
        return CommonHooks.onLivingDrops(entity, source, drops, recentlyHit);
    }

    // ------------------------------------------------------------------ particles, trees, teleporting

    /** {@code TorchBlock(props, ParticleOptions)}: 1.21.1 takes the simple particle type (anything else burns with flames). */
    public static net.minecraft.core.particles.SimpleParticleType simpleParticle(net.minecraft.core.particles.ParticleOptions options) {
        return options instanceof net.minecraft.core.particles.SimpleParticleType s ? s : net.minecraft.core.particles.ParticleTypes.FLAME;
    }

    /** {@code ParticleType.getDeserializer()}: nothing reads it any more. */
    public static LegacyParticleDeserializer<?> noDeserializer(net.minecraft.core.particles.ParticleType<?> type) {
        return null;
    }

    /** A {@code ParticleType.codec()} of the old shape (a plain codec) as the registry-aware stream codec the type needs on the wire. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.network.codec.StreamCodec particleStreamCodec(com.mojang.serialization.Codec codec) {
        return net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(codec);
    }

    /** {@code new SaplingBlock(AbstractTreeGrower, props)}: a {@code TreeGrower} for the feature keys the old grower answers. */
    public static net.minecraft.world.level.block.grower.TreeGrower saplingGrower(LegacyTreeGrower grower) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>>> tree = java.util.Optional.empty();
        java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>>> flowers = java.util.Optional.empty();
        try {
            tree = java.util.Optional.ofNullable(grower.getConfiguredFeature(random, false));
            flowers = java.util.Optional.ofNullable(grower.getConfiguredFeature(random, true)).filter(k -> !k.equals(grower.getConfiguredFeature(random, false)));
        } catch (RuntimeException e) {
            // a grower that needs a level to answer: no tree
        }
        return new net.minecraft.world.level.block.grower.TreeGrower("legacy_" + grower.getClass().getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_"),
                java.util.Optional.empty(), tree, flowers);
    }

    /** {@code Entity.changeDimension(level, teleporter)}: the teleporter may reposition; the default moves the entity to the same position in the other level. */
    public static Entity changeDimension(Entity entity, net.minecraft.server.level.ServerLevel level, legacyconnector.v1_20_1.net.minecraftforge.common.util.ITeleporter teleporter) {
        net.minecraft.world.level.portal.DimensionTransition transition = new net.minecraft.world.level.portal.DimensionTransition(level, entity.position(), entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
        if (teleporter == null || !(entity.level() instanceof net.minecraft.server.level.ServerLevel current)) return entity.changeDimension(transition);
        return teleporter.placeEntity(entity, current, level, entity.getYRot(), repositioned -> entity.changeDimension(transition));
    }
}
