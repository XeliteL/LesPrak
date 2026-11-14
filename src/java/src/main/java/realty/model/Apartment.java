package realty.model;

import java.util.Objects;

/**
 * Квартира как совокупность характеристик, по которым её сравнивают с другими квартирами.
 * Корень иерархии квартир; экземпляр этого класса без цены описывает квартиру, которую оценивают.
 */
public class Apartment {
    private final String district;
    private final int rooms;
    private final double area;
    private final int floor;
    private final int floors;
    private final int yearBuilt;

    public Apartment(String district, int rooms, double area, int floor, int floors, int yearBuilt) {
        if (rooms < 0 || area <= 0 || floor < 1 || floors < floor) {
            throw new IllegalArgumentException("некорректные характеристики квартиры");
        }
        this.district = Objects.requireNonNull(district);
        this.rooms = rooms;
        this.area = area;
        this.floor = floor;
        this.floors = floors;
        this.yearBuilt = yearBuilt;
    }

    public String district() { return district; }
    public int rooms() { return rooms; }
    public double area() { return area; }
    public int floor() { return floor; }
    public int floors() { return floors; }
    public int yearBuilt() { return yearBuilt; }

    public FloorCategory floorCategory() {
        return FloorCategory.of(floor, floors);
    }

    /** Краткое описание для вывода: «Раменки, 2-комн., 54.0 м²». */
    public String describe() {
        return district + ", " + rooms + "-комн., " + area + " м²";
    }
}
