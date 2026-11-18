package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * {@code RenderBuffers.fixedBuffers} of 1.20.1, a {@code SortedMap<RenderType, BufferBuilder>} mods put their own render types into
 * ({@code renderBuffers().fixedBuffers.put(type, new BufferBuilder(type.bufferSize()))}, Alex's Mobs, Quark, Create...). In 1.21.1 the map is a private
 * local of the constructor that ends up in the buffer source; this view puts the builder's memory there, which is what the old map did.
 */
public final class FixedBuffersView extends AbstractMap<Object, Object> implements SortedMap<Object, Object> {
    private static Field fixedBuffers;
    private final Map<RenderType, ByteBufferBuilder> target;

    private FixedBuffersView(Map<RenderType, ByteBufferBuilder> target) {
        this.target = target;
    }

    /** The read of {@code RenderBuffers.fixedBuffers}. */
    @SuppressWarnings("unchecked")
    public static SortedMap<Object, Object> of(RenderBuffers buffers) {
        try {
            MultiBufferSource.BufferSource source = buffers.bufferSource();
            if (fixedBuffers == null) {
                Field f = MultiBufferSource.BufferSource.class.getDeclaredField("fixedBuffers");
                f.setAccessible(true);
                fixedBuffers = f;
            }
            return new FixedBuffersView((Map<RenderType, ByteBufferBuilder>) fixedBuffers.get(source));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new FixedBuffersView(new java.util.HashMap<>());
        }
    }

    @Override
    public Object put(Object type, Object builder) {
        ByteBufferBuilder memory = builder instanceof CompatBufferBuilder c ? c.memory() : null;
        if (!(type instanceof RenderType rt) || memory == null) return null;
        synchronized (target) {
            target.put(rt, memory);
        }
        return null;
    }

    @Override
    public Object get(Object type) {
        return null;
    }

    @Override
    public boolean containsKey(Object type) {
        return target.containsKey(type);
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return Collections.emptySet();
    }

    @Override
    public Comparator<? super Object> comparator() {
        return null;
    }

    @Override
    public SortedMap<Object, Object> subMap(Object from, Object to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedMap<Object, Object> headMap(Object to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedMap<Object, Object> tailMap(Object from) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object firstKey() {
        throw new java.util.NoSuchElementException();
    }

    @Override
    public Object lastKey() {
        throw new java.util.NoSuchElementException();
    }
}
