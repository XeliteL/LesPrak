package realty.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/**
 * Алгоритм 2. Подбор объектов под заявку покупателя.
 *
 * <p>Объект проверяется набором {@link RequestFilter}, оценка соответствия складывается из
 * {@link ScoreComponent}; лучшие N объектов отбираются минимальной кучей размера N.
 * Сложность O(n log N).
 */
public final class ObjectMatcher {
    /** Порядок «хуже — лучше»: по оценке, при равенстве лучше объект с меньшим номером. */
    private static final Comparator<Match> WORSE_FIRST = Comparator.comparingDouble(Match::score)
            .thenComparing(m -> m.object().id(), Comparator.reverseOrder());

    private final List<RequestFilter> filters;
    private final List<ScoreComponent> components;

    public ObjectMatcher(List<? extends RequestFilter> filters, List<? extends ScoreComponent> components) {
        this.filters = List.copyOf(filters);
        this.components = List.copyOf(components);
    }

    /** Подбор по стандартным условиям и весам: цена 50, площадь 30, район 20. */
    public static ObjectMatcher standard() {
        return new ObjectMatcher(List.of(StandardFilter.values()),
                List.of(new PriceScore(50), new AreaScore(30), new DistrictScore(20)));
    }

    /** Шаг 1: проверка всех жёстких условий. */
    public boolean passes(BuyerRequest request, PropertyObject object) {
        for (RequestFilter f : filters) {
            if (!f.accepts(request, object)) {
                return false;
            }
        }
        return true;
    }

    /** Шаг 2: оценка соответствия от 0 до 100, округлённая до десятых. */
    public double score(BuyerRequest request, PropertyObject object) {
        double sum = 0;
        for (ScoreComponent c : components) {
            sum += c.score(request, object);
        }
        return new BigDecimal(sum).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** Шаги 3–4: до topN объектов в порядке убывания оценки. */
    public List<Match> match(BuyerRequest request, Collection<PropertyObject> objects, int topN) {
        if (topN <= 0) {
            return List.of();
        }
        PriorityQueue<Match> heap = new PriorityQueue<>(topN, WORSE_FIRST);
        for (PropertyObject o : objects) {
            if (!passes(request, o)) {
                continue;
            }
            Match m = new Match(score(request, o), o);
            if (heap.size() < topN) {
                heap.add(m);
            } else if (WORSE_FIRST.compare(m, heap.peek()) > 0) {
                heap.poll();
                heap.add(m);
            }
        }
        List<Match> result = new ArrayList<>(heap);
        result.sort(WORSE_FIRST.reversed());
        return result;
    }
}
