"""Демонстрация работы алгоритмов на условных данных.

Запуск из каталога src/python: python main.py [путь к JSON]
"""

import sys

from realty.commission import monthly_report
from realty.data import format_rub, load_json, validate
from realty.documents import check_deal
from realty.matching import match_objects
from realty.valuation import estimate_price

TODAY = "2026-11-07"


def show_valuation(data):
    print("=== Алгоритм 1. Оценка стоимости квартиры")
    subject = {"district": "Раменки", "rooms": 2, "area": 54.0, "floor": 7, "floors": 17, "year_built": 2005}
    print(f"Объект: {subject['district']}, {subject['rooms']}-комн., {subject['area']} м², "
          f"этаж {subject['floor']}/{subject['floors']}, {subject['year_built']} г.")
    r = estimate_price(subject, data["listings"], TODAY)
    if r["status"] != "ok":
        print(f"Недостаточно аналогов: найдено {r['analogs_found']}")
        return
    print(f"Аналогов найдено: {r['analogs_found']}, использовано: {len(r['analogs_used'])}")
    print(f"Цена 1 м²: {format_rub(r['unit_price'])}")
    print(f"Рекомендуемая цена: {format_rub(r['price'])} "
          f"(диапазон {format_rub(r['price_min'])} — {format_rub(r['price_max'])})")


def show_matching(data):
    print("\n=== Алгоритм 2. Подбор объектов под заявку покупателя")
    for req in data["requests"]:
        print(f"Заявка {req['id']}: бюджет до {format_rub(req['budget_max'])}, комнат {req['rooms']}, "
              f"районы: {', '.join(req.get('districts') or ['любой'])}")
        for score, obj in match_objects(req, data["objects"], top_n=5):
            print(f"  {score:5.1f}  объект {obj['id']:3}: {obj['district']}, {obj['rooms']}-комн., "
                  f"{obj['area']} м², эт. {obj['floor']}/{obj['floors']}, {format_rub(obj['price'])}")


def show_documents(data):
    print("\n=== Алгоритм 3. Контроль документов и сроков сделки")
    for deal in data["deals"]:
        if deal.get("status") != "in_progress":
            continue
        r = check_deal(deal, data["doc_types"], deal["documents"], TODAY)
        print(f"Сделка {deal['id']}, подписание {deal.get('signing_date') or 'не назначено'}: "
              f"{'готова' if r['ready'] else 'не готова'} к подписанию")
        for title, key in (("Не хватает", "missing"), ("Просрочены к подписанию", "expired"),
                           ("Скоро истекают", "expiring"), ("Просрочены этапы", "overdue_stages")):
            if r[key]:
                print(f"  {title}: {'; '.join(r[key])}")
        if r["aml_control"]:
            print("  Сделка подлежит обязательному контролю (115-ФЗ): направить сведения в Росфинмониторинг")


def show_commission(data):
    print("\n=== Алгоритм 4. Комиссия за октябрь 2026")
    r = monthly_report(data["deals"], 2026, 10)
    for row in r["rows"]:
        print(f"  сделка {row['deal_id']}, агент {row['agent_id']}: комиссия {format_rub(row['fee'])}, "
              f"агенту {format_rub(row['agent_share'])}")
    for agent_id, t in r["agents"].items():
        print(f"Агент {agent_id}: комиссия {format_rub(t['fee'])}, вознаграждение {format_rub(t['agent_share'])}")
    print(f"Итого комиссия: {format_rub(r['total_fee'])}, доход агентства: {format_rub(r['agency_income'])}")


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "data/sample.json"
    data = load_json(path)
    errors = validate(data)
    if errors:
        print("Ошибки в данных:", *errors, sep="\n  ")
        sys.exit(1)
    show_valuation(data)
    show_matching(data)
    show_documents(data)
    show_commission(data)


if __name__ == "__main__":
    main()
