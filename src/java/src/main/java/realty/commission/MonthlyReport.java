package realty.commission;

import java.time.YearMonth;
import java.util.List;
import java.util.SortedMap;

/** Отчёт о комиссии за месяц. */
public record MonthlyReport(YearMonth month, List<CommissionLine> lines, SortedMap<Integer, AgentTotal> agents) {

    public long totalFee() {
        return agents.values().stream().mapToLong(AgentTotal::fee).sum();
    }

    public long totalAgentShare() {
        return agents.values().stream().mapToLong(AgentTotal::agentShare).sum();
    }

    /** Доход агентства: комиссия за вычетом вознаграждений агентов. */
    public long agencyIncome() {
        return totalFee() - totalAgentShare();
    }
}
