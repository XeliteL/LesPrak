"""Экспериментальная проверка оценок сложности: время работы алгоритмов при росте объёма данных.

Запуск из src/python: python bench.py
Выводит таблицу в формате Markdown (медиана из REPEATS запусков, мс).
"""

import random
import time
from datetime import date, timedelta
from statistics import median

from gen_data import DISTRICTS, make_flat
from realty.commission import monthly_report
from realty.documents import check_deal
from realty.matching import match_objects
from realty.valuation import estimate_price

SIZES = [1_000, 10_000, 100_000, 1_000_000]
REPEATS = 5
TODAY = date(2026, 11, 1)


def timed(fn):
    times = []
    for _ in range(REPEATS):
        t0 = time.perf_counter()
        fn()
        times.append(time.perf_counter() - t0)
    return median(times) * 1000


def flats(n, rng):
    result = []
    for i in range(1, n + 1):
        f = make_flat(rng, i)
        f["date"] = (TODAY - timedelta(days=rng.randint(0, 365))).isoformat()
        f["status"] = "active"
        result.append(f)
    return result


def main():
    rng = random.Random(1)
    subject = {"district": DISTRICTS[1], "rooms": 2, "area": 54.0, "floor": 7, "floors": 17, "year_built": 2005}
    request = {"budget_max": 16_000_000, "rooms": [2, 3], "districts": DISTRICTS[:3], "area_min": 45}
    print("| n | Алгоритм 1, мс | Алгоритм 2, мс | Алгоритм 3, мс | Алгоритм 4, мс |")
    print("|---|---|---|---|---|")
    for n in SIZES:
        data = flats(n, rng)
        doc_types = [{"code": f"t{i}", "name": f"Тип {i}", "validity_days": 30} for i in range(n // 100)]
        docs = [{"type": f"t{rng.randrange(n // 100)}", "received": "2026-10-20"} for _ in range(n)]
        deal = {"price": 9_000_000, "signing_date": "2026-11-20", "conditions": {}, "stages": []}
        deals = [{"id": i, "agent_id": rng.randrange(50), "price": rng.randint(3, 30) * 1_000_000,
                  "rate": 0.03, "status": "closed",
                  "closed": (date(2026, 10, 1) + timedelta(days=rng.randrange(31))).isoformat()}
                 for i in range(n)]
        t1 = timed(lambda: estimate_price(subject, data, TODAY))
        t2 = timed(lambda: match_objects(request, data, top_n=10))
        t3 = timed(lambda: check_deal(deal, doc_types, docs, TODAY))
        t4 = timed(lambda: monthly_report(deals, 2026, 10))
        print(f"| {n:,} | {t1:.1f} | {t2:.1f} | {t3:.1f} | {t4:.1f} |".replace(",", " "))


if __name__ == "__main__":
    main()
