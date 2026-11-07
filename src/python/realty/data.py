"""Загрузка и проверка исходных данных."""

import json
from datetime import date

REQUIRED_FIELDS = {
    "listings": ("id", "district", "rooms", "area", "floor", "floors", "year_built", "price", "date"),
    "objects": ("id", "district", "rooms", "area", "floor", "floors", "price", "status"),
    "requests": ("id", "budget_max", "rooms"),
    "doc_types": ("code", "name"),
    "deals": ("id", "agent_id", "price", "rate"),
}


def parse_date(value):
    """Преобразует строку ГГГГ-ММ-ДД в дату; дату возвращает без изменений."""
    if isinstance(value, date):
        return value
    return date.fromisoformat(value)


def load_json(path):
    """Читает JSON-файл с исходными данными."""
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def validate(data):
    """Проверяет наличие обязательных полей. Возвращает список ошибок (пустой, если ошибок нет)."""
    errors = []
    for section, fields in REQUIRED_FIELDS.items():
        for i, item in enumerate(data.get(section, [])):
            for field in fields:
                if field not in item:
                    errors.append(f"{section}[{i}]: нет поля «{field}»")
    return errors


def floor_category(floor, floors):
    """Категория этажа: 'first' — первый, 'last' — последний, 'middle' — средний."""
    if floor <= 1:
        return "first"
    if floor >= floors:
        return "last"
    return "middle"


def format_rub(value):
    """Форматирует сумму в рублях с разделением разрядов пробелом: 12 500 000 руб."""
    return f"{round(value):,}".replace(",", " ") + " руб."
