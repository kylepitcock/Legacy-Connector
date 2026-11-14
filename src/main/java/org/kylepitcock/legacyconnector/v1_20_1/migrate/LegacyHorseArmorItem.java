package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stands in for 1.20.1's {@code HorseArmorItem(int protection, String | ResourceLocation texture, Item.Properties)}. 1.21.1's {@code AnimalArmorItem} takes an
 * armour material instead, and reads the texture from the material's registry key; this class builds a material holder that carries the protection and the
 * key the texture comes from, and keeps answering {@code getProtection()} and the texture the mod named.
 */
public class LegacyHorseArmorItem extends AnimalArmorItem {
    private static final String TEXTURE_PREFIX = "textures/entity/horse/armor/horse_armor_";

    private final int protection;
    private final ResourceLocation texture;

    public LegacyHorseArmorItem(int protection, String name, Item.Properties properties) {
        this(protection, ResourceLocation.withDefaultNamespace(TEXTURE_PREFIX + name + ".png"), properties, false);
    }

    public LegacyHorseArmorItem(int protection, ResourceLocation texture, Item.Properties properties) {
        this(protection, texture, properties, false);
    }

    LegacyHorseArmorItem(int protection, ResourceLocation texture, Item.Properties properties, boolean overlay) {
        super(material(protection, texture), AnimalArmorItem.BodyType.EQUESTRIAN, overlay, properties);
        this.protection = protection;
        this.texture = texture;
    }

    /** The material of a legacy horse armour: BODY defence = the protection, the key is the texture name AnimalArmorItem builds the texture path from. */
    private static Holder<ArmorMaterial> material(int protection, ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith(TEXTURE_PREFIX)) path = path.substring(TEXTURE_PREFIX.length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        ResourceLocation id = texture.withPath(path);
        ArmorMaterial material = new ArmorMaterial(Map.of(ArmorItem.Type.BODY, protection), 0, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.HORSE_ARMOR), () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(id)), 0.0F, 0.0F);
        ResourceKey<ArmorMaterial> key = ResourceKey.create(Registries.ARMOR_MATERIAL, id);
        return new KeyedHolder(key, material);
    }

    /** A holder that is not in a registry but has a key: {@code AnimalArmorItem} reads the texture name from {@code unwrapKey()}. */
    private record KeyedHolder(ResourceKey<ArmorMaterial> key, ArmorMaterial value) implements Holder<ArmorMaterial> {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean is(ResourceLocation id) {
            return key.location().equals(id);
        }

        @Override
        public boolean is(ResourceKey<ArmorMaterial> other) {
            return key.equals(other);
        }

        @Override
        public boolean is(java.util.function.Predicate<ResourceKey<ArmorMaterial>> predicate) {
            return predicate.test(key);
        }

        @Override
        public boolean is(net.minecraft.tags.TagKey<ArmorMaterial> tag) {
            return false;
        }

        @Override
        public boolean is(Holder<ArmorMaterial> other) {
            return other.value() == value;
        }

        @Override
        public java.util.stream.Stream<net.minecraft.tags.TagKey<ArmorMaterial>> tags() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public com.mojang.datafixers.util.Either<ResourceKey<ArmorMaterial>, ArmorMaterial> unwrap() {
            return com.mojang.datafixers.util.Either.left(key);
        }

        @Override
        public java.util.Optional<ResourceKey<ArmorMaterial>> unwrapKey() {
            return java.util.Optional.of(key);
        }

        @Override
        public Kind kind() {
            return Kind.REFERENCE;
        }

        @Override
        public boolean canSerializeIn(HolderOwner<ArmorMaterial> owner) {
            return true;
        }
    }

    public int getProtection() {
        return protection;
    }

    @Override
    public ResourceLocation getTexture() {
        return texture;
    }
}
