package realty.documents;

import java.time.LocalDate;

import realty.model.Deal;
import realty.model.DealStage;

/** Проверка сроков этапов сделки (шаг 4 алгоритма 3). */
public final class StageDeadlineCheck implements DealCheck {

    @Override
    public void check(Deal deal, LocalDate today, DealReport report) {
        for (DealStage stage : deal.stages()) {
            if (stage.isOverdue(today)) {
                report.add(IssueKind.OVERDUE_STAGE, stage.name());
            }
        }
    }
}
