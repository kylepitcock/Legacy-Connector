package org.kylepitcock.legacyconnector.client;

import java.util.List;

/**
 * Legacy mods placed the buttons they add to the game's screens for the old layout (1.7.10's options screen put its buttons elsewhere), so
 * on the modern screens they can land on top of a vanilla button. A rectangle is {@code {x, y, width, height}}.
 */
public final class ButtonPlacement {
    private ButtonPlacement() {}

    public static boolean overlaps(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    public static boolean overlapsAny(int[] r, List<int[]> obstacles) {
        for (int[] o : obstacles) if (overlaps(r, o)) return true;
        return false;
    }

    /**
     * The free position nearest to where the mod put the button (same x, or centred), on screen, that touches none of the obstacles with
     * a 2 pixel margin; the original position when nothing is free.
     */
    public static int[] freeSpot(int[] wanted, List<int[]> obstacles, int screenWidth, int screenHeight) {
        if (!overlapsAny(wanted, obstacles)) return wanted;
        int[] best = null;
        long bestDistance = Long.MAX_VALUE;
        int[] centred = {Math.max(0, (screenWidth - wanted[2]) / 2), 0};
        for (int x : new int[] {wanted[0], centred[0]}) {
            if (x < 0 || x + wanted[2] > screenWidth) continue;
            for (int y = 0; y + wanted[3] <= screenHeight; y += 2) {
                int[] padded = {x - 2, y - 2, wanted[2] + 4, wanted[3] + 4};
                if (overlapsAny(padded, obstacles)) continue;
                long dx = x - wanted[0];
                long dy = y - wanted[1];
                long d = dx * dx + dy * dy;
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new int[] {x, y, wanted[2], wanted[3]};
                }
            }
        }
        return best != null ? best : wanted;
    }
}
