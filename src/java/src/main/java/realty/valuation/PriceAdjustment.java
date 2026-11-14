package realty.valuation;

import realty.model.Apartment;
import realty.model.MarketListing;

/** Корректировка удельной цены аналога, приводящая её к условиям оцениваемой квартиры. */
@FunctionalInterface
public interface PriceAdjustment {

    double apply(double unitPrice, Apartment subject, MarketListing analog);
}
