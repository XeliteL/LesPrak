package realty.model;

/** Статус объекта агентства. */
public enum ObjectStatus {
    ACTIVE("active"), RESERVED("reserved"), SOLD("sold");

    private final String code;

    ObjectStatus(String code) { this.code = code; }

    public static ObjectStatus byCode(String code) {
        for (ObjectStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("неизвестный статус объекта: " + code);
    }
}
