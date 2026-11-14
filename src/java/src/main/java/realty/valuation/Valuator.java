package realty.valuation;

import java.time.LocalDate;

import realty.model.Apartment;

/** Способ оценки рыночной стоимости квартиры. */
public interface Valuator {

    ValuationResult estimate(Apartment subject, LocalDate today);
}
