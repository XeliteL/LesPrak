"""Модульные тесты алгоритмов. Запуск из src/python: python -m unittest discover -s tests -v"""

import unittest

from realty.commission import agency_fee, agent_share, monthly_report
from realty.data import floor_category, validate
from realty.documents import check_deal, index_documents, required_types
from realty.matching import match_objects, passes_filters
from realty.valuation import adjusted_unit_price, estimate_price, select_analogs

TODAY = "2026-11-07"


def flat(id_, price, area=50.0, floor=5, floors=10, district="Раменки", rooms=2, year=2000, date="2026-10-01"):
    return {"id": id_, "district": district, "rooms": rooms, "area": area, "floor": floor,
            "floors": floors, "year_built": year, "price": price, "date": date}


SUBJECT = {"district": "Раменки", "rooms": 2, "area": 50.0, "floor": 5, "floors": 10, "year_built": 2000}


class TestData(unittest.TestCase):
    def test_floor_category(self):
        self.assertEqual(floor_category(1, 9), "first")
        self.assertEqual(floor_category(9, 9), "last")
        self.assertEqual(floor_category(5, 9), "middle")

    def test_validate_reports_missing_field(self):
        errors = validate({"deals": [{"id": 1, "agent_id": 1, "price": 1}]})
        self.assertEqual(errors, ["deals[0]: нет поля «rate»"])


class TestValuation(unittest.TestCase):
    def test_select_analogs_filters(self):
        listings = [
            flat(1, 10_000_000),
            flat(2, 10_000_000, district="Марьино"),         # другой район
            flat(3, 10_000_000, rooms=3),                    # другое число комнат
            flat(4, 10_000_000, area=70.0),                  # площадь +40 %
            flat(5, 10_000_000, date="2026-01-01"),          # старше 180 дней
        ]
        self.assertEqual([a["id"] for a in select_analogs(SUBJECT, listings, TODAY)], [1])

    def test_floor_adjustment(self):
        middle = adjusted_unit_price(SUBJECT, flat(1, 10_000_000))
        first = adjusted_unit_price(SUBJECT, flat(2, 10_000_000, floor=1))
        self.assertAlmostEqual(middle, 10_000_000 / 50 * 0.97)
        self.assertGreater(first, middle)  # аналог на первом этаже приводится к среднему вверх

    def test_identical_analogs_give_their_price(self):
        listings = [flat(i, 10_000_000) for i in range(1, 6)]
        r = estimate_price(SUBJECT, listings, TODAY)
        self.assertEqual(r["status"], "ok")
        self.assertEqual(r["price"], 9_700_000)  # минус 3 % на торг
        self.assertEqual(r["price_min"], r["price_max"])

    def test_outlier_excluded(self):
        listings = [flat(i, 10_000_000) for i in range(1, 5)] + [flat(9, 30_000_000)]
        r = estimate_price(SUBJECT, listings, TODAY)
        self.assertNotIn(9, r["analogs_used"])
        self.assertEqual(r["price"], 9_700_000)

    def test_insufficient_analogs(self):
        r = estimate_price(SUBJECT, [flat(1, 10_000_000), flat(2, 10_000_000)], TODAY)
        self.assertEqual(r["status"], "insufficient")
        self.assertEqual(r["analogs_found"], 2)

    def test_empty_listings(self):
        self.assertEqual(estimate_price(SUBJECT, [], TODAY)["status"], "insufficient")


class TestMatching(unittest.TestCase):
    REQ = {"budget_max": 10_000_000, "rooms": [2], "districts": ["Раменки", "Марьино"],
           "area_min": 40, "not_first_floor": True}

    def obj(self, id_, price, **kw):
        o = flat(id_, price, **kw)
        o["status"] = kw.get("status", "active")
        return o

    def test_filters(self):
        self.assertTrue(passes_filters(self.REQ, self.obj(1, 10_400_000)))       # в пределах допуска 5 %
        self.assertFalse(passes_filters(self.REQ, self.obj(2, 10_600_000)))      # дороже допуска
        self.assertFalse(passes_filters(self.REQ, self.obj(3, 9_000_000, floor=1)))
        self.assertFalse(passes_filters(self.REQ, self.obj(4, 9_000_000, district="Бутово Северное")))
        sold = self.obj(5, 9_000_000)
        sold["status"] = "sold"
        self.assertFalse(passes_filters(self.REQ, sold))

    def test_ranking_and_top_n(self):
        objects = [self.obj(1, 9_000_000, area=52.0),                       # лучший: в бюджете, площадь с запасом
                   self.obj(2, 9_000_000, area=52.0, district="Марьино"),   # второй район в списке
                   self.obj(3, 10_400_000, area=52.0),                       # дороже бюджета
                   self.obj(4, 9_000_000, area=30.0)]                        # не проходит по площади
        result = match_objects(self.REQ, objects, top_n=2)
        self.assertEqual([o["id"] for _, o in result], [1, 2])
        self.assertEqual(result[0][0], 100.0)

    def test_no_matches(self):
        self.assertEqual(match_objects(self.REQ, [], top_n=5), [])


