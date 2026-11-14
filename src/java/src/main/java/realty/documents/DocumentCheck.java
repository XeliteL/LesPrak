package realty.documents;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import realty.model.Deal;
import realty.model.Document;
import realty.model.DocumentType;

/**
 * Проверка комплектности и сроков действия документов (шаги 1–3 алгоритма 3).
 * Сложность O(t + d): t — число типов документов, d — число полученных документов.
 */
public final class DocumentCheck implements DealCheck {
    public static final int WARN_DAYS = 7;

    private final List<DocumentType> types;

    public DocumentCheck(List<DocumentType> types) {
        this.types = List.copyOf(types);
    }

    /** Шаг 1: типы документов, обязательные для сделки. */
    public List<DocumentType> requiredTypes(Deal deal) {
        return types.stream().filter(t -> t.isRequiredFor(deal)).toList();
    }

    /** Шаг 2: словарь «код типа → последний полученный документ этого типа». */
    public static Map<String, Document> index(List<Document> documents) {
        Map<String, Document> index = new HashMap<>();
        for (Document doc : documents) {
            index.merge(doc.typeCode(), doc, (old, cur) -> cur.received().isAfter(old.received()) ? cur : old);
        }
        return index;
    }

    @Override
    public void check(Deal deal, LocalDate today, DealReport report) {
        Map<String, Document> index = index(deal.documents());
        for (DocumentType type : requiredTypes(deal)) {
            Document doc = index.get(type.code());
            if (doc == null) {
                report.add(IssueKind.MISSING, type.name());
                continue;
            }
            Optional<LocalDate> until = type.validUntil(doc);
            if (until.isEmpty()) {
                continue;
            }
            if (until.get().isBefore(deal.signingDate())) {
                report.add(IssueKind.EXPIRED, type.name());
            } else if (!until.get().isAfter(today.plusDays(WARN_DAYS))) {
                report.add(IssueKind.EXPIRING, type.name());
            }
        }
    }
}
