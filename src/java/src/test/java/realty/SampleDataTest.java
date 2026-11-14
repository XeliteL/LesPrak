package realty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Сквозная проверка: вывод программы на демонстрационных данных совпадает с эталонным выводом
 * процедурной реализации (файл получен командой python main.py > ../java/src/test/resources/expected.txt).
 */
class SampleDataTest {

    @Test
    void outputMatchesProceduralImplementation() throws Exception {
        String expected = Files.readString(Path.of("src/test/resources/expected.txt"), StandardCharsets.UTF_8);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        assertEquals(expected, buf.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n"));
    }
}
