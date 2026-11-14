package realty.commission;

/** Строка отчёта: комиссия агентства и вознаграждение агента по одной сделке. */
public record CommissionLine(int dealId, int agentId, long fee, long agentShare) {
}
