package realty.documents;

import java.util.ArrayList;
import java.util.List;

/** Результат проверки сделки: список замечаний. Сделка готова, если нет блокирующих замечаний. */
public final class DealReport {
    private final List<Issue> issues = new ArrayList<>();

    void add(IssueKind kind, String subject) {
        issues.add(new Issue(kind, subject));
    }

    public List<Issue> issues() {
        return List.copyOf(issues);
    }

    public boolean isReady() {
        return issues.stream().noneMatch(i -> i.kind().isBlocking());
    }

    /** Предметы замечаний заданного вида в порядке обнаружения. */
    public List<String> subjects(IssueKind kind) {
        return issues.stream().filter(i -> i.kind() == kind).map(Issue::subject).toList();
    }

    public boolean requiresAmlControl() {
        return issues.stream().anyMatch(i -> i.kind() == IssueKind.AML_CONTROL);
    }
}
