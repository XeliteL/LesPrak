package realty.model;

/** Квартира, выставленная на продажу по известной цене: общий предок объявления и объекта агентства. */
public abstract class PricedApartment extends Apartment {
    private final int id;
    private final long price;

    protected PricedApartment(int id, String district, int rooms, double area, int floor, int floors,
                              int yearBuilt, long price) {
        super(district, rooms, area, floor, floors, yearBuilt);
        if (price <= 0) {
            throw new IllegalArgumentException("цена должна быть положительной");
        }
        this.id = id;
        this.price = price;
    }

    public int id() { return id; }
    public long price() { return price; }

    /** Цена 1 м², руб. */
    public double unitPrice() {
        return price / area();
    }
}
