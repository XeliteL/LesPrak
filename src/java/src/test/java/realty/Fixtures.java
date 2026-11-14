package realty;

import java.time.LocalDate;

import realty.model.Apartment;
import realty.model.MarketListing;
import realty.model.ObjectStatus;
import realty.model.PropertyObject;

/** Общие тестовые данные (повторяют функцию flat из тестов процедурной реализации). */
final class Fixtures {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 7);
    static final Apartment SUBJECT = new Apartment("Раменки", 2, 50.0, 5, 10, 2000);

    private Fixtures() {
    }

    static Flat flat(int id, long price) {
        return new Flat(id, price);
    }

    /** Построитель тестовой квартиры с характеристиками по умолчанию. */
    static final class Flat {
        int id;
        long price;
        double area = 50.0;
        int floor = 5;
        int floors = 10;
        String district = "Раменки";
        int rooms = 2;
        int year = 2000;
        LocalDate date = LocalDate.of(2026, 10, 1);
        ObjectStatus status = ObjectStatus.ACTIVE;

        Flat(int id, long price) {
            this.id = id;
            this.price = price;
        }

        Flat area(double v) { area = v; return this; }
        Flat floor(int v) { floor = v; return this; }
        Flat district(String v) { district = v; return this; }
        Flat rooms(int v) { rooms = v; return this; }
        Flat date(LocalDate v) { date = v; return this; }
        Flat status(ObjectStatus v) { status = v; return this; }

        MarketListing listing() {
            return new MarketListing(id, district, rooms, area, floor, floors, year, price, date);
        }

        PropertyObject object() {
            return new PropertyObject(id, district, rooms, area, floor, floors, year, price, status);
        }
    }
}
