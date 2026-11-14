package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static realty.Fixtures.flat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import realty.matching.Match;
import realty.matching.ObjectMatcher;
import realty.matching.PriceScore;
import realty.matching.RequestFilter;
import realty.matching.StandardFilter;
import realty.model.BuyerRequest;
import realty.model.ObjectStatus;
import realty.model.PropertyObject;

class MatchingTest {
    static final BuyerRequest REQ = new BuyerRequest(1, "", 0, 10_000_000, Set.of(2), List.of("Раменки", "Марьино"),
            40, true, false);
    final ObjectMatcher matcher = ObjectMatcher.standard();

    @Test
    void filters() {
        assertTrue(matcher.passes(REQ, flat(1, 10_400_000).object()));                       // в пределах допуска 5 %
        assertFalse(matcher.passes(REQ, flat(2, 10_600_000).object()));                      // дороже допуска
        assertFalse(matcher.passes(REQ, flat(3, 9_000_000).floor(1).object()));
        assertFalse(matcher.passes(REQ, flat(4, 9_000_000).district("Бутово Северное").object()));
        assertFalse(matcher.passes(REQ, flat(5, 9_000_000).status(ObjectStatus.SOLD).object()));
    }

    @Test
    void rankingAndTopN() {
        List<PropertyObject> objects = List.of(
                flat(1, 9_000_000).area(52.0).object(),                         // лучший
                flat(2, 9_000_000).area(52.0).district("Марьино").object(),     // второй район в списке
                flat(3, 10_400_000).area(52.0).object(),                         // дороже бюджета
                flat(4, 9_000_000).area(30.0).object());                         // не проходит по площади
        List<Match> result = matcher.match(REQ, objects, 2);
        assertEquals(List.of(1, 2), result.stream().map(m -> m.object().id()).toList());
        assertEquals(100.0, result.get(0).score());
    }

    @Test
    void noMatches() {
        assertEquals(List.of(), matcher.match(REQ, List.of(), 5));
    }

    @Test
    void customFilterAddedWithoutChangingMatcher() {
        // новое условие «дом не старше 2010 года» задаётся реализацией интерфейса RequestFilter
        RequestFilter newBuilding = (r, o) -> o.yearBuilt() >= 2010;
        List<RequestFilter> filters = new java.util.ArrayList<>(List.of(StandardFilter.values()));
        filters.add(newBuilding);
        ObjectMatcher custom = new ObjectMatcher(filters, List.of(new PriceScore(100)));
        assertEquals(List.of(), custom.match(REQ, List.of(flat(1, 9_000_000).area(52.0).object()), 5));
    }
}
