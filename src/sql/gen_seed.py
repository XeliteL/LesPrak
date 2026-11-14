"""Формирует seed.sql — условные данные для БД из src/python/data/sample.json.

Запуск из каталога src/sql: python gen_seed.py > seed.sql
Недостающие в JSON сведения (клиенты, сотрудники, договоры) создаются по простым правилам,
чтобы выгрузка из БД (export.sql) давала тот же набор данных, что и sample.json.
"""

import json
from pathlib import Path

SAMPLE = Path(__file__).resolve().parent.parent / "python" / "data" / "sample.json"
SOURCES = ("Циан", "Авито Недвижимость", "Домклик")
# сделка -> объект агентства, по договору которого она заключена
DEAL_PROPERTY = {1: 3, 2: 9, 3: 16, 4: 18, 5: 22, 6: 7}
SIGNED_ON = "2026-06-01"
DEFAULT_RATE = 0.03


def q(value):
    """Литерал SQL."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return repr(value)
    return "'" + str(value).replace("'", "''") + "'"


def insert(table, columns, rows):
    """INSERT с явными идентификаторами (OVERRIDING SYSTEM VALUE для столбцов IDENTITY)."""
    if not rows:
        return ""
    head = f"INSERT INTO {table} ({', '.join(columns)})"
    if columns[0] == "id":
        head += " OVERRIDING SYSTEM VALUE"
    values = ",\n".join("    (" + ", ".join(q(v) for v in r) + ")" for r in rows)
    return f"{head} VALUES\n{values};\n\n"


def main():
    data = json.loads(SAMPLE.read_text(encoding="utf-8"))
    out = ["-- Сформировано gen_seed.py из src/python/data/sample.json. Не редактировать вручную.\n",
           "SET search_path TO realty;\n\n"]

    names = sorted({x["district"] for x in data["listings"] + data["objects"]}
                   | {n for r in data["requests"] for n in r.get("districts", [])})
    district = {n: i for i, n in enumerate(names, 1)}
    out.append(insert("district", ["id", "name"], [(i, n) for n, i in district.items()]))

    out.append(insert("employee", ["id", "full_name", "position", "hired_on"], [
        (1, "Агент 1", "agent", "2023-03-01"),
        (2, "Агент 2", "agent", "2024-09-02"),
        (3, "Юрист", "lawyer", "2022-01-10"),
        (4, "Руководитель", "head", "2020-05-15"),
    ]))

    out.append(insert("document_type", ["code", "name", "required_if", "validity_days", "list_order"], [
        (t["code"], t["name"], t.get("required_if"), t.get("validity_days"), i)
        for i, t in enumerate(data["doc_types"], 1)]))

    # клиенты: продавец каждого объекта, покупатели по заявкам и по сделкам
    deal_by_property = {p: d for d, p in DEAL_PROPERTY.items()}
    deals = {d["id"]: d for d in data["deals"]}
    seller_married = {DEAL_PROPERTY[d["id"]] for d in data["deals"]
                      if d.get("conditions", {}).get("married_seller")}
    clients = [(o["id"], f"Продавец {o['id']}", o["id"] in seller_married) for o in data["objects"]]
    buyer_base = len(clients)
    request_client = {}
    for r in data["requests"]:
        cid = buyer_base + len(request_client) + 1
        request_client[r["id"]] = cid
        clients.append((cid, r.get("client", f"Покупатель {cid}"), False))
    deal_buyer = {}
    for d in data["deals"]:
        cid = buyer_base + len(request_client) + len(deal_buyer) + 1
        deal_buyer[d["id"]] = cid
        clients.append((cid, f"Покупатель по сделке {d['id']}", False))
    out.append(insert("client", ["id", "full_name", "married"], clients))

    out.append(insert("property", ["id", "district_id", "address", "rooms", "area", "floor", "floors", "year_built"], [
        (o["id"], district[o["district"]], f"ул. Условная, д. {o['id']}", o["rooms"], o["area"], o["floor"],
         o["floors"], o.get("year_built")) for o in data["objects"]]))
    if any(d.get("conditions", {}).get("minor_owners") for d in data["deals"]):
        raise SystemExit("minor_owners=true в sample.json: добавьте несовершеннолетнего собственника в gen_seed.py")
    out.append(insert("property_owner", ["property_id", "client_id", "share", "is_minor"],
                      [(o["id"], o["id"], 1, False) for o in data["objects"]]))

    contracts = []
    for o in data["objects"]:
        d = deals.get(deal_by_property.get(o["id"]))
        agent = d["agent_id"] if d else 1 + o["id"] % 2
        rate = d["rate"] if d else DEFAULT_RATE
        contracts.append((o["id"], o["id"], o["id"], agent, SIGNED_ON, "2027-05-31", o["price"], rate, o["status"]))
    out.append(insert("seller_contract", ["id", "property_id", "seller_id", "agent_id", "signed_on", "valid_until",
                                          "list_price", "commission_rate", "status"], contracts))

    out.append(insert("buyer_request", ["id", "client_id", "agent_id", "budget_min", "budget_max", "rooms_min",
                                        "rooms_max", "area_min", "not_first_floor", "not_last_floor"], [
        (r["id"], request_client[r["id"]], 1 + r["id"] % 2, r.get("budget_min"), r["budget_max"], min(r["rooms"]),
         max(r["rooms"]), r.get("area_min"), r.get("not_first_floor", False), r.get("not_last_floor", False))
        for r in data["requests"]]))
    for r in data["requests"]:
        if sorted(r["rooms"]) != list(range(min(r["rooms"]), max(r["rooms"]) + 1)):
            raise SystemExit(f"заявка {r['id']}: число комнат должно быть диапазоном")
    out.append(insert("request_district", ["request_id", "district_id", "priority"], [
        (r["id"], district[n], i) for r in data["requests"] for i, n in enumerate(r.get("districts", []), 1)]))

    out.append(insert("deal", ["id", "contract_id", "buyer_id", "price", "mortgage", "signing_on", "closed_on",
                               "status"], [
        (d["id"], DEAL_PROPERTY[d["id"]], deal_buyer[d["id"]], d["price"],
         d.get("conditions", {}).get("mortgage", False), d.get("signing_date"), d.get("closed"),
         d.get("status", "in_progress")) for d in data["deals"]]))
    out.append(insert("deal_stage", ["deal_id", "name", "due_on", "done_on"], [
        (d["id"], s["name"], s["due"], s["due"] if s.get("done") else None)
        for d in data["deals"] for s in d.get("stages", [])]))
    out.append(insert("document", ["deal_id", "type_code", "received_on"], [
        (d["id"], doc["type"], doc["received"]) for d in data["deals"] for doc in d.get("documents", [])]))

    out.append(insert("market_listing", ["id", "district_id", "source", "rooms", "area", "floor", "floors",
                                         "year_built", "price", "published_on"], [
        (x["id"], district[x["district"]], SOURCES[x["id"] % len(SOURCES)], x["rooms"], x["area"], x["floor"],
         x["floors"], x["year_built"], x["price"], x["date"]) for x in data["listings"]]))

    # после вставки с явными id сдвигаем последовательности столбцов IDENTITY
    out.append("""DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['district', 'client', 'employee', 'property', 'seller_contract', 'advert',
                             'buyer_request', 'viewing', 'deal', 'deal_stage', 'document', 'accrual',
                             'market_listing'] LOOP
        EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, ''id''), coalesce(max(id), 0) + 1, false) FROM %I',
                       'realty.' || t, t);
    END LOOP;
END $$;
""")
    print("".join(out), end="")


if __name__ == "__main__":
    main()
