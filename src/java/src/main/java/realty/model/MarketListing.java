package realty.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Рыночное предложение: объявление о продаже квартиры, собранное с онлайн-площадки для оценки. */
public final class MarketListing extends PricedApartment {
    private final LocalDate published;

    public MarketListing(int id, String district, int rooms, double area, int floor, int floors,
                         int yearBuilt, long price, LocalDate published) {
        super(id, district, rooms, area, floor, floors, yearBuilt, price);
        this.published = published;
    }

    public LocalDate published() { return published; }

    /** Возраст объявления на дату today, дней. */
    public long ageDays(LocalDate today) {
        return ChronoUnit.DAYS.between(published, today);
    }
}
