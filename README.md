# LesPrak

Материалы учебной практики (РТУ МИРЭА, 5 семестр, осень 2026).

**Тема:** Риелторские услуги по продаже жилой недвижимости.

## Структура

- `docs/stageN/` — тексты отчёта по этапам (Markdown) и собранный `Этап_N.docx`
- `diagrams/` — диаграммы draw.io (BPMN, ER, схемы алгоритмов), `diagrams/png/` — экспорт для отчёта
- `src/python/` — процедурная реализация алгоритмов (этап 3)
- `src/java/` — объектно-ориентированная реализация на Java, проект Maven (этап 4)
- `src/sql/` — физическая модель данных PostgreSQL, загрузка и выгрузка данных, тесты БД (этап 5)
- `tools/build-docx/` — сборка отчёта из Markdown в DOCX
- `tools/diagrams/` — генерация диаграмм (`gen.py`: BPMN, ER, физическая модель, схемы алгоритмов, UML-диаграммы классов) и экспорт в PNG (`export.sh`)

## Команды

```sh
# сборка docx этапа N
cd tools/build-docx && node build.js N

# диаграммы: генерация и экспорт в PNG (нужен drawio-desktop)
python tools/diagrams/gen.py && sh tools/diagrams/export.sh

# Python: демонстрация, тесты, замеры времени
cd src/python && python main.py && python -m unittest discover -s tests -v && python bench.py

# Java: сборка с тестами, демонстрация, замеры (mvn — из IntelliJ IDEA или отдельной установки Maven)
cd src/java && mvn package && java -jar target/realty-1.0.jar && java -cp target/classes realty.Bench

# PostgreSQL: создание схемы, загрузка данных, тесты, выгрузка в JSON для алгоритмов
cd src/sql && python gen_seed.py > seed.sql
createdb realty && for f in schema seed tests; do psql -d realty -v ON_ERROR_STOP=1 -f $f.sql; done
psql -d realty -At -f export.sql > /tmp/realty.json && (cd ../python && python main.py /tmp/realty.json)

# резервная копия БД (хранится вне репозитория)
pg_dump -Fc realty > realty.dump
```
