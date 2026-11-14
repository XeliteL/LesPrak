"""Алгоритм 2. Подбор объектов под заявку покупателя.

Шаги:
1. Жёсткие условия: объект в продаже, цена в пределах бюджета (с допуском на торг),
   подходящее число комнат, район, площадь и этаж.
2. Оценка соответствия (0–100 баллов) по цене, площади и району.
3. Выбор top_n объектов с наибольшей оценкой (куча размера top_n).

Сложность: O(n log N), где n — число объектов, N = top_n; при N << n близка к O(n).
Память O(N).
"""

import heapq

from realty.data import floor_category

BUDGET_TOLERANCE = 0.05   # объект может быть дороже бюджета на эту долю (резерв на торг)
WEIGHTS = {"price": 50, "area": 30, "district": 20}


def passes_filters(request, obj):
    """Шаг 1: проверяет жёсткие условия заявки."""
    if obj["status"] != "active":
        return False
    if obj["price"] > request["budget_max"] * (1 + BUDGET_TOLERANCE):
        return False
    if obj["price"] < (request.get("budget_min") or 0):
        return False
    if obj["rooms"] not in request["rooms"]:
        return False
    districts = request.get("districts") or []
    if districts and obj["district"] not in districts:
        return False
    if obj["area"] < (request.get("area_min") or 0):
        return False
    category = floor_category(obj["floor"], obj["floors"])
    if request.get("not_first_floor") and category == "first":
        return False
    if request.get("not_last_floor") and category == "last":
        return False
    return True


def match_score(request, obj):
    """Шаг 2: оценка соответствия объекта заявке от 0 до 100."""
    budget = request["budget_max"]
    # цена: максимум баллов, если объект укладывается в бюджет; дороже — линейное снижение
    if obj["price"] <= budget:
        price_part = 1.0
    else:
        price_part = 1 - (obj["price"] - budget) / (budget * BUDGET_TOLERANCE)
    # площадь: запас площади над минимальной до 30 % даёт до полного балла
    area_min = request.get("area_min") or 0
    if area_min:
        area_part = min(1.0, (obj["area"] - area_min) / (area_min * 0.3))
    else:
        area_part = 1.0
    # район: первый в списке предпочтений — полный балл, далее по убыванию
    districts = request.get("districts") or []
    if districts:
        district_part = 1 - districts.index(obj["district"]) / len(districts)
    else:
        district_part = 1.0
    score = (WEIGHTS["price"] * price_part + WEIGHTS["area"] * area_part
             + WEIGHTS["district"] * district_part)
    return round(score, 1)


def match_objects(request, objects, top_n=10):
    """Возвращает до top_n пар (оценка, объект), отсортированных по убыванию оценки."""
    heap = []  # минимальная куча из (оценка, -id, объект)
    for obj in objects:
        if not passes_filters(request, obj):
            continue
        item = (match_score(request, obj), -obj["id"], obj)
        if len(heap) < top_n:
            heapq.heappush(heap, item)
        elif item[:2] > heap[0][:2]:
            heapq.heapreplace(heap, item)
    ranked = sorted(heap, key=lambda x: (x[0], x[1]), reverse=True)
    return [(score, obj) for score, _, obj in ranked]
