package com.infobee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SedMosCalculatorTest {

    @Test
    void calculatesSedAndMosPerSccsFormula() {
        // A=1.74 g/day, C=1%, F=1, ABS=50%, NOAEL=100, BW=60
        // SED = 1.74 * 0.01 * 1 * 0.5 * 1000 / 60 = 0.145 mg/kg bw/day
        // MoS = 100 / 0.145 = 689.66 -> SAFE
        SedMosCalculator.Result result = SedMosCalculator.calculate(1.74, 1.0, 1.0, 50.0, 100.0, 60.0);

        assertEquals(0.145, result.sedMgKgDay(), 0.0001);
        assertEquals(689.6552, result.mosValue(), 0.01);
        assertTrue(result.safe());
        assertTrue(result.conclusion().startsWith("SAFE"));
        assertTrue(result.formulaBreakdown().contains("MoS"));
    }

    @Test
    void flagsUnsafeWhenMosBelowThreshold() {
        // SED = 5 g * 10% * 1.0 * 100% * 1000 / 60 = 8.3333; MoS = 200 / 8.3333 = 24.0 -> NOT SAFE
        SedMosCalculator.Result result = SedMosCalculator.calculate(5.0, 10.0, 1.0, 100.0, 200.0, null);

        assertEquals(8.3333, result.sedMgKgDay(), 0.001);
        assertEquals(24.0, result.mosValue(), 0.01);
        assertFalse(result.safe());
        assertTrue(result.conclusion().startsWith("NOT SAFE"));
    }

    @Test
    void appliesDefaultsForOptionalInputs() {
        // retention null -> 1.0; absorption null -> 50%; BW null -> 60 kg
        SedMosCalculator.Result explicit = SedMosCalculator.calculate(2.0, 5.0, 1.0, 50.0, 300.0, 60.0);
        SedMosCalculator.Result withNulls = SedMosCalculator.calculate(2.0, 5.0, null, null, 300.0, null);

        assertEquals(explicit.sedMgKgDay(), withNulls.sedMgKgDay());
        assertEquals(explicit.mosValue(), withNulls.mosValue());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
            () -> SedMosCalculator.calculate(null, 1.0, 1.0, 50.0, 100.0, 60.0));
        assertThrows(IllegalArgumentException.class,
            () -> SedMosCalculator.calculate(-1.0, 1.0, 1.0, 50.0, 100.0, 60.0));
        assertThrows(IllegalArgumentException.class,
            () -> SedMosCalculator.calculate(1.0, 101.0, 1.0, 50.0, 100.0, 60.0));
        assertThrows(IllegalArgumentException.class,
            () -> SedMosCalculator.calculate(1.0, 1.0, 1.5, 50.0, 100.0, 60.0));
        assertThrows(IllegalArgumentException.class,
            () -> SedMosCalculator.calculate(1.0, 1.0, 1.0, 50.0, null, 60.0));
    }
}
