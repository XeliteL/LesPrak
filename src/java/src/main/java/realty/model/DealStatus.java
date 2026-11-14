package realty.model;

/** Статус сделки. */
public enum DealStatus {
    IN_PROGRESS("in_progress"), CLOSED("closed"), CANCELLED("cancelled");

    private final String code;

    DealStatus(String code) { this.code = code; }

    public static DealStatus byCode(String code) {
        for (DealStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("неизвестный статус сделки: " + code);
    }
}
