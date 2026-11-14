package realty.matching;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/** Район: 1 для района с наивысшим приоритетом, далее по убыванию. */
public final class DistrictScore extends WeightedScore {

    public DistrictScore(double weight) {
        super(weight);
    }

    @Override
    public double part(BuyerRequest r, PropertyObject o) {
        if (r.anyDistrict()) {
            return 1.0;
        }
        return 1 - (double) r.districtRank(o.district()) / r.districts().size();
    }
}
