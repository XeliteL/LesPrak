package realty.valuation;

import realty.model.Apartment;
import realty.model.MarketListing;

/** Скидка на торг: цена сделки обычно ниже цены предложения. */
public final class BargainDiscount implements PriceAdjustment {
    private final double discount;

    public BargainDiscount(double discount) {
        if (discount < 0 || discount >= 1) {
            throw new IllegalArgumentException("скидка должна быть в диапазоне [0; 1)");
        }
        this.discount = discount;
    }

    @Override
    public double apply(double unitPrice, Apartment subject, MarketListing analog) {
        return unitPrice * (1 - discount);
    }
}
