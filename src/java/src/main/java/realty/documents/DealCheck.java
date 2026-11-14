package realty.documents;

import java.time.LocalDate;

import realty.model.Deal;

/** Одна проверка сделки; найденные замечания добавляются в отчёт. */
public interface DealCheck {

    void check(Deal deal, LocalDate today, DealReport report);
}
