package realty.model;

import java.time.LocalDate;

/** Этап (контрольная точка) сделки. */
public record DealStage(String name, LocalDate due, boolean done) {

    /** Этап просрочен, если не выполнен, а плановая дата уже прошла. */
    public boolean isOverdue(LocalDate today) {
        return !done && due.isBefore(today);
    }
}
