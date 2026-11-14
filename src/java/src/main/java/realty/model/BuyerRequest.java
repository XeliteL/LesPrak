package realty.model;

import java.util.List;
import java.util.Set;

/**
 * Заявка покупателя.
 *
 * @param districts подходящие районы в порядке убывания приоритета; пустой список — любой район
 */
public record BuyerRequest(int id, String client, long budgetMin, long budgetMax, Set<Integer> rooms,
                           List<String> districts, double areaMin, boolean notFirstFloor, boolean notLastFloor) {

    public BuyerRequest {
        if (budgetMax <= 0 || budgetMin > budgetMax) {
            throw new IllegalArgumentException("некорректный бюджет заявки");
        }
        rooms = Set.copyOf(rooms);
        districts = List.copyOf(districts);
    }

    public boolean anyDistrict() {
        return districts.isEmpty();
    }

    /** Позиция района в списке предпочтений (0 — наивысший приоритет) или -1. */
    public int districtRank(String district) {
        return districts.indexOf(district);
    }
}
