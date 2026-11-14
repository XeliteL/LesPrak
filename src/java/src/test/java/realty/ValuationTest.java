package realty;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static realty.Fixtures.SUBJECT;
import static realty.Fixtures.TODAY;
import static realty.Fixtures.flat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import realty.model.MarketListing;
import realty.valuation.ComparativeValuator;
import realty.valuation.ValuationResult;

class ValuationTest {

    private static List<MarketListing> same(int count, long price) {
        List<MarketListing> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            result.add(flat(i, price).listing());
        }
        return result;
    }

    private static ValuationResult estimate(List<MarketListing> listings) {
        return ComparativeValuator.standard(listings).estimate(SUBJECT, TODAY);
    }

    @Test
    void selectAnalogsFilters() {
        List<MarketListing> listings = List.of(
                flat(1, 10_000_000).listing(),
                flat(2, 10_000_000).district("Марьино").listing(),               // другой район
                flat(3, 10_000_000).rooms(3).listing(),                          // другое число комнат
                flat(4, 10_000_000).area(70.0).listing(),                        // площадь +40 %
                flat(5, 10_000_000).date(LocalDate.of(2026, 1, 1)).listing());   // старше 180 дней
        List<MarketListing> analogs = ComparativeValuator.standard(listings).selectAnalogs(SUBJECT, TODAY);
        assertEquals(List.of(1), analogs.stream().map(MarketListing::id).toList());
    }

    @Test
    void floorAdjustment() {
        ComparativeValuator v = ComparativeValuator.standard(List.of());
        double middle = v.adjustedUnitPrice(SUBJECT, flat(1, 10_000_000).listing());
        double first = v.adjustedUnitPrice(SUBJECT, flat(2, 10_000_000).floor(1).listing());
        assertEquals(10_000_000 / 50.0 * 0.97, middle, 1e-6);
        assertTrue(first > middle, "аналог на первом этаже приводится к среднему вверх");
    }

    @Test
    void identicalAnalogsGiveTheirPrice() {
        ValuationResult.Estimate r = assertInstanceOf(ValuationResult.Estimate.class, estimate(same(5, 10_000_000)));
        assertAll(
                () -> assertEquals(9_700_000, r.price()),   // минус 3 % на торг
                () -> assertEquals(r.priceMin(), r.priceMax()));
    }

    @Test
    void outlierExcluded() {
        List<MarketListing> listings = new ArrayList<>(same(4, 10_000_000));
        listings.add(flat(9, 30_000_000).listing());
        ValuationResult.Estimate r = assertInstanceOf(ValuationResult.Estimate.class, estimate(listings));
        assertFalse(r.analogsUsed().contains(9));
        assertEquals(9_700_000, r.price());
    }

    @Test
    void insufficientAnalogs() {
        ValuationResult r = estimate(same(2, 10_000_000));
        assertInstanceOf(ValuationResult.Insufficient.class, r);
        assertEquals(2, r.analogsFound());
    }

    @Test
    void emptyListings() {
        assertInstanceOf(ValuationResult.Insufficient.class, estimate(List.of()));
    }

    @Test
    void additionalAdjustmentPluggedWithoutChangingValuator() {
        // поправка +10 % к удельной цене подключается лямбда-выражением, класс оценщика не меняется
        ComparativeValuator v = new ComparativeValuator(same(5, 10_000_000),
                List.of((unit, subject, analog) -> unit * 1.1));
        ValuationResult.Estimate r = assertInstanceOf(ValuationResult.Estimate.class, v.estimate(SUBJECT, TODAY));
        assertEquals(11_000_000, r.price());
    }
}
