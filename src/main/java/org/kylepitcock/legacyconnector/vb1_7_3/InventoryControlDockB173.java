package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;

/** Inventory-relative control placement, independent of rendering and input state. */
public final class InventoryControlDockB173 {
    public enum Edge { LEFT, RIGHT, TOP, BOTTOM, FREE }
    public record Rect(int x, int y, int width, int height) {}
    public record Anchor(Edge edge, double along, int freeX, int freeY) {}
    public record Placement(Rect bounds, Edge edge) {}
    private static final int OVERLAP = 4, SNAP = 8;
    private InventoryControlDockB173() {}

    /** Resolves saved intent without changing it when temporary obstacles require another position. */
    public static Placement resolve(Anchor anchor, Rect inventory, int size, int viewportW, int viewportH, List<Rect> obstacles) {
        if (!valid(anchor) || !valid(inventory) || size <= 0 || size > viewportW || size > viewportH) return null;
        List<Rect> blocked = blockers(obstacles);
        Rect desired = anchor.edge() == Edge.FREE
                ? new Rect(saturated((long) inventory.x() + anchor.freeX()), saturated((long) inventory.y() + anchor.freeY()), size, size)
                : attached(anchor.edge(), anchor.along(), inventory, size);
        if (desired == null) desired = new Rect(inventory.x(), inventory.y(), size, size);
        if (anchor.edge() == Edge.FREE && safe(desired, inventory, viewportW, viewportH, blocked)) return new Placement(desired, Edge.FREE);
        if (anchor.edge() != Edge.FREE) {
            Rect sameEdge = slide(anchor.edge(), desired, inventory, size, viewportW, viewportH, blocked);
            if (sameEdge != null) return new Placement(sameEdge, anchor.edge());
        }
        Placement best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Edge edge : Edge.values()) {
            if (edge == Edge.FREE || edge == anchor.edge()) continue;
            Rect candidate = slide(edge, desired, inventory, size, viewportW, viewportH, blocked);
            if (candidate != null && distance(candidate, desired) < distance) {
                best = new Placement(candidate, edge);distance = distance(candidate, desired);
            }
        }
        if (best != null) return best;
        Rect free = free(desired, inventory, size, viewportW, viewportH, blocked);
        return free == null ? null : new Placement(free, Edge.FREE);
    }

    /** Commits the safe drop location; returns null only when no placement fits. */
    public static Anchor drop(Rect proposed, Rect inventory, int size, int viewportW, int viewportH, List<Rect> obstacles) {
        if (!valid(proposed) || !valid(inventory) || size <= 0 || size > viewportW || size > viewportH) return null;
        Rect target = new Rect(proposed.x(), proposed.y(), size, size);
        Edge nearest = null;double nearestDistance = Double.POSITIVE_INFINITY;
        for (Edge edge : Edge.values()) {
            if (edge == Edge.FREE) continue;
            Rect projected = project(edge, target, inventory, size);
            if (projected != null && distance(projected, target) <= SNAP * SNAP && distance(projected, target) < nearestDistance) {
                nearest = edge;nearestDistance = distance(projected, target);
            }
        }
        Anchor requested = nearest == null
                ? new Anchor(Edge.FREE, 0, saturated((long) target.x() - inventory.x()), saturated((long) target.y() - inventory.y()))
                : new Anchor(nearest, fraction(nearest, target, inventory, size), 0, 0);
        Placement placement = resolve(requested, inventory, size, viewportW, viewportH, obstacles);
        if (placement == null) return null;
        Rect bounds = placement.bounds();
        return placement.edge() == Edge.FREE
                ? new Anchor(Edge.FREE, 0, saturated((long) bounds.x() - inventory.x()), saturated((long) bounds.y() - inventory.y()))
                : new Anchor(placement.edge(), fraction(placement.edge(), bounds, inventory, size), 0, 0);
    }

    public static boolean valid(Anchor anchor) {
        return anchor != null && anchor.edge() != null && Double.isFinite(anchor.along()) && anchor.along() >= 0 && anchor.along() <= 1;
    }
    private static boolean valid(Rect rect) { return rect != null && rect.width() > 0 && rect.height() > 0; }
    private static List<Rect> blockers(List<Rect> obstacles) {
        if (obstacles == null) return List.of();
        return obstacles.stream().filter(InventoryControlDockB173::valid).toList();
    }
    private static Rect attached(Edge edge, double along, Rect inventory, int size) {
        long start = vertical(edge) ? (long) inventory.y() : (long) inventory.x();
        long end = vertical(edge) ? bottom(inventory) - size : right(inventory) - size;
        if (end < start) return null;
        int axis = saturated(start + Math.round((end - start) * along));
        return edgeRect(edge, axis, inventory, size);
    }
    private static Rect project(Edge edge, Rect target, Rect inventory, int size) {
        long start = vertical(edge) ? (long) inventory.y() : (long) inventory.x();
        long end = vertical(edge) ? bottom(inventory) - size : right(inventory) - size;
        if (end < start) return null;
        long axis = vertical(edge) ? target.y() : target.x();
        return edgeRect(edge, saturated(clamp(axis, start, end)), inventory, size);
    }
    private static Rect edgeRect(Edge edge, int axis, Rect inventory, int size) {
        return switch (edge) {
            case LEFT -> new Rect(saturated((long) inventory.x() - size + OVERLAP), axis, size, size);
            case RIGHT -> new Rect(saturated(right(inventory) - OVERLAP), axis, size, size);
            case TOP -> new Rect(axis, saturated((long) inventory.y() - size + OVERLAP), size, size);
            case BOTTOM -> new Rect(axis, saturated(bottom(inventory) - OVERLAP), size, size);
            case FREE -> null;
        };
    }
    private static Rect slide(Edge edge, Rect desired, Rect inventory, int size, int viewportW, int viewportH, List<Rect> blocked) {
        long start = Math.max(0, vertical(edge) ? (long) inventory.y() : (long) inventory.x());
        long end = Math.min(vertical(edge) ? (long) viewportH - size : (long) viewportW - size,
                vertical(edge) ? bottom(inventory) - size : right(inventory) - size);
        if (start > end) return null;
        long axis = vertical(edge) ? desired.y() : desired.x();
        var candidates = new TreeSet<Integer>();
        candidates.add(saturated(clamp(axis, start, end)));candidates.add(saturated(start));candidates.add(saturated(end));
        for (Rect obstacle : blocked) {
            long before = vertical(edge) ? (long) obstacle.y() - size : (long) obstacle.x() - size;
            long after = vertical(edge) ? bottom(obstacle) : right(obstacle);
            candidates.add(saturated(clamp(before, start, end)));candidates.add(saturated(clamp(after, start, end)));
        }
        Rect best = null;double distance = Double.POSITIVE_INFINITY;
        for (int candidate : candidates) {
            Rect bounds = edgeRect(edge, candidate, inventory, size);
            if (safe(bounds, inventory, viewportW, viewportH, blocked) && distance(bounds, desired) < distance) {
                best = bounds;distance = distance(bounds, desired);
            }
        }
        return best;
    }
    private static Rect free(Rect desired, Rect inventory, int size, int viewportW, int viewportH, List<Rect> blocked) {
        var xs = new TreeSet<Integer>();var ys = new TreeSet<Integer>();
        add(xs, desired.x(), viewportW - size);add(ys, desired.y(), viewportH - size);
        add(xs, 0, viewportW - size);add(xs, viewportW - size, viewportW - size);
        add(ys, 0, viewportH - size);add(ys, viewportH - size, viewportH - size);
        add(xs, (long) inventory.x() - size + OVERLAP, viewportW - size);add(xs, right(inventory) - OVERLAP, viewportW - size);
        add(ys, (long) inventory.y() - size + OVERLAP, viewportH - size);add(ys, bottom(inventory) - OVERLAP, viewportH - size);
        for (Rect obstacle : concat(inventory, blocked)) {
            add(xs, (long) obstacle.x() - size, viewportW - size);add(xs, right(obstacle), viewportW - size);
            add(ys, (long) obstacle.y() - size, viewportH - size);add(ys, bottom(obstacle), viewportH - size);
        }
        Rect best = null;double distance = Double.POSITIVE_INFINITY;
        for (int x : xs) for (int y : ys) {
            Rect bounds = new Rect(x, y, size, size);
            if (safe(bounds, inventory, viewportW, viewportH, blocked) && distance(bounds, desired) < distance) {
                best = bounds;distance = distance(bounds, desired);
            }
        }
        return best;
    }
    private static List<Rect> concat(Rect inventory, List<Rect> blocked) {
        var result = new ArrayList<Rect>(blocked.size() + 1);result.add(inventory);result.addAll(blocked);return result;
    }
    private static void add(Set<Integer> values, long value, int max) { values.add(saturated(clamp(value, 0, max))); }
    private static boolean safe(Rect bounds, Rect inventory, int width, int height, List<Rect> blocked) {
        if (bounds == null || bounds.x() < 0 || bounds.y() < 0 || right(bounds) > width || bottom(bounds) > height) return false;
        for (Rect obstacle : blocked) if (overlaps(bounds, obstacle)) return false;
        if (!overlaps(bounds, inventory)) return true;
        long overlapX = Math.min(right(bounds), right(inventory)) - Math.max(bounds.x(), inventory.x());
        long overlapY = Math.min(bottom(bounds), bottom(inventory)) - Math.max(bounds.y(), inventory.y());
        return (bounds.x() < inventory.x() || right(bounds) > right(inventory)) && overlapX <= OVERLAP
                || (bounds.y() < inventory.y() || bottom(bounds) > bottom(inventory)) && overlapY <= OVERLAP;
    }
    private static boolean overlaps(Rect a, Rect b) { return a.x() < right(b) && right(a) > b.x() && a.y() < bottom(b) && bottom(a) > b.y(); }
    private static boolean vertical(Edge edge) { return edge == Edge.LEFT || edge == Edge.RIGHT; }
    private static double fraction(Edge edge, Rect bounds, Rect inventory, int size) {
        long start = vertical(edge) ? (long) inventory.y() : (long) inventory.x();
        long span = (vertical(edge) ? inventory.height() : inventory.width()) - size;
        return span <= 0 ? 0 : Math.max(0, Math.min(1, ((vertical(edge) ? bounds.y() : bounds.x()) - start) / (double) span));
    }
    private static double distance(Rect a, Rect b) { double x = (double) a.x() - b.x(), y = (double) a.y() - b.y();return x * x + y * y; }
    private static long right(Rect rect) { return (long) rect.x() + rect.width(); }
    private static long bottom(Rect rect) { return (long) rect.y() + rect.height(); }
    private static long clamp(long value, long min, long max) { return Math.max(min, Math.min(max, value)); }
    private static int saturated(long value) { return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)); }
}
