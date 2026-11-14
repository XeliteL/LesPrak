package realty.model;

/** Объект агентства: квартира, которую агентство продаёт по договору с собственником. */
public final class PropertyObject extends PricedApartment {
    private final ObjectStatus status;

    public PropertyObject(int id, String district, int rooms, double area, int floor, int floors,
                          int yearBuilt, long price, ObjectStatus status) {
        super(id, district, rooms, area, floor, floors, yearBuilt, price);
        this.status = status;
    }

    public ObjectStatus status() { return status; }

    public boolean isOnSale() {
        return status == ObjectStatus.ACTIVE;
    }
}
