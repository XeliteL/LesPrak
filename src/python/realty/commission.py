"""Алгоритм 4. Расчёт комиссии агентства и вознаграждения агентов за месяц.

Комиссия агентства по сделке: max(цена × ставка договора, минимальная комиссия).
Доля агента считается по прогрессивной шкале от накопленной за месяц комиссии
(как налоговые ступени): каждая часть комиссии оплачивается по ставке своей ступени.

Шаги:
1. Отбор закрытых сделок месяца и группировка по агентам (словарь).
2. Сортировка сделок каждого агента по дате закрытия.
3. Для каждой сделки: комиссия агентства, доля агента по шкале с учётом накопленного итога.
4. Итоги по агентам и по агентству.

Сложность: O(m log m + m·T), где m — число сделок месяца, T — число ступеней шкалы
(константа), итого O(m log m). Память O(m).
"""

from realty.data import parse_date

MIN_FEE = 100_000  # минимальная комиссия агентства по сделке, руб. (условное значение)

# Прогрессивная шкала доли агента: (верхняя граница накопленной комиссии, ставка).
# Условные значения, задаются агентством в настройках системы.
AGENT_TIERS = [(300_000, 0.40), (600_000, 0.50), (float("inf"), 0.60)]


def agency_fee(price, rate, min_fee=MIN_FEE):
    """Комиссия агентства по одной сделке, руб."""
    return max(round(price * rate), min_fee)


def agent_share(fee, accumulated, tiers=AGENT_TIERS):
    """Доля агента от комиссии fee, если до этой сделки накоплено accumulated (руб.)."""
    share = 0.0
    start = accumulated
    end = accumulated + fee
    lower = 0
    for upper, rate in tiers:
        lo = max(start, lower)
        hi = min(end, upper)
        if hi > lo:
            share += (hi - lo) * rate
        lower = upper
        if upper >= end:
            break
    return round(share)


def monthly_report(deals, year, month, tiers=AGENT_TIERS):
    """Отчёт о комиссии за месяц: строки по сделкам и итоги по агентам."""
    by_agent = {}
    for d in deals:
        if d.get("status") != "closed":
            continue
        closed = parse_date(d["closed"])
        if closed.year == year and closed.month == month:
            by_agent.setdefault(d["agent_id"], []).append(d)

    rows, totals = [], {}
    for agent_id in sorted(by_agent):
        accumulated = 0
        agent_total = 0
        for d in sorted(by_agent[agent_id], key=lambda x: (x["closed"], x["id"])):
            fee = agency_fee(d["price"], d["rate"])
            share = agent_share(fee, accumulated, tiers)
            accumulated += fee
            agent_total += share
            rows.append({"deal_id": d["id"], "agent_id": agent_id, "fee": fee, "agent_share": share})
        totals[agent_id] = {"fee": accumulated, "agent_share": agent_total}

    total_fee = sum(t["fee"] for t in totals.values())
    total_share = sum(t["agent_share"] for t in totals.values())
    return {
        "rows": rows,
        "agents": totals,
        "total_fee": total_fee,
        "total_agent_share": total_share,
        "agency_income": total_fee - total_share,
    }
