package OnlineRecruitment.OnlineRecruitment.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SalaryRanges {

    private static final Map<String, BigDecimal[]> OPTIONS = new LinkedHashMap<>();

    static {
        put("Under $400", 0, 10);
        put("$400 - $600", 10, 15);
        put("$600 - $800", 15, 20);
        put("$800 - $1,000", 20, 25);
        put("$1,000 - $1,200", 25, 30);
        put("$1,200 - $2,000", 30, 50);
        put("Over $2,000", 50, null);
        put("Negotiable", null, null);
    }

    private SalaryRanges() {
    }

    private static void put(String label, Integer min, Integer max) {
        OPTIONS.put(label, new BigDecimal[]{min == null ? null : BigDecimal.valueOf(min), max == null ? null : BigDecimal.valueOf(max)});
    }

    public static List<String> labels() {
        return new ArrayList<>(OPTIONS.keySet());
    }

    /**
     * Label -> [min, max]
     */
    public static BigDecimal[] toRange(String label) {
        BigDecimal[] r = OPTIONS.get(label);
        return r != null ? r : new BigDecimal[]{null, null};
    }

    /**
     * [min, max] -> label.
     * If no matching range is found, display "min - max".
     */
    public static String toLabel(BigDecimal min, BigDecimal max) {
        for (Map.Entry<String, BigDecimal[]> e : OPTIONS.entrySet()) {
            if (same(e.getValue()[0], min) && same(e.getValue()[1], max)) {
                return e.getKey();
            }
        }

        if (min != null && max != null) {
            return "$" + fmt(min) + " - $" + fmt(max);
        }

        if (min != null) {
            return "From $" + fmt(min);
        }

        if (max != null) {
            return "Up to $" + fmt(max);
        }

        return "Negotiable";
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }

        return a.compareTo(b) == 0;
    }

    private static String fmt(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}