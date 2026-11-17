package legacyconnector.v1_7_10.net.minecraftforge.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The damage distribution of Forge 3.3.7's {@code ArmorProperties.StandardizeList} (checked against its source) in the shared armour record. */
class ArmorDistribution125Test {
    private static ISpecialArmor.ArmorProperties p(int priority, double ratio, int max) {
        return new ISpecialArmor.ArmorProperties(priority, ratio, max);
    }

    @Test
    void ratiosOverOneAreScaledDown() {
        var a = new ISpecialArmor.ArmorProperties[] {p(0, 0.6, Integer.MAX_VALUE), p(0, 0.6, Integer.MAX_VALUE)};
        ISpecialArmor.ArmorProperties.standardizeList(a, 100);
        assertEquals(0.5, a[0].AbsorbRatio, 1e-9);
        assertEquals(0.5, a[1].AbsorbRatio, 1e-9);
    }

    @Test
    void aPieceCappedByItsMaximumAbsorbsOnlyThat() {
        var a = new ISpecialArmor.ArmorProperties[] {p(0, 0.6, 10), p(0, 0.6, Integer.MAX_VALUE)};
        ISpecialArmor.ArmorProperties.standardizeList(a, 100);
        assertEquals(10, a[0].AbsorbMax);
        assertEquals(0.1, a[0].AbsorbRatio, 1e-9);
        assertEquals(0.6, a[1].AbsorbRatio, 1e-9);
    }

    @Test
    void higherPriorityIsFirstAndSortKeyIsMaxTimes100OverRatio() {
        var a = new ISpecialArmor.ArmorProperties[] {p(0, 0.2, 100), p(1, 0.2, 100)};
        java.util.Arrays.sort(a);
        assertEquals(1, a[0].Priority);
        // (int) (left - right): 100*100/0.5 - 100*100/0.25 = 20000 - 40000
        assertEquals(-20000, p(0, 0.5, 100).compareTo(p(0, 0.25, 100)));
    }
}
