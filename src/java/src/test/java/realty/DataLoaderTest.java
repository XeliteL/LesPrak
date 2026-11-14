package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import realty.io.DataFormatException;
import realty.io.DataLoader;
import realty.io.DataSet;
import realty.io.Json;

class DataLoaderTest {

    @Test
    void jsonParsesNestedValues() {
        Map<?, ?> v = (Map<?, ?>) Json.parse("{\"a\": [1, 2.5, \"\\u0444\"], \"b\": {\"c\": true, \"d\": null}}");
        assertEquals(List.of(1L, 2.5, "ф"), v.get("a"));
        Map<?, ?> b = (Map<?, ?>) v.get("b");
        assertEquals(true, b.get("c"));
        assertEquals(true, b.containsKey("d"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\": 1,}"));
    }

    @Test
    void validateReportsMissingField() {
        DataFormatException e = assertThrows(DataFormatException.class,
                () -> DataLoader.fromJson("{\"deals\": [{\"id\": 1, \"agent_id\": 1, \"price\": 1}]}"));
        assertEquals(List.of("deals[0]: нет поля «rate»"), e.errors());
    }

    @Test
    void loadsSampleData() throws Exception {
        DataSet data = DataLoader.load(Path.of("../python/data/sample.json"));
        assertEquals(300, data.listings().size());
        assertEquals(100, data.objects().size());
        assertEquals(2, data.requests().size());
        assertEquals(9, data.docTypes().size());
        assertEquals(6, data.deals().size());
    }
}
