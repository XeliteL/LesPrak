package realty.matching;

import realty.model.BuyerRequest;
import realty.model.PropertyObject;

/** Жёсткое условие заявки: объект, не прошедший проверку, покупателю не предлагается. */
@FunctionalInterface
public interface RequestFilter {

    boolean accepts(BuyerRequest request, PropertyObject object);
}
