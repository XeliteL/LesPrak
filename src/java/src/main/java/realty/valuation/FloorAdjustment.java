package realty.valuation;

import java.util.EnumMap;
import java.util.Map;

import realty.model.Apartment;
import realty.model.FloorCategory;
import realty.model.MarketListing;

/** Поправка на категорию этажа: u × k(оцениваемая) / k(аналог). */
public final class FloorAdjustment implements PriceAdjustment {
    private final Map<FloorCategory, Double> coefficients;

    public FloorAdjustment(Map<FloorCategory, Double> coefficients) {
        if (!coefficients.keySet().containsAll(java.util.EnumSet.allOf(FloorCategory.class))) {
            throw new IllegalArgumentException("нужны коэффициенты для всех категорий этажа");
        }
        this.coefficients = new EnumMap<>(coefficients);
    }

    /** Условные коэффициенты: первый этаж 0,95, средний 1, последний 0,97. */
    public static FloorAdjustment standard() {
        return new FloorAdjustment(Map.of(FloorCategory.FIRST, 0.95, FloorCategory.MIDDLE, 1.0,
                FloorCategory.LAST, 0.97));
    }

    @Override
    public double apply(double unitPrice, Apartment subject, MarketListing analog) {
        return unitPrice * coefficients.get(subject.floorCategory()) / coefficients.get(analog.floorCategory());
    }
}
