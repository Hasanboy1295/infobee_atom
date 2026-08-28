package com.infobee.service;

import java.lang.Math;

/**
 * SCCS "Notes of Guidance" exposure calculations for cosmetic safety reports.
 *
 * SED  = A(g/day) x C(%)/100 x F(retention) x ABS(%)/100 x 1000 / BW(kg)
 *        [mg/kg bw/day]
 * MoS  = NOAEL(mg/kg bw/day) / SED
 *
 * A substance is considered safe for the evaluated use when MoS >= 100.
 */
public final class SedMosCalculator {

    public static final double DEFAULT_BODY_WEIGHT_KG = 60.0;
    public static final double DEFAULT_DERMAL_ABSORPTION_PERCENT = 50.0;
    public static final double SAFE_MOS_THRESHOLD = 100.0;

    private SedMosCalculator() {}

    public record Result(double sedMgKgDay, double mosValue, boolean safe,
                         String conclusion, String formulaBreakdown) {}

    /**
     * @param dailyAmountGrams          A - amount of finished product applied per day (g/day)
     * @param concentrationPercent      C - concentration of the substance in the product (%)
     * @param retentionFactor           F - retention factor (0..1]; null treated as 1.0 (leave-on)
     * @param dermalAbsorptionPercent   ABS_der - dermal absorption (%); null -> conservative default 50%
     * @param noaelMgKgDay              NOAEL from repeated-dose studies (mg/kg bw/day)
     * @param bodyWeightKg              BW - defaults to 60 kg when null
     */
    public static Result calculate(Double dailyAmountGrams,
                                   Double concentrationPercent,
                                   Double retentionFactor,
                                   Double dermalAbsorptionPercent,
                                   Double noaelMgKgDay,
                                   Double bodyWeightKg) {
        requirePositive(dailyAmountGrams, "Daily amount");
        requireInRange(concentrationPercent, 0.0, 100.0, "Concentration");
        double f = retentionFactor == null ? 1.0 : retentionFactor;
        requireInRange(f, 0.0, 1.0, "Retention factor");
        double absorption = dermalAbsorptionPercent == null
            ? DEFAULT_DERMAL_ABSORPTION_PERCENT
            : dermalAbsorptionPercent;
        requireInRange(absorption, 0.0, 100.0, "Dermal absorption");
        requirePositive(noaelMgKgDay, "NOAEL");
        double bw = bodyWeightKg == null ? DEFAULT_BODY_WEIGHT_KG : bodyWeightKg;
        requirePositive(bw, "Body weight");

        double sed = (dailyAmountGrams * (concentrationPercent / 100.0) * f
            * (absorption / 100.0) * 1000.0) / bw;
        double mos = sed > 0 ? noaelMgKgDay / sed : Double.POSITIVE_INFINITY;
        boolean safe = mos >= SAFE_MOS_THRESHOLD;

        String conclusion = safe
            ? String.format("SAFE: MoS %.1f >= %.0f. Systemic exposure is acceptable for the evaluated use.",
                mos, SAFE_MOS_THRESHOLD)
            : String.format("NOT SAFE: MoS %.1f < %.0f. Reduce concentration/refine exposure or derive a "
                + "substance-specific NOAEL before approval.", mos, SAFE_MOS_THRESHOLD);

        String breakdown = String.format(
            "SED = %.3f g/day x %.2f%%/100 x F=%.2f x ABS=%.0f%%/100 x 1000 / %.0f kg = %.4f mg/kg bw/day; "
                + "MoS = NOAEL %.2f / SED %.4f = %.1f",
            dailyAmountGrams, concentrationPercent, f, absorption, bw, sed, noaelMgKgDay, sed, mos);

        return new Result(round(sed), round(mos), safe, conclusion, breakdown);
    }

    private static void requirePositive(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a positive number");
        }
    }

    private static void requireInRange(Double value, double min, double max, String name) {
        if (value == null || !Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(String.format("%s must be between %s and %s", name, min, max));
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
