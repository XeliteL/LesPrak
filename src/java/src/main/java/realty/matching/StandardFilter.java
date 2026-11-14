package realty.matching;

import realty.model.BuyerRequest;
import realty.model.FloorCategory;
import realty.model.PropertyObject;

/** Стандартные жёсткие условия заявки (шаг 1 алгоритма 2). */
public enum StandardFilter implements RequestFilter {
    ON_SALE {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            return o.isOnSale();
        }
    },
    BUDGET {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            return o.price() <= r.budgetMax() * (1 + BUDGET_TOLERANCE) && o.price() >= r.budgetMin();
        }
    },
    ROOMS {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            return r.rooms().contains(o.rooms());
        }
    },
    DISTRICT {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            return r.anyDistrict() || r.districtRank(o.district()) >= 0;
        }
    },
    AREA {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            return o.area() >= r.areaMin();
        }
    },
    FLOOR {
        @Override
        public boolean accepts(BuyerRequest r, PropertyObject o) {
            FloorCategory category = o.floorCategory();
            return !(r.notFirstFloor() && category == FloorCategory.FIRST)
                    && !(r.notLastFloor() && category == FloorCategory.LAST);
        }
    };

    /** Объект может быть дороже бюджета на эту долю (резерв на торг). */
    public static final double BUDGET_TOLERANCE = 0.05;
}
