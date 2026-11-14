package realty.commission;

import java.util.List;

/**
 * Прогрессивная шкала: каждая часть комиссии оплачивается по ставке своей ступени
 * (подпрограмма алгоритма 4). Сложность O(T), T — число ступеней.
 */
public final class ProgressiveScale implements RewardScale {
    private final List<Tier> tiers;

    public ProgressiveScale(List<Tier> tiers) {
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).upper() <= tiers.get(i - 1).upper()) {
                throw new IllegalArgumentException("границы ступеней должны возрастать");
            }
        }
        if (tiers.isEmpty() || tiers.getLast().upper() != Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("последняя ступень должна быть без верхней границы");
        }
        this.tiers = List.copyOf(tiers);
    }

    /** Условная шкала: до 300 тыс. руб. — 40 %, до 600 тыс. руб. — 50 %, свыше — 60 %. */
    public static ProgressiveScale standard() {
        return new ProgressiveScale(List.of(new Tier(300_000, 0.40), new Tier(600_000, 0.50),
                new Tier(Double.POSITIVE_INFINITY, 0.60)));
    }

    @Override
    public long share(long fee, long accumulated) {
        double share = 0;
        double start = accumulated;
        double end = accumulated + fee;
        double lower = 0;
        for (Tier t : tiers) {
            double lo = Math.max(start, lower);
            double hi = Math.min(end, t.upper());
            if (hi > lo) {
                share += (hi - lo) * t.rate();
            }
            lower = t.upper();
            if (t.upper() >= end) {
                break;
            }
        }
        return (long) Math.rint(share);
    }
}
