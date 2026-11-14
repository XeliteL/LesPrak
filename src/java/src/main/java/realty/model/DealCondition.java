package realty.model;

/** Условие сделки, от которого зависит перечень обязательных документов. */
public enum DealCondition {
    MORTGAGE("mortgage"), MARRIED_SELLER("married_seller"), MINOR_OWNERS("minor_owners");

    private final String code;

    DealCondition(String code) { this.code = code; }

    public String code() { return code; }

    public static DealCondition byCode(String code) {
        for (DealCondition c : values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }
        throw new IllegalArgumentException("неизвестное условие сделки: " + code);
    }
}
