# LesPrak

Материалы учебной практики (РТУ МИРЭА, 5 семестр, осень 2026).

**Тема:** Риелторские услуги по продаже жилой недвижимости.

## Структура

- `docs/stageN/` — тексты отчёта по этапам (Markdown) и собранный `Этап_N.docx`
- `diagrams/` — диаграммы draw.io (BPMN, ER, схемы алгоритмов), `diagrams/png/` — экспорт для отчёта
- `src/python/` — процедурная реализация алгоритмов (этап 3)
- `tools/build-docx/` — сборка отчёта из Markdown в DOCX
- `tools/diagrams/` — генерация диаграмм (`gen.py`) и экспорт в PNG (`export.sh`)

## Команды

```sh
# сборка docx этапа N
cd tools/build-docx && node build.js N

# диаграммы: генерация и экспорт в PNG (нужен drawio-desktop)
python tools/diagrams/gen.py && sh tools/diagrams/export.sh

# Python: демонстрация, тесты, замеры времени
cd src/python && python main.py && python -m unittest discover -s tests -v && python bench.py
```
