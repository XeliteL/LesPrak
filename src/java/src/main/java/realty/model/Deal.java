package realty.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Сделка: продажа объекта покупателю с сопровождением агента. */
public final class Deal {
    private final int id;
    private final int agentId;
    private final long price;
    private final double rate;
    private final DealStatus status;
    private final LocalDate closed;
    private final LocalDate signingDate;
    private final Set<DealCondition> conditions;
    private final List<DealStage> stages;
    private final List<Document> documents;

    private Deal(Builder b) {
        if (b.price <= 0 || b.rate < 0) {
            throw new IllegalArgumentException("некорректная цена или ставка сделки " + b.id);
        }
        if (b.status == DealStatus.CLOSED && b.closed == null) {
            throw new IllegalArgumentException("у закрытой сделки " + b.id + " нет даты закрытия");
        }
        id = b.id;
        agentId = b.agentId;
        price = b.price;
        rate = b.rate;
        status = b.status;
        closed = b.closed;
        signingDate = b.signingDate;
        conditions = b.conditions.isEmpty() ? Set.of() : EnumSet.copyOf(b.conditions);
        stages = List.copyOf(b.stages);
        documents = List.copyOf(b.documents);
    }

    public static Builder builder(int id, int agentId, long price, double rate) {
        return new Builder(id, agentId, price, rate);
    }

    public int id() { return id; }
    public int agentId() { return agentId; }
    public long price() { return price; }
    public double rate() { return rate; }
    public DealStatus status() { return status; }
    public LocalDate closed() { return closed; }
    public LocalDate signingDate() { return signingDate; }
    public List<DealStage> stages() { return stages; }
    public List<Document> documents() { return documents; }

    public boolean has(DealCondition condition) {
        return conditions.contains(condition);
    }

    /** Сделка закрыта в указанном месяце. */
    public boolean isClosedIn(YearMonth month) {
        return status == DealStatus.CLOSED && YearMonth.from(closed).equals(month);
    }

    /** Построитель сделки: у сделки много необязательных атрибутов. */
    public static final class Builder {
        private final int id;
        private final int agentId;
        private final long price;
        private final double rate;
        private DealStatus status = DealStatus.IN_PROGRESS;
        private LocalDate closed;
        private LocalDate signingDate;
        private final Set<DealCondition> conditions = EnumSet.noneOf(DealCondition.class);
        private List<DealStage> stages = List.of();
        private List<Document> documents = List.of();

        private Builder(int id, int agentId, long price, double rate) {
            this.id = id;
            this.agentId = agentId;
            this.price = price;
            this.rate = rate;
        }

        public Builder status(DealStatus value) { status = value; return this; }
        public Builder closed(LocalDate value) { closed = value; return this; }
        public Builder signingDate(LocalDate value) { signingDate = value; return this; }
        public Builder condition(DealCondition value) { conditions.add(value); return this; }
        public Builder stages(List<DealStage> value) { stages = value; return this; }
        public Builder documents(List<Document> value) { documents = value; return this; }

        public Deal build() {
            return new Deal(this);
        }
    }
}
