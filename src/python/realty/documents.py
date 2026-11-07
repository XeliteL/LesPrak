"""Алгоритм 3. Контроль комплектности документов и сроков сделки.

Шаги:
1. Формирование перечня обязательных документов по условиям сделки
   (ипотека, несовершеннолетние собственники, супруг продавца и т. п.).
2. Индексация полученных документов по типу (словарь, последний по дате документ).
3. Проверка каждого обязательного документа: отсутствует, просрочен к дате подписания,
   истекает в ближайшие WARN_DAYS дней.
4. Проверка порога обязательного контроля по 115-ФЗ и просроченных этапов сделки.

Сложность: O(t + d + s), где t — число типов документов, d — число полученных документов,
s — число этапов сделки. Память O(t + d).
"""

from datetime import timedelta

from realty.data import parse_date

WARN_DAYS = 7                      # предупреждать за столько дней до истечения срока
CONTROL_THRESHOLD = 5_000_000      # порог обязательного контроля, руб. (п. 1.1 ст. 6 115-ФЗ)


def required_types(deal, doc_types):
    """Шаг 1: типы документов, обязательные для данной сделки."""
    result = []
    for t in doc_types:
        condition = t.get("required_if")
        if condition is None or deal.get("conditions", {}).get(condition):
            result.append(t)
    return result


def index_documents(documents):
    """Шаг 2: словарь «код типа → самый поздний полученный документ этого типа»."""
    index = {}
    for doc in documents:
        code = doc["type"]
        if code not in index or parse_date(doc["received"]) > parse_date(index[code]["received"]):
            index[code] = doc
    return index


def check_deal(deal, doc_types, documents, today):
    """Проверяет готовность сделки к подписанию. Возвращает словарь с результатом."""
    today = parse_date(today)
    signing = parse_date(deal["signing_date"])
    index = index_documents(documents)

    missing, expired, expiring = [], [], []
    for t in required_types(deal, doc_types):
        doc = index.get(t["code"])
        if doc is None:
            missing.append(t["name"])
            continue
        days = t.get("validity_days")
        if days is None:
            continue
        valid_until = parse_date(doc["received"]) + timedelta(days=days)
        if valid_until < signing:
            expired.append(t["name"])
        elif (valid_until - today).days <= WARN_DAYS:
            expiring.append(t["name"])

    overdue_stages = [
        s["name"] for s in deal.get("stages", [])
        if not s.get("done") and parse_date(s["due"]) < today
    ]
    return {
        "ready": not missing and not expired,
        "missing": missing,
        "expired": expired,
        "expiring": expiring,
        "overdue_stages": overdue_stages,
        "aml_control": deal["price"] >= CONTROL_THRESHOLD,
    }
