-- Тесты физической модели: ограничения целостности и представления.
-- Запуск на БД с загруженными schema.sql и seed.sql: psql -d realty -v ON_ERROR_STOP=1 -f tests.sql
-- Все изменения откатываются. Ошибка любого теста прерывает выполнение.
SET search_path TO realty;
\set QUIET on
\o /dev/null
BEGIN;

CREATE FUNCTION pg_temp.expect_error(name text, stmt text, state text) RETURNS void AS $$
BEGIN
    BEGIN
        EXECUTE stmt;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE <> state THEN
            RAISE EXCEPTION 'FAIL %: ожидался код %, получен % (%)', name, state, SQLSTATE, SQLERRM;
        END IF;
        RAISE NOTICE 'ok   %', name;
        RETURN;
    END;
    RAISE EXCEPTION 'FAIL %: запрос выполнен, а должен был быть отклонён', name;
END $$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.expect(name text, ok boolean) RETURNS void AS $$
BEGIN
    IF ok IS NOT TRUE THEN
        RAISE EXCEPTION 'FAIL %', name;
    END IF;
    RAISE NOTICE 'ok   %', name;
END $$ LANGUAGE plpgsql;

-- 23514 — нарушение CHECK, 23505 — уникальности, 23503 — внешнего ключа
SELECT pg_temp.expect_error('T1 этаж выше этажности',
    $$INSERT INTO property (district_id, address, rooms, area, floor, floors) VALUES (1, 'x', 2, 50, 18, 17)$$, '23514');
SELECT pg_temp.expect_error('T2 нулевая площадь',
    $$INSERT INTO property (district_id, address, rooms, area, floor, floors) VALUES (1, 'x', 2, 0, 1, 5)$$, '23514');
SELECT pg_temp.expect_error('T3 бюджет «от» больше бюджета «до»',
    $$INSERT INTO buyer_request (client_id, agent_id, budget_min, budget_max, rooms_min, rooms_max)
      VALUES (101, 1, 20000000, 10000000, 1, 2)$$, '23514');
SELECT pg_temp.expect_error('T4 второй действующий договор на ту же квартиру',
    $$INSERT INTO seller_contract (property_id, seller_id, agent_id, signed_on, valid_until, list_price, commission_rate)
      VALUES (1, 1, 1, '2026-11-01', '2027-11-01', 10000000, 0.03)$$, '23505');
SELECT pg_temp.expect_error('T5 ставка комиссии выше 10 %',
    $$INSERT INTO seller_contract (property_id, seller_id, agent_id, signed_on, valid_until, list_price, commission_rate, status)
      VALUES (1, 1, 1, '2026-11-01', '2027-11-01', 10000000, 0.15, 'terminated')$$, '23514');
SELECT pg_temp.expect_error('T6 закрытая сделка без даты закрытия',
    $$UPDATE deal SET status = 'closed' WHERE id = 6$$, '23514');
SELECT pg_temp.expect_error('T7 документ неизвестного типа',
    $$INSERT INTO document (deal_id, type_code, received_on) VALUES (6, 'unknown', '2026-11-01')$$, '23503');
SELECT pg_temp.expect_error('T8 вознаграждение агента больше комиссии',
    $$INSERT INTO accrual (deal_id, employee_id, agency_fee, agent_reward, period) VALUES (1, 1, 100, 200, '2026-10-01')$$, '23514');
SELECT pg_temp.expect_error('T9 период начисления не с первого числа',
    $$INSERT INTO accrual (deal_id, employee_id, agency_fee, agent_reward, period) VALUES (1, 1, 200, 100, '2026-10-15')$$, '23514');
SELECT pg_temp.expect_error('T10 доля в праве больше 1',
    $$UPDATE property_owner SET share = 1.5 WHERE property_id = 1$$, '23514');
SELECT pg_temp.expect_error('T11 телефон не в формате +7XXXXXXXXXX',
    $$UPDATE client SET phone = '8 (999) 123-45-67' WHERE id = 1$$, '23514');
SELECT pg_temp.expect_error('T12 удаление района, на который ссылаются квартиры',
    $$DELETE FROM district WHERE id = 1$$, '23503');
SELECT pg_temp.expect_error('T13 условие обязательности документа вне перечня',
    $$INSERT INTO document_type (code, name, required_if, list_order) VALUES ('x', 'x', 'pets', 99)$$, '23514');

SELECT pg_temp.expect('T14 загружены все объекты и предложения',
    (SELECT count(*) FROM property_on_sale) = 100 AND (SELECT count(*) FROM market_listing) = 300);
SELECT pg_temp.expect('T15 статусы объектов: 65 в продаже, 21 забронирован, 14 продано',
    (SELECT array_agg(n ORDER BY status) FROM (SELECT status, count(*) n FROM property_on_sale GROUP BY status) s)
    = ARRAY[65, 21, 14]::bigint[]);
SELECT pg_temp.expect('T16 условия сделки 6: ипотека, продавец в браке, без несовершеннолетних',
    (SELECT (mortgage, married_seller, minor_owners) = (true, true, false) FROM deal_conditions WHERE deal_id = 6));
INSERT INTO property_owner (property_id, client_id, share, is_minor) VALUES (7, 101, 0.5, true);
SELECT pg_temp.expect('T17 несовершеннолетний собственник меняет условия сделки',
    (SELECT minor_owners FROM deal_conditions WHERE deal_id = 6));
DELETE FROM deal WHERE id = 6;
SELECT pg_temp.expect('T18 удаление сделки удаляет её этапы и документы',
    NOT EXISTS (SELECT 1 FROM deal_stage WHERE deal_id = 6) AND NOT EXISTS (SELECT 1 FROM document WHERE deal_id = 6));

SELECT pg_temp.expect('T19 у каждого внешнего ключа есть индекс',
    NOT EXISTS (SELECT 1 FROM pg_constraint c
                WHERE c.contype = 'f' AND c.connamespace = 'realty'::regnamespace
                  AND NOT EXISTS (SELECT 1 FROM pg_index i
                                  WHERE i.indrelid = c.conrelid AND i.indpred IS NULL
                                    AND (i.indkey::int2[])[0:array_length(c.conkey, 1) - 1] = c.conkey)));

ROLLBACK;
\o
\echo 'Все тесты физической модели пройдены'
