package realty.model;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Тип документа из справочника.
 *
 * @param requiredIf   условие обязательности; null — документ нужен всегда
 * @param validityDays срок действия, дней; null — бессрочный
 */
public record DocumentType(String code, String name, DealCondition requiredIf, Integer validityDays) {

    public boolean isRequiredFor(Deal deal) {
        return requiredIf == null || deal.has(requiredIf);
    }

    /** Последний день действия документа этого типа; пусто для бессрочных документов. */
    public Optional<LocalDate> validUntil(Document doc) {
        return validityDays == null ? Optional.empty() : Optional.of(doc.received().plusDays(validityDays));
    }
}
