-- Физическая модель данных агентства недвижимости (PostgreSQL 16+).
-- Построена по логической модели этапа 3 (16 сущностей, 3НФ).
-- Запуск: psql -d realty -v ON_ERROR_STOP=1 -f schema.sql

DROP SCHEMA IF EXISTS realty CASCADE;
CREATE SCHEMA realty;
SET search_path TO realty;

-- ---------- справочники ----------

CREATE TABLE district (
    id   smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE
);
COMMENT ON TABLE district IS 'Район города';

CREATE TABLE document_type (
    code          text PRIMARY KEY,
    name          text NOT NULL,
    required_if   text CHECK (required_if IN ('mortgage', 'married_seller', 'minor_owners')),
    validity_days smallint CHECK (validity_days > 0),
    list_order    smallint NOT NULL UNIQUE
);
COMMENT ON TABLE document_type IS 'Тип документа для сделки';
COMMENT ON COLUMN document_type.required_if IS 'Условие обязательности; NULL — документ нужен всегда';
COMMENT ON COLUMN document_type.validity_days IS 'Срок действия в днях; NULL — бессрочный';
COMMENT ON COLUMN document_type.list_order IS 'Порядок в перечне документов для проверки сделки';

-- ---------- участники ----------

CREATE TABLE client (
    id        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name text NOT NULL,
    phone     text CHECK (phone ~ '^\+7\d{10}$'),
    email     text CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    passport  text CHECK (passport ~ '^\d{4} \d{6}$'),
    married   boolean NOT NULL DEFAULT false
);
COMMENT ON TABLE client IS 'Клиент: продавец, собственник или покупатель';
COMMENT ON COLUMN client.married IS 'Состоит в браке: нужно нотариальное согласие супруга при продаже';

CREATE TABLE employee (
    id        smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name text NOT NULL,
    position  text NOT NULL CHECK (position IN ('agent', 'lawyer', 'head')),
    phone     text CHECK (phone ~ '^\+7\d{10}$'),
    hired_on  date NOT NULL
);
COMMENT ON TABLE employee IS 'Сотрудник агентства';

-- ---------- объекты и договоры ----------

CREATE TABLE property (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    district_id      smallint NOT NULL REFERENCES district,
    address          text NOT NULL,
    cadastral_number text UNIQUE CHECK (cadastral_number ~ '^\d{2}:\d{2}:\d{6,7}:\d+$'),
    rooms            smallint NOT NULL CHECK (rooms BETWEEN 0 AND 10),
    area             numeric(6, 1) NOT NULL CHECK (area > 0),
    floor            smallint NOT NULL CHECK (floor >= 1),
    floors           smallint NOT NULL CHECK (floors >= 1),
    year_built       smallint CHECK (year_built BETWEEN 1800 AND 2100),
    CHECK (floor <= floors)
);
COMMENT ON TABLE property IS 'Квартира, которую продаёт агентство';
COMMENT ON COLUMN property.rooms IS '0 — студия';

CREATE TABLE property_owner (
    property_id integer NOT NULL REFERENCES property ON DELETE CASCADE,
    client_id   integer NOT NULL REFERENCES client,
    share       numeric(5, 4) NOT NULL CHECK (share > 0 AND share <= 1),
    is_minor    boolean NOT NULL DEFAULT false,
    PRIMARY KEY (property_id, client_id)
);
COMMENT ON TABLE property_owner IS 'Доля клиента в праве собственности на квартиру';

CREATE TABLE seller_contract (
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    property_id     integer NOT NULL REFERENCES property,
    seller_id       integer NOT NULL REFERENCES client,
    agent_id        smallint NOT NULL REFERENCES employee,
    signed_on       date NOT NULL,
    valid_until     date NOT NULL,
    list_price      bigint NOT NULL CHECK (list_price > 0),
    commission_rate numeric(5, 4) NOT NULL CHECK (commission_rate > 0 AND commission_rate <= 0.1),
    status          text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'reserved', 'sold', 'terminated')),
    CHECK (valid_until >= signed_on)
);
COMMENT ON TABLE seller_contract IS 'Договор возмездного оказания услуг с продавцом';
-- у квартиры не может быть двух действующих договоров одновременно
CREATE UNIQUE INDEX seller_contract_one_open ON seller_contract (property_id)
    WHERE status IN ('active', 'reserved');

CREATE TABLE advert (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id  integer NOT NULL REFERENCES seller_contract,
    platform     text NOT NULL,
    published_on date NOT NULL,
    price        bigint NOT NULL CHECK (price > 0),
    status       text NOT NULL DEFAULT 'published' CHECK (status IN ('published', 'paused', 'removed'))
);
COMMENT ON TABLE advert IS 'Объявление на онлайн-площадке';

-- ---------- покупатели ----------

CREATE TABLE buyer_request (
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client_id       integer NOT NULL REFERENCES client,
    agent_id        smallint NOT NULL REFERENCES employee,
    budget_min      bigint CHECK (budget_min >= 0),
    budget_max      bigint NOT NULL CHECK (budget_max > 0),
    rooms_min       smallint NOT NULL CHECK (rooms_min >= 0),
    rooms_max       smallint NOT NULL,
    area_min        numeric(6, 1) CHECK (area_min > 0),
    not_first_floor boolean NOT NULL DEFAULT false,
    not_last_floor  boolean NOT NULL DEFAULT false,
    status          text NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'closed')),
    CHECK (budget_min IS NULL OR budget_min <= budget_max),
    CHECK (rooms_min <= rooms_max)
);
COMMENT ON TABLE buyer_request IS 'Заявка покупателя';

