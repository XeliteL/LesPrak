package realty.matching;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/** Площадь: показатель растёт с запасом площади над минимальной и достигает 1 при запасе 30 %. */
public final class AreaScore extends WeightedScore {
    static final double FULL_MARGIN = 0.3;

    public AreaScore(double weight) {
        super(weight);
    }

    @Override
    public double part(BuyerRequest r, PropertyObject o) {
        if (r.areaMin() <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (o.area() - r.areaMin()) / (r.areaMin() * FULL_MARGIN));
    }
}