class TestDocuments(unittest.TestCase):
    TYPES = [
        {"code": "egrn", "name": "Выписка из ЕГРН", "validity_days": 30},
        {"code": "passport", "name": "Паспорт продавца"},
        {"code": "bank", "name": "Одобрение банка", "required_if": "mortgage"},
    ]

    def deal(self, mortgage=False, price=4_000_000, stages=()):
        return {"id": 1, "price": price, "signing_date": "2026-11-20",
                "conditions": {"mortgage": mortgage}, "stages": list(stages)}

    def test_required_types_depend_on_conditions(self):
        self.assertEqual(len(required_types(self.deal(), self.TYPES)), 2)
        self.assertEqual(len(required_types(self.deal(mortgage=True), self.TYPES)), 3)

    def test_index_keeps_latest(self):
        docs = [{"type": "egrn", "received": "2026-10-01"}, {"type": "egrn", "received": "2026-11-01"}]
        self.assertEqual(index_documents(docs)["egrn"]["received"], "2026-11-01")

    def test_ready(self):
        docs = [{"type": "egrn", "received": "2026-11-01"}, {"type": "passport", "received": "2026-10-01"}]
        r = check_deal(self.deal(), self.TYPES, docs, TODAY)
        self.assertTrue(r["ready"])
        self.assertFalse(r["aml_control"])

    def test_missing_expired_expiring(self):
        docs = [{"type": "egrn", "received": "2026-10-10"}]     # действует до 09.11, подписание 20.11
        r = check_deal(self.deal(mortgage=True), self.TYPES, docs, TODAY)
        self.assertFalse(r["ready"])
        self.assertEqual(r["missing"], ["Паспорт продавца", "Одобрение банка"])
        self.assertEqual(r["expired"], ["Выписка из ЕГРН"])
        docs = [{"type": "egrn", "received": "2026-10-21"}, {"type": "passport", "received": "2026-10-01"}]
        r = check_deal(self.deal(), self.TYPES, docs, TODAY)   # действует до 20.11 — истекает через 13 дней
        self.assertTrue(r["ready"])
        self.assertEqual(r["expiring"], [])
        r = check_deal(self.deal(), self.TYPES, docs, "2026-11-15")
        self.assertEqual(r["expiring"], ["Выписка из ЕГРН"])

    def test_aml_threshold_and_overdue(self):
        stages = [{"name": "Аванс", "due": "2026-11-01", "done": True},
                  {"name": "Одобрение", "due": "2026-11-05", "done": False},
                  {"name": "Подписание", "due": "2026-11-20", "done": False}]
        r = check_deal(self.deal(price=5_000_000, stages=stages), self.TYPES, [], TODAY)
        self.assertTrue(r["aml_control"])
        self.assertEqual(r["overdue_stages"], ["Одобрение"])


class TestCommission(unittest.TestCase):
    def test_agency_fee_minimum(self):
        self.assertEqual(agency_fee(10_000_000, 0.03), 300_000)
        self.assertEqual(agency_fee(2_000_000, 0.03), 100_000)

    def test_agent_share_tiers(self):
        self.assertEqual(agent_share(300_000, 0), 120_000)
        self.assertEqual(agent_share(100_000, 250_000), 20_000 + 25_000)     # переход через границу 300 000
        self.assertEqual(agent_share(100_000, 1_000_000), 60_000)
        # сумма по частям равна расчёту одной суммой
        self.assertEqual(agent_share(435_000, 0) + agent_share(294_000, 435_000), agent_share(729_000, 0))

    def test_monthly_report(self):
        deals = [
            {"id": 1, "agent_id": 1, "price": 10_000_000, "rate": 0.03, "status": "closed", "closed": "2026-10-05"},
            {"id": 2, "agent_id": 1, "price": 10_000_000, "rate": 0.03, "status": "closed", "closed": "2026-10-25"},
            {"id": 3, "agent_id": 2, "price": 10_000_000, "rate": 0.03, "status": "closed", "closed": "2026-11-01"},
            {"id": 4, "agent_id": 2, "price": 10_000_000, "rate": 0.03, "status": "in_progress"},
        ]
        r = monthly_report(deals, 2026, 10)
        self.assertEqual([row["deal_id"] for row in r["rows"]], [1, 2])
        self.assertEqual(r["agents"][1], {"fee": 600_000, "agent_share": 120_000 + 150_000})
        self.assertEqual(r["agency_income"], 600_000 - 270_000)


if __name__ == "__main__":
    unittest.main()
