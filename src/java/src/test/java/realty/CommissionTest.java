package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import realty.commission.AgentTotal;
import realty.commission.CommissionCalculator;
import realty.commission.CommissionLine;
import realty.commission.MonthlyReport;
import realty.commission.PercentFeeWithMinimum;
import realty.commission.ProgressiveScale;
import realty.commission.Tier;
import realty.model.Deal;
import realty.model.DealStatus;

class CommissionTest {
    final ProgressiveScale scale = ProgressiveScale.standard();

    static Deal closed(int id, int agent, int month, int day) {
        return Deal.builder(id, agent, 10_000_000, 0.03).status(DealStatus.CLOSED)
                .closed(LocalDate.of(2026, month, day)).build();
    }

    @Test
    void agencyFeeMinimum() {
        PercentFeeWithMinimum policy = new PercentFeeWithMinimum(100_000);
        assertEquals(300_000, policy.fee(Deal.builder(1, 1, 10_000_000, 0.03).build()));
        assertEquals(100_000, policy.fee(Deal.builder(2, 1, 2_000_000, 0.03).build()));
    }

    @Test
    void agentShareTiers() {
        assertEquals(120_000, scale.share(300_000, 0));
        assertEquals(20_000 + 25_000, scale.share(100_000, 250_000));   // переход через границу 300 000
        assertEquals(60_000, scale.share(100_000, 1_000_000));
        // сумма по частям равна расчёту одной суммой
        assertEquals(scale.share(729_000, 0), scale.share(435_000, 0) + scale.share(294_000, 435_000));
    }

    @Test
    void scaleValidatesTiers() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProgressiveScale(List.of(new Tier(600_000, 0.5), new Tier(300_000, 0.4))));
        assertThrows(IllegalArgumentException.class, () -> new ProgressiveScale(List.of(new Tier(300_000, 0.4))));
    }

    @Test
    void monthlyReport() {
        List<Deal> deals = List.of(closed(1, 1, 10, 5), closed(2, 1, 10, 25), closed(3, 2, 11, 1),
                Deal.builder(4, 2, 10_000_000, 0.03).build());
        MonthlyReport r = CommissionCalculator.standard().monthly(deals, YearMonth.of(2026, 10));
        assertEquals(List.of(1, 2), r.lines().stream().map(CommissionLine::dealId).toList());
        assertEquals(new AgentTotal(600_000, 120_000 + 150_000), r.agents().get(1));
        assertEquals(600_000 - 270_000, r.agencyIncome());
    }
}
