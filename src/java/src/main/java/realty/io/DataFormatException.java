package realty.io;

import java.util.List;

/** Ошибки в исходных данных: перечень всех найденных нарушений. */
public final class DataFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    // массив строк сериализуем, в отличие от неизменяемого списка List.copyOf
    private final String[] errors;

    public DataFormatException(List<String> errors) {
        super("ошибки в данных: " + String.join("; ", errors));
        this.errors = errors.toArray(String[]::new);
    }

    public List<String> errors() {
        return List.of(errors);
    }
}
