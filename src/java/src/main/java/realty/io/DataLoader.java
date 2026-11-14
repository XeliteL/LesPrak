package realty.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import realty.model.BuyerRequest;
import realty.model.Deal;
import realty.model.DealCondition;
import realty.model.DealStage;
import realty.model.DealStatus;
import realty.model.Document;
import realty.model.DocumentType;
import realty.model.MarketListing;
import realty.model.ObjectStatus;
import realty.model.PropertyObject;

/** Загрузка исходных данных из файла JSON того же формата, что и в процедурной реализации. */
public final class DataLoader {
    /** Обязательные поля записей каждого раздела. */
    static final Map<String, List<String>> REQUIRED_FIELDS = new LinkedHashMap<>();

    static {
        REQUIRED_FIELDS.put("listings", List.of("id", "district", "rooms", "area", "floor", "floors", "year_built",
                "price", "date"));
        REQUIRED_FIELDS.put("objects", List.of("id", "district", "rooms", "area", "floor", "floors", "price",
                "status"));
        REQUIRED_FIELDS.put("requests", List.of("id", "budget_max", "rooms"));
        REQUIRED_FIELDS.put("doc_types", List.of("code", "name"));
        REQUIRED_FIELDS.put("deals", List.of("id", "agent_id", "price", "rate"));
    }

    private DataLoader() {
    }

    public static DataSet load(Path path) throws IOException, DataFormatException {
        return fromJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static DataSet fromJson(String text) throws DataFormatException {
        Map<String, Object> root;
        try {
            root = (Map<String, Object>) Json.parse(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new DataFormatException(List.of(e.getMessage()));
        }
        List<String> errors = validate(root);
        if (!errors.isEmpty()) {
            throw new DataFormatException(errors);
        }
        try {
            return new DataSet(
                    section(root, "listings", DataLoader::listing),
                    section(root, "objects", DataLoader::object),
                    section(root, "requests", DataLoader::request),
                    section(root, "doc_types", DataLoader::docType),
                    section(root, "deals", DataLoader::deal));
        } catch (RuntimeException e) {
            throw new DataFormatException(List.of(e.getMessage()));
        }
    }

    /** Проверка наличия обязательных полей; возвращает список ошибок (пустой, если ошибок нет). */
    @SuppressWarnings("unchecked")
    public static List<String> validate(Map<String, Object> root) {
        List<String> errors = new ArrayList<>();
        REQUIRED_FIELDS.forEach((section, fields) -> {
            List<Map<String, Object>> items = (List<Map<String, Object>>) root.getOrDefault(section, List.of());
            for (int i = 0; i < items.size(); i++) {
                for (String field : fields) {
                    if (!items.get(i).containsKey(field)) {
                        errors.add(section + "[" + i + "]: нет поля «" + field + "»");
                    }
                }
            }
        });
        return errors;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> section(Map<String, Object> root, String name, Function<Rec, T> mapper) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) root.getOrDefault(name, List.of());
        return items.stream().map(m -> mapper.apply(new Rec(m))).toList();
    }

    private static MarketListing listing(Rec r) {
        return new MarketListing(r.integer("id"), r.str("district"), r.integer("rooms"), r.dbl("area"),
                r.integer("floor"), r.integer("floors"), r.integer("year_built"), r.lng("price"), r.date("date"));
    }

    private static PropertyObject object(Rec r) {
        return new PropertyObject(r.integer("id"), r.str("district"), r.integer("rooms"), r.dbl("area"),
                r.integer("floor"), r.integer("floors"), r.optInt("year_built", 0), r.lng("price"),
                ObjectStatus.byCode(r.str("status")));
    }

    private static BuyerRequest request(Rec r) {
        Set<Integer> rooms = r.list("rooms").stream().map(x -> ((Number) x).intValue()).collect(Collectors.toSet());
        List<String> districts = r.list("districts").stream().map(String.class::cast).toList();
        return new BuyerRequest(r.integer("id"), r.optStr("client", ""), r.optLng("budget_min", 0),
                r.lng("budget_max"), rooms, districts, r.optDbl("area_min", 0), r.flag("not_first_floor"),
                r.flag("not_last_floor"));
    }

    private static DocumentType docType(Rec r) {
        String condition = r.optStr("required_if", null);
        return new DocumentType(r.str("code"), r.str("name"),
                condition == null ? null : DealCondition.byCode(condition),
                r.has("validity_days") ? r.integer("validity_days") : null);
    }

    @SuppressWarnings("unchecked")
    private static Deal deal(Rec r) {
        Deal.Builder b = Deal.builder(r.integer("id"), r.integer("agent_id"), r.lng("price"), r.dbl("rate"))
                .status(DealStatus.byCode(r.optStr("status", "in_progress")));
        if (r.has("closed")) {
            b.closed(r.date("closed"));
        }
        if (r.has("signing_date")) {
            b.signingDate(r.date("signing_date"));
        }
        Map<String, Object> conditions = (Map<String, Object>) r.m.getOrDefault("conditions", Map.of());
        conditions.forEach((code, value) -> {
            if (Boolean.TRUE.equals(value)) {
                b.condition(DealCondition.byCode(code));
            }
        });
        b.stages(r.list("stages").stream().map(x -> new Rec((Map<String, Object>) x))
                .map(s -> new DealStage(s.str("name"), s.date("due"), s.flag("done"))).toList());
        b.documents(r.list("documents").stream().map(x -> new Rec((Map<String, Object>) x))
                .map(d -> new Document(d.str("type"), d.date("received"))).toList());
        return b.build();
    }

    /** Обёртка над записью JSON с типизированным доступом к полям. */
    private record Rec(Map<String, Object> m) {
        boolean has(String key) { return m.get(key) != null; }
        String str(String key) { return (String) m.get(key); }
        String optStr(String key, String def) { return has(key) ? str(key) : def; }
        int integer(String key) { return ((Number) m.get(key)).intValue(); }
        int optInt(String key, int def) { return has(key) ? integer(key) : def; }
        long lng(String key) { return ((Number) m.get(key)).longValue(); }
        long optLng(String key, long def) { return has(key) ? lng(key) : def; }
        double dbl(String key) { return ((Number) m.get(key)).doubleValue(); }
        double optDbl(String key, double def) { return has(key) ? dbl(key) : def; }
        boolean flag(String key) { return Boolean.TRUE.equals(m.get(key)); }
        LocalDate date(String key) { return LocalDate.parse(str(key)); }
        @SuppressWarnings("unchecked")
        List<Object> list(String key) { return has(key) ? (List<Object>) m.get(key) : List.of(); }
    }
}
