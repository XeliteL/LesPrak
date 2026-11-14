package realty.commission;

import realty.model.Deal;

/** Правило расчёта комиссии агентства по сделке. */
@FunctionalInterface
public interface FeePolicy {

    long fee(Deal deal);
}
