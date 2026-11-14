package realty.commission;

import realty.model.Deal;

/** Комиссия = цена × ставка договора, но не меньше минимальной. */
public final class PercentFeeWithMinimum implements FeePolicy {
    public static final long DEFAULT_MIN_FEE = 100_000;

    private final long minFee;

    public PercentFeeWithMinimum(long minFee) {
        this.minFee = minFee;
    }

    @Override
    public long fee(Deal deal) {
        return Math.max((long) Math.rint(deal.price() * deal.rate()), minFee);
    }
}
