"""Генерация условных (синтетических) исходных данных для демонстрации и замеров.

Запуск: python gen_data.py [число объявлений] > data/sample.json
Генератор детерминирован (фиксированное зерно), поэтому результат воспроизводим.
"""

import json
import random
import sys
from datetime import date, timedelta

DISTRICTS = ["Тропарёво-Никулино", "Раменки", "Хорошёво-Мнёвники", "Марьино", "Бутово Северное"]
BASE_PRICE_M2 = {  # условная базовая цена 1 м², руб.
    "Тропарёво-Никулино": 330_000, "Раменки": 380_000, "Хорошёво-Мнёвники": 350_000,
    "Марьино": 270_000, "Бутово Северное": 250_000,
}
TODAY = date(2026, 11, 1)


def make_flat(rng, idx):
    district = rng.choice(DISTRICTS)
    rooms = rng.choice([1, 1, 2, 2, 2, 3, 3, 4])
    area = round(rng.uniform(20 + rooms * 12, 30 + rooms * 18), 1)
    floors = rng.choice([5, 9, 12, 14, 17, 22, 25])
    floor = rng.randint(1, floors)
    year = rng.randint(1960, 2024)
    price_m2 = BASE_PRICE_M2[district] * rng.uniform(0.85, 1.15)
    if floor == 1:
        price_m2 *= 0.95
    return {
        "id": idx, "district": district, "rooms": rooms, "area": area,
        "floor": floor, "floors": floors, "year_built": year,
        "price": round(price_m2 * area / 10_000) * 10_000,
    }


def generate(n_listings=300, seed=2026):
    rng = random.Random(seed)
    listings = []
    for i in range(1, n_listings + 1):
        f = make_flat(rng, i)
        f["date"] = (TODAY - timedelta(days=rng.randint(0, 365))).isoformat()
        listings.append(f)

    objects = []
    for i in range(1, n_listings // 3 + 1):
        f = make_flat(rng, i)
        f["status"] = rng.choice(["active", "active", "active", "reserved", "sold"])
        objects.append(f)

    requests = [
        {"id": 1, "client": "Покупатель 1", "budget_min": 10_000_000, "budget_max": 16_000_000,
         "rooms": [2], "districts": ["Раменки", "Тропарёво-Никулино"], "area_min": 45,
         "not_first_floor": True, "not_last_floor": False},
        {"id": 2, "client": "Покупатель 2", "budget_max": 12_000_000, "rooms": [1, 2],
         "districts": [], "area_min": 35, "not_first_floor": True, "not_last_floor": True},
    ]

    doc_types = [
        {"code": "egrn", "name": "Выписка из ЕГРН", "validity_days": 30},
        {"code": "title", "name": "Правоустанавливающий документ"},
        {"code": "passport", "name": "Паспорт продавца"},
        {"code": "registered", "name": "Сведения о зарегистрированных лицах", "validity_days": 30},
        {"code": "no_debt", "name": "Справка об отсутствии задолженности по ЖКУ", "validity_days": 30},
        {"code": "spouse", "name": "Нотариальное согласие супруга", "required_if": "married_seller"},
        {"code": "guardianship", "name": "Разрешение органа опеки", "required_if": "minor_owners"},
        {"code": "bank_approval", "name": "Решение банка об одобрении объекта", "required_if": "mortgage",
         "validity_days": 90},
        {"code": "appraisal", "name": "Отчёт об оценке", "required_if": "mortgage", "validity_days": 180},
    ]

    deals = [
        {"id": 1, "agent_id": 1, "price": 14_500_000, "rate": 0.03, "status": "closed", "closed": "2026-10-05"},
        {"id": 2, "agent_id": 1, "price": 9_800_000, "rate": 0.03, "status": "closed", "closed": "2026-10-17"},
        {"id": 3, "agent_id": 2, "price": 3_200_000, "rate": 0.025, "status": "closed", "closed": "2026-10-20"},
        {"id": 4, "agent_id": 1, "price": 21_000_000, "rate": 0.025, "status": "closed", "closed": "2026-10-28"},
        {"id": 5, "agent_id": 2, "price": 12_300_000, "rate": 0.03, "status": "closed", "closed": "2026-11-02"},
        {
            "id": 6, "agent_id": 2, "price": 12_300_000, "rate": 0.03, "status": "in_progress",
            "signing_date": "2026-11-20",
            "conditions": {"mortgage": True, "married_seller": True, "minor_owners": False},
            "stages": [
                {"name": "Внесение аванса", "due": "2026-10-25", "done": True},
                {"name": "Одобрение объекта банком", "due": "2026-11-05", "done": False},
                {"name": "Подписание ДКП", "due": "2026-11-20", "done": False},
            ],
            "documents": [
                {"type": "egrn", "received": "2026-10-10"},
                {"type": "title", "received": "2026-10-03"},
                {"type": "passport", "received": "2026-10-03"},
                {"type": "registered", "received": "2026-10-28"},
                {"type": "no_debt", "received": "2026-10-28"},
                {"type": "appraisal", "received": "2026-10-30"},
            ],
        },
    ]
    return {"listings": listings, "objects": objects, "requests": requests,
            "doc_types": doc_types, "deals": deals}


if __name__ == "__main__":
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 300
    json.dump(generate(n), sys.stdout, ensure_ascii=False, indent=1)
