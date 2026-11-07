"""Генерация диаграмм draw.io для этапа 3.

Запуск из корня репозитория: python tools/diagrams/gen.py
Создаёт:
    diagrams/bpmn-prodazha-kvartiry.drawio — модель процесса в нотации BPMN 2.0 (3 страницы);
    diagrams/er-logicheskaya-model.drawio  — логическая модель данных (нотация «вороньи лапки»);
    diagrams/algoritmy.drawio              — схемы алгоритмов по ГОСТ 19.701-90 (4 страницы).
Файлы можно дорабатывать вручную в draw.io; повторный запуск перезапишет их.
"""

from html import escape
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "diagrams"
FONT = "fontFamily=Times New Roman;"


class Page:
    def __init__(self, name):
        self.name = name
        self.cells = []
        self.n = 0

    def _id(self):
        self.n += 1
        return f"c{self.n}"

    def vertex(self, value, x, y, w, h, style, parent="1", id_=None):
        id_ = id_ or self._id()
        self.cells.append(
            f'<mxCell id="{id_}" value="{escape(value)}" style="{style}" vertex="1" parent="{parent}">'
            f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')
        return id_

    def edge(self, src, dst, label="", style="", points=()):
        id_ = self._id()
        pts = ""
        if points:
            pts = "<Array as=\"points\">" + "".join(f'<mxPoint x="{x}" y="{y}"/>' for x, y in points) + "</Array>"
        self.cells.append(
            f'<mxCell id="{id_}" value="{escape(label)}" style="{style}" edge="1" parent="1" source="{src}" target="{dst}">'
            f'<mxGeometry relative="1" as="geometry">{pts}</mxGeometry></mxCell>')
        return id_

    def xml(self, idx):
        body = "".join(self.cells)
        return (f'<diagram id="p{idx}" name="{escape(self.name)}"><mxGraphModel grid="1" gridSize="10" guides="1" '
                f'tooltips="1" connect="1" arrows="1" fold="1" page="0" pageScale="1" math="0" shadow="0">'
                f'<root><mxCell id="0"/><mxCell id="1" parent="0"/>{body}</root></mxGraphModel></diagram>')


def save(name, pages):
    xml = '<mxfile host="drawio" type="device">' + "".join(p.xml(i) for i, p in enumerate(pages)) + "</mxfile>\n"
    (OUT / name).write_text(xml, encoding="utf-8")
    print("written", OUT / name)


def side(exit_=None, entry=None):
    """Стиль точек выхода/входа ребра: стороны 'r', 'l', 't', 'b'."""
    pos = {"r": (1, 0.5), "l": (0, 0.5), "t": (0.5, 0), "b": (0.5, 1)}
    s = ""
    if exit_:
        x, y = pos[exit_]
        s += f"exitX={x};exitY={y};exitDx=0;exitDy=0;"
    if entry:
        x, y = pos[entry]
        s += f"entryX={x};entryY={y};entryDx=0;entryDy=0;"
    return s


# ======================================================================================
# BPMN 2.0
# ======================================================================================
LANES = ["Продавец", "Агент", "Юрист", "Руководитель агентства", "Покупатель", "Банк", "Росреестр"]
LANE_H = 140
POOL_LABEL = 34
LANE_LABEL = 46
COL_W = 165
X0 = POOL_LABEL + LANE_LABEL + 20

BPMN_FLOW = FONT + "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;fontSize=13;labelBackgroundColor=#ffffff;"
TASK = FONT + "rounded=1;arcSize=12;whiteSpace=wrap;html=1;fontSize=13;"
# подпись шлюза — слева сверху, чтобы не пересекаться с входящими и исходящими потоками
GW = FONT + ("shape=mxgraph.bpmn.gateway2;html=1;gwType=exclusive;outline=none;symbol=exclusiveGw;whiteSpace=wrap;"
             "fontSize=12;labelPosition=left;verticalLabelPosition=top;align=right;verticalAlign=bottom;labelWidth=80;")
EVENT = FONT + ("shape=mxgraph.bpmn.event;html=1;whiteSpace=wrap;verticalLabelPosition=bottom;verticalAlign=top;"
                "labelPosition=center;align=center;fontSize=12;perimeter=ellipsePerimeter;labelWidth=120;")


class Bpmn(Page):
    def __init__(self, name, cols, title):
        super().__init__(name)
        width = X0 + cols * COL_W
        height = LANE_H * len(LANES)
        self.vertex(title, 0, 0, width, height,
                    FONT + "swimlane;html=1;horizontal=0;startSize=%d;fontStyle=1;fontSize=14;" % POOL_LABEL)
        for i, lane in enumerate(LANES):
            self.vertex(lane, POOL_LABEL, i * LANE_H, width - POOL_LABEL, LANE_H,
                        FONT + "swimlane;html=1;horizontal=0;whiteSpace=wrap;startSize=%d;fontSize=13;" % LANE_LABEL)

    def _xy(self, col, lane, w, h, frac=0.5):
        cx = X0 + col * COL_W + COL_W / 2
        cy = LANES.index(lane) * LANE_H + LANE_H * frac
        return cx - w / 2, cy - h / 2

    def task(self, text, col, lane):
        x, y = self._xy(col, lane, 132, 70)
        return self.vertex(text, x, y, 132, 70, TASK)

    def gw(self, text, col, lane):
        x, y = self._xy(col, lane, 50, 50)
        return self.vertex(text, x, y, 50, 50, GW)

    def event(self, text, col, lane, kind="start", frac=0.5):
        styles = {
            "start": "outline=standard;symbol=general;",
            "end": "outline=end;symbol=terminate2;",
            "end_plain": "outline=end;symbol=general;",
            "link_catch": "outline=catching;symbol=link;",
            "link_throw": "outline=throwing;symbol=link;",
        }
        x, y = self._xy(col, lane, 40, 40, frac)
        return self.vertex(text, x, y, 40, 40, EVENT + styles[kind])

    def flow(self, a, b, label="", exit_=None, entry=None, points=(), extra=""):
        return self.edge(a, b, label, BPMN_FLOW + side(exit_, entry) + extra, points)

    def lane_y(self, lane, frac=0.5):
        return LANES.index(lane) * LANE_H + LANE_H * frac


def X(col):
    return X0 + col * COL_W + COL_W / 2


def bpmn():
    # --- часть 1: от обращения до заключения договора с продавцом
    p = Bpmn("Часть 1. Договор с продавцом", 9,
             "Продажа квартиры на вторичном рынке по поручению собственника (часть 1)")
    s = p.event("Решение продать квартиру", 0, "Продавец", "start")
    t1 = p.task("1. Приём обращения, назначение агента", 1, "Агент")
    t2 = p.task("2. Консультация и осмотр объекта", 2, "Агент")
    t3 = p.task("3. Проверка документов и юридической чистоты", 3, "Юрист")
    g1 = p.gw("Результат проверки", 4, "Юрист")
    fix = p.task("Устранение замечаний по документам", 4, "Продавец")
    e_ref = p.event("Отказ в работе с объектом", 5, "Руководитель агентства", "end_plain")
    t4 = p.task("4. Оценка стоимости, расчёт цены выставления", 5, "Агент")
    g2 = p.gw("Продавец согласен с условиями?", 6, "Агент")
    e_no = p.event("Договор не заключён", 7, "Продавец", "end_plain")
    t5 = p.task("5. Заключение договора с продавцом", 7, "Руководитель агентства")
    l_a = p.event("К продвижению объекта (А)", 8, "Руководитель агентства", "link_throw")
    p.flow(s, t1, exit_="r", entry="t", points=((X(1), p.lane_y("Продавец")),))
    p.flow(t1, t2)
    p.flow(t2, t3, exit_="b", entry="l", points=((X(2), p.lane_y("Юрист")),))
    p.flow(t3, g1)
    p.flow(g1, t4, "в порядке", exit_="r", entry="b", points=((X(5), p.lane_y("Юрист")),))
    p.flow(g1, fix, "устранимые замечания", exit_="t", entry="b")
    p.flow(fix, t3, exit_="l", entry="t", points=((X(3), p.lane_y("Продавец")),))
    p.flow(g1, e_ref, "препятствия к сделке", exit_="b", entry="l", points=((X(4), p.lane_y("Руководитель агентства")),))
    p.flow(t4, g2)
    p.flow(g2, e_no, "нет", exit_="t", entry="l", points=((X(6), p.lane_y("Продавец")),))
    p.flow(g2, t5, "да", exit_="r", entry="t", points=((X(7), p.lane_y("Агент")),))
    p.flow(t5, l_a)
    pages = [p]

    # --- часть 2: продвижение объекта и поиск покупателя
    p = Bpmn("Часть 2. Поиск покупателя", 10,
             "Продажа квартиры на вторичном рынке по поручению собственника (часть 2)")
    c_a = p.event("От договора с продавцом (А)", 0, "Агент", "link_catch")
    t6 = p.task("6. Подготовка объекта: фото, описание", 1, "Агент")
    t7 = p.task("7. Размещение объявлений на площадках", 2, "Агент")
    c_b = p.event("Возврат к поиску (Б)", 3, "Агент", "link_catch", frac=0.82)
    t8 = p.task("8. Обработка заявок, подбор покупателей, показы", 4, "Агент")
    v = p.task("Просмотр квартиры", 5, "Покупатель")
    g3 = p.gw("Покупатель найден?", 6, "Агент")
    g4 = p.gw("Срок договора истёк?", 6, "Продавец")
    e_end = p.event("Договор прекращён", 7, "Продавец", "end_plain")
    adj = p.task("Корректировка цены", 4, "Продавец")
    t9 = p.task("9. Переговоры, соглашение об авансе", 7, "Агент")
    adv = p.task("Внесение аванса", 8, "Покупатель")
    l_c = p.event("К подготовке сделки (В)", 9, "Покупатель", "link_throw")
    p.flow(c_a, t6)
    p.flow(t6, t7)
    p.flow(t7, t8)
    p.flow(c_b, t8, exit_="r", entry="b", points=((X(4), p.lane_y("Агент", 0.82)),))
    p.flow(t8, v, exit_="r", entry="t", points=((X(5), p.lane_y("Агент")),))
    p.flow(v, g3, exit_="r", entry="b", points=((X(6), p.lane_y("Покупатель")),))
    p.flow(g3, t9, "да", exit_="r", entry="l")
    p.flow(g3, g4, "нет", exit_="t", entry="b")
    p.flow(g4, e_end, "да", exit_="r", entry="l")
    p.flow(g4, adj, "нет", exit_="l", entry="r")
    p.flow(adj, t7, exit_="l", entry="t", points=((X(2), p.lane_y("Продавец")),))
    p.flow(t9, adv, exit_="b", entry="l", points=((X(7), p.lane_y("Покупатель")),))
    p.flow(adv, l_c)
    pages.append(p)

    # --- часть 3: сделка, регистрация, расчёты
    p = Bpmn("Часть 3. Сделка и регистрация", 11,
             "Продажа квартиры на вторичном рынке по поручению собственника (часть 3)")
    c_c = p.event("От внесения аванса (В)", 0, "Агент", "link_catch")
    g5 = p.gw("Ипотека?", 1, "Агент")
    bank = p.task("Рассмотрение ипотеки и одобрение объекта", 1, "Банк")
    g6 = p.gw("Одобрено?", 2, "Банк")
    l_b = p.event("К поиску покупателя (Б)", 2, "Росреестр", "link_throw")
    g7 = p.gw("", 3, "Агент")
    t10 = p.task("10. Подготовка сделки: документы, проект ДКП, контроль по 115-ФЗ", 4, "Юрист")
    t11 = p.task("11. Подписание ДКП, подача на регистрацию", 5, "Агент")
    t12 = p.task("12. Регистрация перехода права", 6, "Росреестр")
    g8 = p.gw("Регистрация приостановлена?", 7, "Росреестр")
    fix2 = p.task("Устранение замечаний регистратора", 7, "Юрист")
    pay = p.task("Расчёты с продавцом", 8, "Банк")
    t13 = p.task("13. Передача квартиры, закрытие сделки, оплата услуг", 9, "Агент")
    e = p.event("Сделка закрыта", 10, "Агент", "end")
    p.flow(c_c, g5)
    p.flow(g5, bank, "да", exit_="b", entry="t")
    p.flow(g5, g7, "нет", exit_="r", entry="l")
    p.flow(bank, g6)
    p.flow(g6, g7, "да", exit_="t", entry="b", points=((X(2), p.lane_y("Агент", 0.85)), (X(3), p.lane_y("Агент", 0.85))))
    p.flow(g6, l_b, "нет", exit_="b", entry="t")
    p.flow(g7, t10, exit_="r", entry="t", points=((X(4), p.lane_y("Агент")),))
    p.flow(t10, t11, exit_="r", entry="b", points=((X(5), p.lane_y("Юрист")),))
    p.flow(t11, t12, exit_="r", entry=None, points=((X(6) - 35, p.lane_y("Агент")),),
           extra="entryX=0.25;entryY=0;entryDx=0;entryDy=0;")
    p.flow(t12, g8)
    p.flow(g8, fix2, "да", exit_="t", entry="b")
    p.flow(fix2, t12, exit_="l", points=((X(6) + 35, p.lane_y("Юрист")),),
           extra="entryX=0.75;entryY=0;entryDx=0;entryDy=0;")
    p.flow(g8, pay, "нет", exit_="r", entry="b", points=((X(8), p.lane_y("Росреестр")),))
    p.flow(pay, t13, exit_="r", entry="b", points=((X(9), p.lane_y("Банк")),))
    p.flow(t13, e)
    pages.append(p)
    save("bpmn-prodazha-kvartiry.drawio", pages)


# ======================================================================================
# ER (логическая модель)
# ======================================================================================
ENTITIES = {
    "Клиент": ["PK id_клиента", "ФИО", "телефон", "email", "паспортные_данные"],
    "Сотрудник": ["PK id_сотрудника", "ФИО", "должность", "телефон", "дата_приёма"],
    "Район": ["PK id_района", "название"],
    "Район_заявки": ["PK,FK id_заявки", "PK,FK id_района", "приоритет"],
    "Объект": ["PK id_объекта", "FK id_района", "адрес", "кадастровый_номер", "число_комнат", "площадь",
               "этаж", "этажность", "год_постройки"],
    "Собственник_объекта": ["PK,FK id_объекта", "PK,FK id_клиента", "доля", "несовершеннолетний"],
    "Договор_с_продавцом": ["PK id_договора", "FK id_объекта", "FK id_продавца", "FK id_агента", "дата_заключения",
                            "срок_действия", "цена_выставления", "ставка_комиссии", "статус"],
    "Объявление": ["PK id_объявления", "FK id_договора", "площадка", "дата_размещения", "цена", "статус"],
    "Заявка_покупателя": ["PK id_заявки", "FK id_клиента", "FK id_агента", "бюджет_мин", "бюджет_макс",
                          "комнат_мин", "комнат_макс", "площадь_мин", "не_первый_этаж", "не_последний_этаж", "статус"],
    "Показ": ["PK id_показа", "FK id_договора", "FK id_заявки", "дата_время", "результат"],
    "Сделка": ["PK id_сделки", "FK id_договора", "FK id_покупателя", "цена", "ипотека", "способ_расчётов",
               "дата_аванса", "дата_подписания", "дата_регистрации", "дата_закрытия", "статус"],
    "Этап_сделки": ["PK id_этапа", "FK id_сделки", "название", "плановая_дата", "фактическая_дата"],
    "Тип_документа": ["PK код_типа", "название", "условие_обязательности", "срок_действия_дн"],
    "Документ": ["PK id_документа", "FK id_сделки", "FK код_типа", "номер", "дата_получения", "файл"],
    "Начисление": ["PK id_начисления", "FK id_сделки", "FK id_сотрудника", "комиссия_агентства",
                   "вознаграждение_агента", "период"],
    "Рыночное_предложение": ["PK id_предложения", "FK id_района", "источник", "число_комнат", "площадь", "этаж",
                             "этажность", "год_постройки", "цена", "дата_публикации"],
}
GRID = [
    ["Рыночное_предложение", "Район", "Район_заявки", "Заявка_покупателя", "Клиент"],
    ["Объявление", "Объект", "Показ", "Сотрудник", "Собственник_объекта"],
    [None, "Договор_с_продавцом", "Сделка", "Начисление", None],
    [None, "Этап_сделки", "Документ", "Тип_документа", None],
]
ER_W, ROW_H, HEAD_H, GAP_X, GAP_Y = 210, 19, 26, 100, 90


def er():
    p = Page("Логическая модель данных")
    ids, box = {}, {}
    row_top, row_bottom = [], []
    y = 0
    for row in GRID:
        h_row = max(HEAD_H + ROW_H * len(ENTITIES[e]) for e in row if e)
        row_top.append(y)
        row_bottom.append(y + h_row)
        for c, name in enumerate(row):
            if not name:
                continue
            attrs = ENTITIES[name]
            x = c * (ER_W + GAP_X)
            h = HEAD_H + ROW_H * len(attrs)
            eid = p.vertex(name, x, y, ER_W, h,
                           FONT + "swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize=%d;"
                           "horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;"
                           "marginBottom=0;html=1;fontSize=13;fillColor=#f5f5f5;" % HEAD_H)
            for i, a in enumerate(attrs):
                key, _, rest = a.partition(" ")
                if key in ("PK", "FK", "PK,FK"):
                    text = f"<b>{key}</b>&nbsp;{rest}" if "PK" in key else f"<i>{key}</i>&nbsp;{rest}"
                    if "PK" in key:
                        text = f"<u>{text}</u>"
                else:
                    text = a
                p.cells.append(
                    f'<mxCell id="{eid}a{i}" value="{escape(text)}" style="{FONT}text;html=1;strokeColor=none;'
                    f'fillColor=none;align=left;verticalAlign=middle;spacingLeft=6;fontSize=12;" vertex="1" '
                    f'parent="{eid}"><mxGeometry y="{HEAD_H + i * ROW_H}" width="{ER_W}" height="{ROW_H}" '
                    f'as="geometry"/></mxCell>')
            ids[name] = eid
            box[name] = (x, y, ER_W, h)
        y += h_row + GAP_Y

    def xch(c, off=0):   # вертикальный коридор справа от колонки c
        return c * (ER_W + GAP_X) + ER_W + GAP_X / 2 + off

    def ych(r, off=0):   # горизонтальный коридор под строкой r
        return row_bottom[r] + GAP_Y / 2 + off

    def at(name, fx, fy):
        x, y, w, h = box[name]
        return x + w * fx, y + h * fy

    def pt(fx, fy, kind):
        return f"{kind}X={fx};{kind}Y={fy};{kind}Dx=0;{kind}Dy=0;"

    style = FONT + ("edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=ERmany;startArrow=ERmandOne;"
                    "endFill=0;startFill=0;")
    # (родитель «один», потомок «много», точка выхода, точка входа, промежуточные точки)
    routes = [
        ("Район", "Объект", (0.5, 1), (0.5, 0), []),
        ("Район", "Рыночное_предложение", (0, 0.5), (1, 0.5), []),
        ("Район", "Район_заявки", (1, 0.4), (0, 0.4), []),
        ("Заявка_покупателя", "Район_заявки", (0, 0.15), (1, 0.4), []),
        ("Клиент", "Заявка_покупателя", (0, 0.4), (1, 0.4), []),
        ("Клиент", "Собственник_объекта", (0.5, 1), (0.5, 0), []),
        ("Сотрудник", "Заявка_покупателя", (0.65, 0), (0.65, 1), []),
        ("Заявка_покупателя", "Показ", (0.3, 1), (0.5, 0),
         [(at("Заявка_покупателя", 0.3, 1)[0], ych(0)), (at("Показ", 0.5, 0)[0], ych(0))]),
        ("Объект", "Договор_с_продавцом", (0.5, 1), (0.5, 0), []),
        ("Объект", "Собственник_объекта", (0.85, 1), (0.5, 1),
         [(at("Объект", 0.85, 1)[0], ych(1, -14)), (at("Собственник_объекта", 0.5, 1)[0], ych(1, -14))]),
        ("Договор_с_продавцом", "Показ", (0.85, 0), (0.5, 1),
         [(at("Договор_с_продавцом", 0.85, 0)[0], ych(1, 14)), (at("Показ", 0.5, 1)[0], ych(1, 14))]),
        ("Сотрудник", "Договор_с_продавцом", (0.3, 1), (1, 0.12),
         [(at("Сотрудник", 0.3, 1)[0], ych(1, 0)), (xch(1, 12), ych(1, 0)),
          (xch(1, 12), at("Договор_с_продавцом", 1, 0.12)[1])]),
        ("Клиент", "Договор_с_продавцом", (1, 0.6), (0, 0.25),
         [(xch(4, -10), at("Клиент", 1, 0.6)[1]), (xch(4, -10), ych(1, -28)), (xch(0, 0), ych(1, -28)),
          (xch(0, 0), at("Договор_с_продавцом", 0, 0.25)[1])]),
        ("Договор_с_продавцом", "Объявление", (0, 0.6), (0.5, 1),
         [(at("Объявление", 0.5, 1)[0], at("Договор_с_продавцом", 0, 0.6)[1])]),
        ("Договор_с_продавцом", "Сделка", (1, 0.45), (0, 0.45 * 197 / 235), []),
        ("Клиент", "Сделка", (1, 0.85), (1, 0.9),
         [(xch(4, 0), at("Клиент", 1, 0.85)[1]), (xch(4, 0), at("Сделка", 1, 0.9)[1])]),
        ("Сделка", "Начисление", (1, 0.3), (0, 0.5), []),
        ("Сотрудник", "Начисление", (0.5, 1), (0.5, 0), []),
        ("Сделка", "Этап_сделки", (0, 0.85), (1, 0.5),
         [(xch(1, -12), at("Сделка", 0, 0.85)[1]), (xch(1, -12), at("Этап_сделки", 1, 0.5)[1])]),
        ("Сделка", "Документ", (0.5, 1), (0.5, 0), []),
        ("Тип_документа", "Документ", (0, 0.4), (1, 0.4), []),
    ]
    for parent, child, ex, en, pts in routes:
        p.edge(ids[parent], ids[child], "", style + pt(*ex, "exit") + pt(*en, "entry"), pts)
    save("er-logicheskaya-model.drawio", [p])


# ======================================================================================
# Схемы алгоритмов (ГОСТ 19.701-90)
# ======================================================================================
FC_W, FC_H, FC_ROW, FC_COL = 250, 56, 90, 340
FC_STYLES = {
    "term": "rounded=1;arcSize=50;",
    "io": "shape=parallelogram;perimeter=parallelogramPerimeter;size=0.1;spacingLeft=22;spacingRight=22;",
    "proc": "rounded=0;",
    "sub": "shape=process;size=0.06;",
    "dec": "rhombus;spacingLeft=48;spacingRight=48;",
    "loop": "shape=loopLimit;",
    "loopend": "shape=loopLimit;flipV=1;",
}
FC_EDGE = FONT + "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=block;endFill=1;fontSize=12;labelBackgroundColor=#ffffff;"


class Flow(Page):
    def node(self, kind, text, col, row, h=None):
        h = h or (FC_H + 22 if kind == "dec" else FC_H)
        w = FC_W + 60 if kind == "dec" else FC_W
        x = col * FC_COL + (FC_COL - w) / 2
        y = row * FC_ROW + (FC_ROW - h) / 2
        return self.vertex(text, x, y, w, h, FONT + "whiteSpace=wrap;html=1;fontSize=12;" + FC_STYLES[kind])

    def go(self, a, b, label="", exit_="b", entry="t", points=()):
        return self.edge(a, b, label, FC_EDGE + side(exit_, entry), points)


def cx(col):
    return col * FC_COL + FC_COL / 2


def cy(row):
    return row * FC_ROW + FC_ROW / 2


def algorithms():
    pages = []
    # ---------------- Алгоритм 1
    f = Flow("Алгоритм 1. Оценка стоимости")
    n = [
        f.node("term", "Начало", 0, 0),
        f.node("io", "Ввод: оцениваемая квартира, объявления, текущая дата", 0, 1),
        f.node("proc", "аналоги ← пустой список", 0, 2),
        f.node("loop", "Для каждого объявления a", 0, 3),
        f.node("dec", "Район и комнаты совпадают, |ΔS|/S ≤ 0,25, возраст ≤ 180 дн.?", 0, 4, h=84),
        f.node("proc", "Добавить a в аналоги", 0, 5),
        f.node("loopend", "Конец цикла по a", 0, 6),
        f.node("dec", "Аналогов меньше 3?", 0, 7),
        f.node("proc", "Для каждого аналога: u ← цена / S × 0,97 × kэт(объект) / kэт(аналог)", 0, 8),
        f.node("proc", "m ← медиана(u); оставить аналоги с |u − m| / m ≤ 0,25", 0, 9),
        f.node("dec", "Осталось меньше 3?", 0, 10),
        f.node("proc", "w ← 1 / (1 + d); u* ← Σ(w·u) / Σw; цена ← u* × S, округлить до 10 000", 0, 11),
        f.node("io", "Вывод: цена, диапазон цен, использованные аналоги", 0, 12),
        f.node("term", "Конец", 0, 13),
    ]
    ins = f.node("io", "Вывод: недостаточно аналогов", 1, 10)
    for a, b in zip(n, n[1:]):
        lbl = "да" if b in (n[5],) else "нет" if b in (n[8], n[11]) else ""
        if a == n[4] and b == n[5]:
            f.go(a, b, "да")
        elif (a, b) in ((n[7], n[8]), (n[10], n[11])):
            f.go(a, b, "нет")
        else:
            f.go(a, b, lbl)
    f.go(n[4], n[6], "нет", "r", "r", points=((cx(0) + FC_W / 2 + 60, cy(4)), (cx(0) + FC_W / 2 + 60, cy(6))))
    f.go(n[7], ins, "да", "r", "t", points=((cx(1), cy(7)),))
    f.go(n[10], ins, "да", "r", "l")
    f.go(ins, n[13], "", "b", "r", points=((cx(1), cy(13)),))
    pages.append(f)

    # ---------------- Алгоритм 2
    f = Flow("Алгоритм 2. Подбор объектов")
    n = [
        f.node("term", "Начало", 0, 0),
        f.node("io", "Ввод: заявка покупателя, объекты, N", 0, 1),
        f.node("proc", "куча ← пустая (минимальная, по оценке)", 0, 2),
        f.node("loop", "Для каждого объекта o", 0, 3),
        f.node("dec", "o в продаже и удовлетворяет условиям заявки?", 0, 4, h=84),
        f.node("proc", "s ← 50·Pцена + 30·Pплощадь + 20·Pрайон", 0, 5),
        f.node("dec", "В куче меньше N элементов?", 0, 6),
        f.node("proc", "Добавить (s, o) в кучу", 0, 7),
        f.node("loopend", "Конец цикла по o", 0, 8),
        f.node("proc", "Упорядочить элементы кучи по убыванию s", 0, 9),
        f.node("io", "Вывод: до N объектов с оценками", 0, 10),
        f.node("term", "Конец", 0, 11),
    ]
    d2 = f.node("dec", "s больше минимума кучи?", 1, 6)
    rep = f.node("proc", "Заменить минимальный элемент кучи на (s, o)", 1, 7)
    for a, b in zip(n, n[1:]):
        f.go(a, b, "да" if (a, b) in ((n[4], n[5]), (n[6], n[7])) else "")
    f.go(n[4], n[8], "нет", "r", "r", points=((cx(1) + FC_W / 2 + 60, cy(4)), (cx(1) + FC_W / 2 + 60, cy(8))))
    f.go(n[6], d2, "нет", "r", "l")
    f.go(d2, rep, "да", "b", "t")
    f.go(d2, n[8], "нет", "r", "r", points=((cx(1) + FC_W / 2 + 60, cy(6)),))
    f.go(rep, n[8], "", "b", "r", points=((cx(1), cy(8)),))
    pages.append(f)

    # ---------------- Алгоритм 3
    f = Flow("Алгоритм 3. Контроль документов")
    n = [
        f.node("term", "Начало", 0, 0),
        f.node("io", "Ввод: сделка, типы документов, полученные документы, текущая дата", 0, 1),
        f.node("proc", "R ← типы, обязательные при условиях сделки", 0, 2),
        f.node("proc", "I ← словарь «тип → последний полученный документ»", 0, 3),
        f.node("loop", "Для каждого типа t из R", 0, 4),
        f.node("dec", "Документ типа t есть в I?", 0, 5),
        f.node("dec", "Для t задан срок действия?", 0, 6),
        f.node("proc", "до ← дата получения + срок действия", 0, 7),
        f.node("dec", "до < даты подписания?", 0, 8),
        f.node("dec", "до − сегодня ≤ 7 дн.?", 0, 9),
        f.node("proc", "Добавить t в «истекают»", 0, 10),
        f.node("loopend", "Конец цикла по t", 0, 11),
        f.node("proc", "Просроченные этапы ← не выполненные этапы с плановой датой < сегодня", 0, 12),
        f.node("proc", "контроль_115ФЗ ← цена ≥ 5 000 000; готова ← нет отсутствующих и просроченных", 0, 13),
        f.node("io", "Вывод: готовность, отсутствующие, просроченные, истекающие документы, этапы", 0, 14),
        f.node("term", "Конец", 0, 15),
    ]
    miss = f.node("proc", "Добавить t в «отсутствуют»", 1, 5)
    exp = f.node("proc", "Добавить t в «просрочены»", 1, 8)
    yes = {(n[5], n[6]), (n[6], n[7]), (n[9], n[10])}
    no = {(n[8], n[9])}
    for a, b in zip(n, n[1:]):
        f.go(a, b, "да" if (a, b) in yes else "нет" if (a, b) in no else "")
    right = cx(1) + FC_W / 2 + 60
    f.go(n[5], miss, "нет", "r", "l")
    f.go(miss, n[11], "", "r", "r", points=((right, cy(5)), (right, cy(11))))
    f.go(n[6], n[11], "нет", "r", "r", points=((right, cy(6)),))
    f.go(n[8], exp, "да", "r", "l")
    f.go(exp, n[11], "", "r", "r", points=((right, cy(8)),))
    f.go(n[9], n[11], "нет", "r", "r", points=((right, cy(9)),))
    pages.append(f)

    # ---------------- Алгоритм 4
    f = Flow("Алгоритм 4. Расчёт комиссии")
    n = [
        f.node("term", "Начало", 0, 0),
        f.node("io", "Ввод: сделки, год, месяц, шкала вознаграждения агента", 0, 1),
        f.node("proc", "Сгруппировать закрытые в месяце сделки по агентам", 0, 2),
        f.node("loop", "Для каждого агента g", 0, 3),
        f.node("proc", "Упорядочить сделки агента по дате закрытия; накоплено ← 0", 0, 4),
        f.node("loop", "Для каждой сделки d агента", 0, 5),
        f.node("proc", "комиссия ← max(цена × ставка, минимальная комиссия)", 0, 6),
        f.node("sub", "доля ← доля агента по шкале (комиссия, накоплено)", 0, 7),
        f.node("proc", "накоплено ← накоплено + комиссия; итог агента ← итог + доля", 0, 8),
        f.node("loopend", "Конец цикла по d", 0, 9),
        f.node("proc", "Записать итоги агента g", 0, 10),
        f.node("loopend", "Конец цикла по g", 0, 11),
        f.node("proc", "Доход агентства ← Σ комиссий − Σ вознаграждений", 0, 12),
        f.node("io", "Вывод: строки по сделкам, итоги по агентам и агентству", 0, 13),
        f.node("term", "Конец", 0, 14),
    ]
    for a, b in zip(n, n[1:]):
        f.go(a, b)
    pages.append(f)

    # ---------------- Подпрограмма: доля агента по прогрессивной шкале
    f = Flow("Подпрограмма. Доля агента по шкале")
    n = [
        f.node("term", "Начало", 0, 0),
        f.node("io", "Ввод: комиссия F, накоплено A, шкала ступеней (граница, ставка)", 0, 1),
        f.node("proc", "доля ← 0; нижняя ← 0; конец ← A + F", 0, 2),
        f.node("loop", "Для каждой ступени (верхняя, ставка)", 0, 3),
        f.node("proc", "lo ← max(A, нижняя); hi ← min(конец, верхняя)", 0, 4),
        f.node("dec", "hi > lo?", 0, 5),
        f.node("proc", "доля ← доля + (hi − lo) × ставка", 0, 6),
        f.node("proc", "нижняя ← верхняя", 0, 7),
        f.node("dec", "верхняя ≥ конец?", 0, 8),
        f.node("loopend", "Конец цикла по ступеням", 0, 9),
        f.node("io", "Вывод: доля, округлённая до рубля", 0, 10),
        f.node("term", "Конец", 0, 11),
    ]
    for a, b in zip(n, n[1:]):
        f.go(a, b, "да" if (a, b) == (n[5], n[6]) else "нет" if (a, b) == (n[8], n[9]) else "")
    f.go(n[5], n[7], "нет", "r", "r", points=((cx(0) + FC_W / 2 + 60, cy(5)), (cx(0) + FC_W / 2 + 60, cy(7))))
    f.go(n[8], n[10], "да", "r", "r", points=((cx(0) + FC_W / 2 + 60, cy(8)), (cx(0) + FC_W / 2 + 60, cy(10))))
    pages.append(f)
    save("algoritmy.drawio", pages)


if __name__ == "__main__":
    bpmn()
    er()
    algorithms()
