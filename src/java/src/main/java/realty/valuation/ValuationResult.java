package realty.valuation;

import java.util.List;

/** Результат оценки: либо оценка выполнена, либо аналогов недостаточно. */
public sealed interface ValuationResult permits ValuationResult.Estimate, ValuationResult.Insufficient {

    /** Число аналогов, отобранных по критериям сопоставимости. */
    int analogsFound();

    /** Оценка выполнена: цены округлены до ROUND_TO руб. */
    record Estimate(long unitPrice, long price, long priceMin, long priceMax, int analogsFound,
                    List<Integer> analogsUsed) implements ValuationResult {
    }

    /** Оценка не выполнена: аналогов меньше минимально допустимого числа. */
    record Insufficient(int analogsFound) implements ValuationResult {
    }
}
