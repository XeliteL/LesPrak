"""Алгоритм 1. Оценка рыночной стоимости квартиры методом сравнения с объектами-аналогами.

Шаги:
1. Отбор аналогов: тот же район и число комнат, площадь отличается не более чем на
   AREA_TOLERANCE, объявление не старше MAX_AGE_DAYS дней.
2. Корректировка цены 1 м² каждого аналога: скидка на торг и поправка на этаж.
3. Исключение выбросов: аналоги, удельная цена которых отклоняется от медианы
   больше чем на OUTLIER_TOLERANCE.
4. Средневзвешенная удельная цена (вес тем больше, чем аналог ближе к оцениваемой квартире),
   рекомендуемая цена и диапазон.

Сложность: O(n) на отбор и корректировку, O(k log k) на поиск медианы (k — число аналогов),
в худшем случае k = n, итого O(n log n). Память O(k).
"""

from statistics import median

from realty.data import floor_category, parse_date

AREA_TOLERANCE = 0.25      # допустимое отклонение площади аналога, доля
MAX_AGE_DAYS = 180         # максимальный возраст объявления, дней
BARGAIN_DISCOUNT = 0.03    # скидка на торг от цены предложения, доля
OUTLIER_TOLERANCE = 0.25   # допустимое отклонение удельной цены от медианы, доля
MIN_ANALOGS = 3            # минимальное число аналогов для оценки
ROUND_TO = 10_000          # округление рекомендуемой цены, руб.

# Коэффициенты к удельной цене по категории этажа (средний этаж = 1). Условные значения,
# задаются агентством в настройках системы.
FLOOR_COEF = {"first": 0.95, "middle": 1.0, "last": 0.97}


def select_analogs(subject, listings, today):
    """Шаг 1: отбирает объявления, сопоставимые с оцениваемой квартирой."""
    today = parse_date(today)
    result = []
    for item in listings:
        if item["district"] != subject["district"] or item["rooms"] != subject["rooms"]:
            continue
        if abs(item["area"] - subject["area"]) / subject["area"] > AREA_TOLERANCE:
            continue
        if (today - parse_date(item["date"])).days > MAX_AGE_DAYS:
            continue
        result.append(item)
    return result


def adjusted_unit_price(subject, analog):
    """Шаг 2: удельная цена аналога (руб./м²), приведённая к условиям оцениваемой квартиры."""
    unit = analog["price"] / analog["area"] * (1 - BARGAIN_DISCOUNT)
    k_subject = FLOOR_COEF[floor_category(subject["floor"], subject["floors"])]
    k_analog = FLOOR_COEF[floor_category(analog["floor"], analog["floors"])]
    return unit * k_subject / k_analog


def similarity_weight(subject, analog):
    """Вес аналога: 1 / (1 + d), где d — относительное различие по площади и году постройки."""
    d = abs(analog["area"] - subject["area"]) / subject["area"]
    d += abs(analog["year_built"] - subject["year_built"]) / 50
    return 1 / (1 + d)


def estimate_price(subject, listings, today):
    """Оценивает квартиру. Возвращает словарь с результатом.

    Поле status: 'ok' — оценка выполнена; 'insufficient' — аналогов меньше MIN_ANALOGS.
    """
    analogs = select_analogs(subject, listings, today)
    if len(analogs) < MIN_ANALOGS:
        return {"status": "insufficient", "analogs_found": len(analogs), "analogs_used": []}

    adjusted = [(a, adjusted_unit_price(subject, a)) for a in analogs]
    med = median(u for _, u in adjusted)
    kept = [(a, u) for a, u in adjusted if abs(u - med) / med <= OUTLIER_TOLERANCE]
    if len(kept) < MIN_ANALOGS:
        return {"status": "insufficient", "analogs_found": len(analogs), "analogs_used": []}

    total_w = 0.0
    total = 0.0
    for a, u in kept:
        w = similarity_weight(subject, a)
        total_w += w
        total += w * u
    unit_price = total / total_w
    price = round(unit_price * subject["area"] / ROUND_TO) * ROUND_TO
    units = [u for _, u in kept]
    return {
        "status": "ok",
        "unit_price": round(unit_price),
        "price": price,
        "price_min": round(min(units) * subject["area"] / ROUND_TO) * ROUND_TO,
        "price_max": round(max(units) * subject["area"] / ROUND_TO) * ROUND_TO,
        "analogs_found": len(analogs),
        "analogs_used": [a["id"] for a, _ in kept],
    }
