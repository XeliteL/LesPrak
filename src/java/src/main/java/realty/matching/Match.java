package realty.matching;

import realty.model.PropertyObject;

/** Объект, подобранный под заявку, и его оценка соответствия (0–100). */
public record Match(double score, PropertyObject object) {
}
