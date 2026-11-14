package realty.documents;

import java.time.LocalDate;

import realty.model.Deal;

/**
 * Признак обязательного контроля: сделка с недвижимостью на сумму от порога
 * (п. 1.1 ст. 6 Федерального закона от 07.08.2001 № 115-ФЗ) — шаг 5 алгоритма 3.
 */
public final class AmlThresholdCheck implements DealCheck {
    public static final long DEFAULT_THRESHOLD = 5_000_000;

    private final long threshold;

    public AmlThresholdCheck(long threshold) {
        this.threshold = threshold;
    }

    @Override
    public void check(Deal deal, LocalDate today, DealReport report) {
        if (deal.price() >= threshold) {
            report.add(IssueKind.AML_CONTROL, "сведения направляются в Росфинмониторинг");
        }
    }
}
