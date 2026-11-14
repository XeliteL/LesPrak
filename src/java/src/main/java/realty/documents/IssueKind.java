package realty.documents;

/** Вид замечания по сделке. Блокирующие замечания не позволяют подписать договор. */
public enum IssueKind {
    MISSING("Не хватает", true),
    EXPIRED("Просрочены к подписанию", true),
    EXPIRING("Скоро истекают", false),
    OVERDUE_STAGE("Просрочены этапы", false),
    AML_CONTROL("Обязательный контроль (115-ФЗ)", false);

    private final String title;
    private final boolean blocking;

    IssueKind(String title, boolean blocking) {
        this.title = title;
        this.blocking = blocking;
    }

    public String title() { return title; }
    public boolean isBlocking() { return blocking; }
}
