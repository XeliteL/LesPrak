package realty.commission;

/** Шкала вознаграждения агента. */
@FunctionalInterface
public interface RewardScale {

    /** Доля агента от комиссии fee, если до этой сделки за месяц накоплено accumulated руб. */
    long share(long fee, long accumulated);
}
