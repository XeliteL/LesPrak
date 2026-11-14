package realty.model;

/** Категория этажа квартиры: влияет на цену и на условия заявки покупателя. */
public enum FloorCategory {
    FIRST, MIDDLE, LAST;

    /** Определяет категорию по номеру этажа и этажности дома. */
    public static FloorCategory of(int floor, int floors) {
        if (floor <= 1) {
            return FIRST;
        }
        if (floor >= floors) {
            return LAST;
        }
        return MIDDLE;
    }
}
