package realty.documents;

import java.time.LocalDate;
import java.util.List;

import realty.model.Deal;
import realty.model.DocumentType;

/** Алгоритм 3. Контроль документов и сроков сделки: последовательно применяет набор проверок. */
public final class DealInspector {
    private final List<DealCheck> checks;

    public DealInspector(List<? extends DealCheck> checks) {
        this.checks = List.copyOf(checks);
    }

    /** Стандартный набор проверок: документы, этапы, порог 115-ФЗ. */
    public static DealInspector standard(List<DocumentType> types) {
        return new DealInspector(List.of(new DocumentCheck(types), new StageDeadlineCheck(),
                new AmlThresholdCheck(AmlThresholdCheck.DEFAULT_THRESHOLD)));
    }

    public DealReport inspect(Deal deal, LocalDate today) {
        DealReport report = new DealReport();
        for (DealCheck check : checks) {
            check.check(deal, today, report);
        }
        return report;
    }
}
