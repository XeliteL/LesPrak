package realty.model;

import java.time.LocalDate;

/** Документ, полученный по сделке. */
public record Document(String typeCode, LocalDate received) {
}
