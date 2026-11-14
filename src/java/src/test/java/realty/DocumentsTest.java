package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static realty.Fixtures.TODAY;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import realty.documents.DealInspector;
import realty.documents.DealReport;
import realty.documents.DocumentCheck;
import realty.documents.IssueKind;
import realty.model.Deal;
import realty.model.DealCondition;
import realty.model.DealStage;
import realty.model.Document;
import realty.model.DocumentType;

class DocumentsTest {
    static final List<DocumentType> TYPES = List.of(
            new DocumentType("egrn", "Выписка из ЕГРН", null, 30),
            new DocumentType("passport", "Паспорт продавца", null, null),
            new DocumentType("bank", "Одобрение банка", DealCondition.MORTGAGE, null));
    static final DealInspector INSPECTOR = DealInspector.standard(TYPES);

    static Deal deal(boolean mortgage, long price, List<DealStage> stages, List<Document> docs) {
        Deal.Builder b = Deal.builder(1, 1, price, 0.03).signingDate(LocalDate.of(2026, 11, 20))
                .stages(stages).documents(docs);
        if (mortgage) {
            b.condition(DealCondition.MORTGAGE);
        }
        return b.build();
    }

    static Deal deal(boolean mortgage, List<Document> docs) {
        return deal(mortgage, 4_000_000, List.of(), docs);
    }

    static Document doc(String type, int month, int day) {
        return new Document(type, LocalDate.of(2026, month, day));
    }

    @Test
    void requiredTypesDependOnConditions() {
        DocumentCheck check = new DocumentCheck(TYPES);
        assertEquals(2, check.requiredTypes(deal(false, List.of())).size());
        assertEquals(3, check.requiredTypes(deal(true, List.of())).size());
    }

    @Test
    void indexKeepsLatest() {
        var index = DocumentCheck.index(List.of(doc("egrn", 10, 1), doc("egrn", 11, 1)));
        assertEquals(LocalDate.of(2026, 11, 1), index.get("egrn").received());
    }

    @Test
    void ready() {
        DealReport r = INSPECTOR.inspect(deal(false, List.of(doc("egrn", 11, 1), doc("passport", 10, 1))), TODAY);
        assertTrue(r.isReady());
        assertFalse(r.requiresAmlControl());
    }

    @Test
    void missingExpiredExpiring() {
        // выписка действует до 09.11, подписание 20.11
        DealReport r = INSPECTOR.inspect(deal(true, List.of(doc("egrn", 10, 10))), TODAY);
        assertFalse(r.isReady());
        assertEquals(List.of("Паспорт продавца", "Одобрение банка"), r.subjects(IssueKind.MISSING));
        assertEquals(List.of("Выписка из ЕГРН"), r.subjects(IssueKind.EXPIRED));
        // выписка действует до 20.11 — на 07.11 до истечения 13 дней, на 15.11 — 5 дней
        Deal d = deal(false, List.of(doc("egrn", 10, 21), doc("passport", 10, 1)));
        r = INSPECTOR.inspect(d, TODAY);
        assertTrue(r.isReady());
        assertEquals(List.of(), r.subjects(IssueKind.EXPIRING));
        r = INSPECTOR.inspect(d, LocalDate.of(2026, 11, 15));
        assertEquals(List.of("Выписка из ЕГРН"), r.subjects(IssueKind.EXPIRING));
    }

    @Test
    void amlThresholdAndOverdue() {
        List<DealStage> stages = List.of(
                new DealStage("Аванс", LocalDate.of(2026, 11, 1), true),
                new DealStage("Одобрение", LocalDate.of(2026, 11, 5), false),
                new DealStage("Подписание", LocalDate.of(2026, 11, 20), false));
        DealReport r = INSPECTOR.inspect(deal(false, 5_000_000, stages, List.of()), TODAY);
        assertTrue(r.requiresAmlControl());
        assertEquals(List.of("Одобрение"), r.subjects(IssueKind.OVERDUE_STAGE));
    }
}