CREATE TABLE request_district (
    request_id  integer NOT NULL REFERENCES buyer_request ON DELETE CASCADE,
    district_id smallint NOT NULL REFERENCES district,
    priority    smallint NOT NULL CHECK (priority >= 1),
    PRIMARY KEY (request_id, district_id),
    UNIQUE (request_id, priority)
);
COMMENT ON TABLE request_district IS 'Район, подходящий покупателю; 1 — наивысший приоритет';

CREATE TABLE viewing (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id integer NOT NULL REFERENCES seller_contract,
    request_id  integer NOT NULL REFERENCES buyer_request,
    held_at     timestamptz NOT NULL,
    result      text
);
COMMENT ON TABLE viewing IS 'Показ квартиры покупателю';

-- ---------- сделки ----------

CREATE TABLE deal (
    id             integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id    integer NOT NULL REFERENCES seller_contract,
    buyer_id       integer NOT NULL REFERENCES client,
    price          bigint NOT NULL CHECK (price > 0),
    mortgage       boolean NOT NULL DEFAULT false,
    payment_method text NOT NULL DEFAULT 'escrow' CHECK (payment_method IN ('escrow', 'letter_of_credit', 'safe_box', 'transfer')),
    advance_on     date,
    signing_on     date,
    registered_on  date,
    closed_on      date,
    status         text NOT NULL DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'closed', 'cancelled')),
    CHECK ((status = 'closed') = (closed_on IS NOT NULL)),
    CHECK (registered_on IS NULL OR signing_on IS NULL OR registered_on >= signing_on)
);
COMMENT ON TABLE deal IS 'Сделка купли-продажи';

CREATE TABLE deal_stage (
    id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deal_id integer NOT NULL REFERENCES deal ON DELETE CASCADE,
    name    text NOT NULL,
    due_on  date NOT NULL,
    done_on date
);
COMMENT ON TABLE deal_stage IS 'Этап сделки с плановой и фактической датой';

CREATE TABLE document (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deal_id     integer NOT NULL REFERENCES deal ON DELETE CASCADE,
    type_code   text NOT NULL REFERENCES document_type,
    number      text,
    received_on date NOT NULL,
    file_path   text
);
COMMENT ON TABLE document IS 'Документ, полученный по сделке';

CREATE TABLE accrual (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deal_id      integer NOT NULL REFERENCES deal,
    employee_id  smallint NOT NULL REFERENCES employee,
    agency_fee   bigint NOT NULL CHECK (agency_fee >= 0),
    agent_reward bigint NOT NULL CHECK (agent_reward >= 0),
    period       date NOT NULL CHECK (extract(day FROM period) = 1),
    UNIQUE (deal_id, employee_id),
    CHECK (agent_reward <= agency_fee)
);
COMMENT ON TABLE accrual IS 'Начисление комиссии агентства и вознаграждения агента';
COMMENT ON COLUMN accrual.period IS 'Первый день месяца начисления';

-- ---------- внешние данные о рынке ----------

CREATE TABLE market_listing (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    district_id  smallint NOT NULL REFERENCES district,
    source       text NOT NULL,
    rooms        smallint NOT NULL CHECK (rooms BETWEEN 0 AND 10),
    area         numeric(6, 1) NOT NULL CHECK (area > 0),
    floor        smallint NOT NULL CHECK (floor >= 1),
    floors       smallint NOT NULL CHECK (floors >= 1),
    year_built   smallint CHECK (year_built BETWEEN 1800 AND 2100),
    price        bigint NOT NULL CHECK (price > 0),
    published_on date NOT NULL,
    CHECK (floor <= floors)
);
COMMENT ON TABLE market_listing IS 'Рыночное предложение с онлайн-площадок для оценки стоимости';

-- ---------- индексы ----------
-- PostgreSQL не создаёт индексы по внешним ключам автоматически

CREATE INDEX ON property (district_id);
CREATE INDEX ON property_owner (client_id);
CREATE INDEX ON seller_contract (seller_id);
CREATE INDEX ON seller_contract (agent_id);
CREATE INDEX ON advert (contract_id);
CREATE INDEX ON buyer_request (client_id);
CREATE INDEX ON request_district (district_id);
CREATE INDEX ON viewing (contract_id);
CREATE INDEX ON viewing (request_id);
CREATE INDEX ON deal (contract_id);
CREATE INDEX ON deal (buyer_id);
CREATE INDEX ON deal_stage (deal_id);
CREATE INDEX ON document (deal_id, type_code, received_on DESC); -- последний документ каждого типа
CREATE INDEX ON accrual (employee_id, period);
-- алгоритм 1: отбор аналогов по району и числу комнат за последние месяцы
CREATE INDEX ON market_listing (district_id, rooms, published_on);
-- алгоритм 4: закрытые сделки за месяц
CREATE INDEX ON deal (closed_on) WHERE status = 'closed';

-- ---------- представления для алгоритмов ----------

-- квартиры агентства с ценой и статусом по действующему (или последнему) договору
CREATE VIEW property_on_sale AS
SELECT DISTINCT ON (p.id)
       p.id, d.name AS district, p.rooms, p.area, p.floor, p.floors, p.year_built,
       c.list_price AS price, c.status, c.id AS contract_id
FROM property p
JOIN district d ON d.id = p.district_id
JOIN seller_contract c ON c.property_id = p.id
ORDER BY p.id, (c.status IN ('active', 'reserved')) DESC, c.signed_on DESC;

-- условия сделки, от которых зависит перечень документов (алгоритм 3)
CREATE VIEW deal_conditions AS
SELECT dl.id AS deal_id,
       dl.mortgage,
       s.married AS married_seller,
       EXISTS (SELECT 1 FROM property_owner o WHERE o.property_id = c.property_id AND o.is_minor) AS minor_owners
FROM deal dl
JOIN seller_contract c ON c.id = dl.contract_id
JOIN client s ON s.id = c.seller_id;
