package realty.io;

import java.util.Locale;

/** Форматирование денежных сумм: 12 500 000 руб. */
public final class Rub {
    private Rub() {
    }

    public static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ') + " руб.";
    }
}
