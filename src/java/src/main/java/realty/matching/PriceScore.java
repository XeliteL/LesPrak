package realty.matching;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/** Цена: 1, если объект в пределах бюджета, при превышении бюджета — линейное снижение. */
public final class PriceScore extends WeightedScore {

    public PriceScore(double weight) {
        super(weight);
    }

    @Override
    public double part(BuyerRequest r, PropertyObject o) {
        long budget = r.budgetMax();
        if (o.price() <= budget) {
            return 1.0;
        }
        return 1 - (o.price() - budget) / (budget * StandardFilter.BUDGET_TOLERANCE);
    }
}
