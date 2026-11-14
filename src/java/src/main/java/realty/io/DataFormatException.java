package realty.io;

import java.util.List;

/** Ошибки в исходных данных: перечень всех найденных нарушений. */
public final class DataFormatException extends Exception {
    private final List<String> errors;

    public DataFormatException(List<String> errors) {
        super("ошибки в данных: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
