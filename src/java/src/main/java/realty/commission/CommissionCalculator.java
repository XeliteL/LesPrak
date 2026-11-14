package realty.commission;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import realty.model.Deal;

/**
 * Алгоритм 4. Расчёт комиссии агентства и вознаграждения агентов за месяц.
 * Сложность O(m + m′ log m′): m — число сделок, m′ — число сделок, закрытых в месяце.
 */
public final class CommissionCalculator {
    private static final Comparator<Deal> BY_CLOSING = Comparator.comparing(Deal::closed).thenComparingInt(Deal::id);

    private final FeePolicy feePolicy;
    private final RewardScale scale;

    public CommissionCalculator(FeePolicy feePolicy, RewardScale scale) {
        this.feePolicy = feePolicy;
        this.scale = scale;
    }

    public static CommissionCalculator standard() {
        return new CommissionCalculator(new PercentFeeWithMinimum(PercentFeeWithMinimum.DEFAULT_MIN_FEE),
                ProgressiveScale.standard());
    }

    public MonthlyReport monthly(Collection<Deal> deals, YearMonth month) {
        SortedMap<Integer, List<Deal>> byAgent = new TreeMap<>();
        for (Deal d : deals) {
            if (d.isClosedIn(month)) {
                byAgent.computeIfAbsent(d.agentId(), k -> new ArrayList<>()).add(d);
            }
        }

        List<CommissionLine> lines = new ArrayList<>();
        SortedMap<Integer, AgentTotal> totals = new TreeMap<>();
        for (Map.Entry<Integer, List<Deal>> e : byAgent.entrySet()) {
            List<Deal> agentDeals = e.getValue();
            agentDeals.sort(BY_CLOSING);
            long accumulated = 0;
            long agentTotal = 0;
            for (Deal d : agentDeals) {
                long fee = feePolicy.fee(d);
                long share = scale.share(fee, accumulated);
                accumulated += fee;
                agentTotal += share;
                lines.add(new CommissionLine(d.id(), e.getKey(), fee, share));
            }
            totals.put(e.getKey(), new AgentTotal(accumulated, agentTotal));
        }
        return new MonthlyReport(month, Collections.unmodifiableList(lines), Collections.unmodifiableSortedMap(totals));
    }
}
