package realty;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import realty.commission.CommissionCalculator;
import realty.commission.CommissionLine;
import realty.commission.MonthlyReport;
import realty.documents.DealInspector;
import realty.documents.DealReport;
import realty.documents.IssueKind;
import realty.io.DataFormatException;
import realty.io.DataLoader;
import realty.io.DataSet;
import realty.matching.Match;
import realty.matching.ObjectMatcher;
import realty.model.Apartment;
import realty.model.BuyerRequest;
import realty.model.Deal;
import realty.model.DealStatus;
import realty.model.PropertyObject;
import realty.valuation.ComparativeValuator;
import realty.valuation.ValuationResult;

import static realty.io.Rub.format;

/**
 * Демонстрация работы алгоритмов на условных данных.
 * Запуск из каталога src/java: java -jar target/realty-1.0.jar [путь к JSON]
 */
public final class Main {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 7);
    static final String DEFAULT_DATA = "../python/data/sample.json";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        DataSet data;
        try {
            data = DataLoader.load(Path.of(args.length > 0 ? args[0] : DEFAULT_DATA));
        } catch (DataFormatException e) {
            System.out.println("Ошибки в данных:");
            e.errors().forEach(err -> System.out.println("  " + err));
            System.exit(1);
            return;
        }
        showValuation(data);
        showMatching(data);
        showDocuments(data);
        showCommission(data);
    }

    static void showValuation(DataSet data) {
        System.out.println("=== Алгоритм 1. Оценка стоимости квартиры");
        Apartment subject = new Apartment("Раменки", 2, 54.0, 7, 17, 2005);
        System.out.printf("Объект: %s, этаж %d/%d, %d г.%n", subject.describe(), subject.floor(), subject.floors(),
                subject.yearBuilt());
        ValuationResult result = ComparativeValuator.standard(data.listings()).estimate(subject, TODAY);
        switch (result) {
            case ValuationResult.Insufficient r ->
                    System.out.println("Недостаточно аналогов: найдено " + r.analogsFound());
            case ValuationResult.Estimate r -> {
                System.out.printf("Аналогов найдено: %d, использовано: %d%n", r.analogsFound(), r.analogsUsed().size());
                System.out.println("Цена 1 м²: " + format(r.unitPrice()));
                System.out.printf("Рекомендуемая цена: %s (диапазон %s — %s)%n", format(r.price()),
                        format(r.priceMin()), format(r.priceMax()));
            }
        }
    }

    static void showMatching(DataSet data) {
        System.out.println("\n=== Алгоритм 2. Подбор объектов под заявку покупателя");
        ObjectMatcher matcher = ObjectMatcher.standard();
        for (BuyerRequest req : data.requests()) {
            System.out.printf("Заявка %d: бюджет до %s, комнат %s, районы: %s%n", req.id(), format(req.budgetMax()),
                    req.rooms().stream().sorted().toList(),
                    req.anyDistrict() ? "любой" : String.join(", ", req.districts()));
            for (Match m : matcher.match(req, data.objects(), 5)) {
                PropertyObject o = m.object();
                System.out.printf(Locale.ROOT, "  %5.1f  объект %3d: %s, эт. %d/%d, %s%n", m.score(), o.id(),
                        o.describe(), o.floor(), o.floors(), format(o.price()));
            }
        }
    }

    static void showDocuments(DataSet data) {
        System.out.println("\n=== Алгоритм 3. Контроль документов и сроков сделки");
        DealInspector inspector = DealInspector.standard(data.docTypes());
        for (Deal deal : data.deals()) {
            if (deal.status() != DealStatus.IN_PROGRESS) {
                continue;
            }
            DealReport r = inspector.inspect(deal, TODAY);
            System.out.printf("Сделка %d, подписание %s: %s к подписанию%n", deal.id(), deal.signingDate() != null ? deal.signingDate() : "не назначено",
                    r.isReady() ? "готова" : "не готова");
            for (IssueKind kind : List.of(IssueKind.MISSING, IssueKind.EXPIRED, IssueKind.EXPIRING,
                    IssueKind.OVERDUE_STAGE)) {
                List<String> subjects = r.subjects(kind);
                if (!subjects.isEmpty()) {
                    System.out.println("  " + kind.title() + ": " + String.join("; ", subjects));
                }
            }
            if (r.requiresAmlControl()) {
                System.out.println("  Сделка подлежит обязательному контролю (115-ФЗ): "
                        + "направить сведения в Росфинмониторинг");
            }
        }
    }

    static void showCommission(DataSet data) {
        System.out.println("\n=== Алгоритм 4. Комиссия за октябрь 2026");
        MonthlyReport r = CommissionCalculator.standard().monthly(data.deals(), YearMonth.of(2026, 10));
        for (CommissionLine line : r.lines()) {
            System.out.printf("  сделка %d, агент %d: комиссия %s, агенту %s%n", line.dealId(), line.agentId(),
                    format(line.fee()), format(line.agentShare()));
        }
        r.agents().forEach((agentId, t) -> System.out.printf("Агент %d: комиссия %s, вознаграждение %s%n",
                agentId, format(t.fee()), format(t.agentShare())));
        System.out.printf("Итого комиссия: %s, доход агентства: %s%n", format(r.totalFee()), format(r.agencyIncome()));
    }
}
