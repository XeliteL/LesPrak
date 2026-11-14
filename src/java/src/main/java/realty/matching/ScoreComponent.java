package realty.matching;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/** Составляющая оценки соответствия объекта заявке. */
public interface ScoreComponent {

    /** Вес составляющей в итоговой оценке (сумма весов равна 100). */
    double weight();

    /** Показатель соответствия от 0 до 1. */
    double part(BuyerRequest request, PropertyObject object);

    default double score(BuyerRequest request, PropertyObject object) {
        return weight() * part(request, object);
    }
}
