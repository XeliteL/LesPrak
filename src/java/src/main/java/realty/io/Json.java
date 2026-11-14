package realty.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Минимальный разборщик JSON (RFC 8259) без сторонних библиотек.
 * Объект → {@code Map<String, Object>}, массив → {@code List<Object>}, целое число → {@code Long},
 * дробное → {@code Double}, строка → {@code String}, true/false → {@code Boolean}, null → null.
 */
public final class Json {
    private final String s;
    private int pos;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.skipSpace();
        Object value = p.value();
        p.skipSpace();
        if (p.pos != text.length()) {
            throw p.error("лишние символы после значения");
        }
        return value;
    }

    private Object value() {
        if (pos >= s.length()) {
            throw error("неожиданный конец текста");
        }
        char c = s.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipSpace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipSpace();
            if (peek() != '"') {
                throw error("ожидалось имя поля");
            }
            String key = string();
            skipSpace();
            expect(':');
            skipSpace();
            map.put(key, value());
            skipSpace();
            if (peek() == ',') {
                pos++;
            } else {
                expect('}');
                return map;
            }
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipSpace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipSpace();
            list.add(value());
            skipSpace();
            if (peek() == ',') {
                pos++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        pos++;
        while (true) {
            if (pos >= s.length()) {
                throw error("незакрытая строка");
            }
            char c = s.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = s.charAt(pos++);
            switch (e) {
                case '"', '\\', '/' -> sb.append(e);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw error("неизвестная escape-последовательность \\" + e);
            }
        }
    }

    private Object number() {
        int start = pos;
        boolean fraction = false;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
            fraction |= "eE.".indexOf(s.charAt(pos)) >= 0;
            pos++;
        }
        if (start == pos) {
            throw error("неожиданный символ «" + s.charAt(pos) + "»");
        }
        String token = s.substring(start, pos);
        try {
            return fraction ? (Object) Double.parseDouble(token) : (Object) Long.parseLong(token);
        } catch (NumberFormatException ex) {
            throw error("некорректное число " + token);
        }
    }

    private Object literal(String word, Object value) {
        if (!s.startsWith(word, pos)) {
            throw error("ожидалось " + word);
        }
        pos += word.length();
        return value;
    }

    private void skipSpace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        return pos < s.length() ? s.charAt(pos) : '\0';
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("ожидался символ «" + c + "»");
        }
        pos++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON, позиция " + pos + ": " + message);
    }
}
