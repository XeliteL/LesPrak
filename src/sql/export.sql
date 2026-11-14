-- Выгрузка данных из БД в формате src/python/data/sample.json для процедурной и ООП-реализаций.
-- Запуск: psql -d realty -At -f export.sql > data.json
SET search_path TO realty;

SELECT jsonb_pretty(jsonb_build_object(
    'listings', (SELECT jsonb_agg(jsonb_build_object(
                    'id', m.id, 'district', d.name, 'rooms', m.rooms, 'area', m.area, 'floor', m.floor,
                    'floors', m.floors, 'year_built', m.year_built, 'price', m.price, 'date', m.published_on)
                    ORDER BY m.id)
                 FROM market_listing m JOIN district d ON d.id = m.district_id),
    'objects', (SELECT jsonb_agg(jsonb_build_object(
                    'id', p.id, 'district', p.district, 'rooms', p.rooms, 'area', p.area, 'floor', p.floor,
                    'floors', p.floors, 'year_built', p.year_built, 'price', p.price, 'status', p.status)
                    ORDER BY p.id)
                FROM property_on_sale p),
    'requests', (SELECT jsonb_agg(jsonb_strip_nulls(jsonb_build_object(
                    'id', r.id, 'client', c.full_name, 'budget_min', r.budget_min, 'budget_max', r.budget_max,
                    'rooms', (SELECT jsonb_agg(n) FROM generate_series(r.rooms_min::int, r.rooms_max::int) n),
                    'districts', coalesce((SELECT jsonb_agg(d.name ORDER BY rd.priority)
                                           FROM request_district rd JOIN district d ON d.id = rd.district_id
                                           WHERE rd.request_id = r.id), '[]'),
                    'area_min', r.area_min, 'not_first_floor', r.not_first_floor,
                    'not_last_floor', r.not_last_floor))
                    ORDER BY r.id)
                 FROM buyer_request r JOIN client c ON c.id = r.client_id
                 WHERE r.status = 'open'),
    'doc_types', (SELECT jsonb_agg(jsonb_strip_nulls(jsonb_build_object(
                    'code', t.code, 'name', t.name, 'required_if', t.required_if,
                    'validity_days', t.validity_days)) ORDER BY t.list_order)
                  FROM document_type t),
    'deals', (SELECT jsonb_agg(jsonb_strip_nulls(jsonb_build_object(
                    'id', dl.id, 'agent_id', sc.agent_id, 'price', dl.price, 'rate', sc.commission_rate,
                    'status', dl.status, 'closed', dl.closed_on, 'signing_date', dl.signing_on))
                  || CASE WHEN dl.status = 'in_progress' THEN jsonb_build_object(
                    'conditions', jsonb_build_object('mortgage', dc.mortgage, 'married_seller', dc.married_seller,
                                                     'minor_owners', dc.minor_owners),
                    'stages', coalesce((SELECT jsonb_agg(jsonb_build_object(
                                            'name', s.name, 'due', s.due_on, 'done', s.done_on IS NOT NULL)
                                            ORDER BY s.due_on, s.id)
                                        FROM deal_stage s WHERE s.deal_id = dl.id), '[]'),
                    'documents', coalesce((SELECT jsonb_agg(jsonb_build_object(
                                               'type', doc.type_code, 'received', doc.received_on) ORDER BY doc.id)
                                           FROM document doc WHERE doc.deal_id = dl.id), '[]'))
                    ELSE '{}' END
                  ORDER BY dl.id)
              FROM deal dl
              JOIN seller_contract sc ON sc.id = dl.contract_id
              JOIN deal_conditions dc ON dc.deal_id = dl.id)
));
