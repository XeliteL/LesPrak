package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import realty.model.Apartment;
import realty.model.FloorCategory;
import realty.model.PropertyObject;
import realty.model.ObjectStatus;

class ModelTest {

    @Test
    void floorCategory() {
        assertEquals(FloorCategory.FIRST, FloorCategory.of(1, 9));
        assertEquals(FloorCategory.LAST, FloorCategory.of(9, 9));
        assertEquals(FloorCategory.MIDDLE, FloorCategory.of(5, 9));
    }

    @Test
    void invalidObjectsAreRejectedByConstructors() {
        assertThrows(IllegalArgumentException.class, () -> new Apartment("Раменки", 2, 0, 5, 10, 2000));
        assertThrows(IllegalArgumentException.class, () -> new Apartment("Раменки", 2, 50, 11, 10, 2000));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyObject(1, "Раменки", 2, 50, 5, 10, 2000, 0, ObjectStatus.ACTIVE));
    }
}
