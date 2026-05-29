-- Seeds payment methods and payments for financial dashboard testing
-- This script is idempotent and safe to run multiple times.

-- 1) Seed client payment methods used by dashboard method-distribution widgets.
WITH method_seed (client_email, method_type, method_details) AS (
	VALUES
		('custodio@gmail.com', 'Cartao de Credito', 'VISA **** 1204'),
		('custodio@gmail.com', 'MB Way', '+351930111222'),
		('custodio@gmail.com', 'PayPal', 'custodio@paypal.test'),
		('humberto@gmail.com', 'Cartao de Credito', 'Mastercard **** 4421'),
		('humberto@gmail.com', 'MB Way', '+351930333444'),
		('humberto@gmail.com', 'PayPal', 'humberto@paypal.test')
)
INSERT INTO payment_methods (client_id, type, details, active)
SELECT
	u.id,
	m.method_type,
	m.method_details,
	true
FROM method_seed m
JOIN users u ON lower(u.email) = lower(m.client_email)
WHERE NOT EXISTS (
	SELECT 1
	FROM payment_methods pm
	WHERE pm.client_id = u.id
	  AND lower(pm.type) = lower(m.method_type)
	  AND lower(coalesce(pm.details, '')) = lower(coalesce(m.method_details, ''))
);

-- 2) Set a default payment method for seeded clients (if missing).
UPDATE users u
SET default_payment_method_id = pm.id
FROM payment_methods pm
WHERE u.id = pm.client_id
  AND u.default_payment_method_id IS NULL
  AND lower(pm.type) = 'mb way'
  AND lower(u.email) IN ('custodio@gmail.com', 'humberto@gmail.com');

-- 3) Seed additional completed trips to generate realistic daily revenue bars.
WITH trip_seed (
	note,
	client_email,
	driver_email,
	route_origin,
	route_destination,
	request_offset,
	estimated_price,
	final_price
) AS (
	VALUES
		('seed_fin_trip_01', 'custodio@gmail.com', 'carlos.driver@gmail.com', 'serdedelo', 'cabacos', interval '1 day', 15.20, 14.80),
		('seed_fin_trip_02', 'humberto@gmail.com', 'ricardo.driver@gmail.com', 'ponte de lima', 'viana do castelo', interval '2 day', 25.60, 24.40),
		('seed_fin_trip_03', 'custodio@gmail.com', 'ricardo.driver@gmail.com', 'ponte de lima', 'viana do castelo', interval '3 day', 27.10, 26.50),
		('seed_fin_trip_04', 'humberto@gmail.com', 'carlos.driver@gmail.com', 'serdedelo', 'cabacos', interval '4 day', 13.90, 13.60),
		('seed_fin_trip_05', 'custodio@gmail.com', 'carlos.driver@gmail.com', 'serdedelo', 'cabacos', interval '5 day', 16.30, 15.70),
		('seed_fin_trip_06', 'humberto@gmail.com', 'ricardo.driver@gmail.com', 'ponte de lima', 'viana do castelo', interval '6 day', 24.00, 23.20),
		('seed_fin_trip_07', 'custodio@gmail.com', 'ricardo.driver@gmail.com', 'ponte de lima', 'viana do castelo', interval '8 day', 26.80, 25.90),
		('seed_fin_trip_08', 'humberto@gmail.com', 'carlos.driver@gmail.com', 'serdedelo', 'cabacos', interval '10 day', 14.60, 14.10),
		('seed_fin_trip_09', 'custodio@gmail.com', 'carlos.driver@gmail.com', 'serdedelo', 'cabacos', interval '12 day', 15.00, 14.40),
		('seed_fin_trip_10', 'humberto@gmail.com', 'ricardo.driver@gmail.com', 'ponte de lima', 'viana do castelo', interval '14 day', 28.20, 27.10)
)
INSERT INTO trips (
	client_id,
	driver_id,
	vehicle_id,
	route_id,
	request_time,
	start_time,
	end_time,
	status,
	trip_type,
	notes,
	estimated_price,
	final_price
)
SELECT
	c.id,
	d.id,
	v.id,
	r.id,
	now() - ts.request_offset,
	now() - ts.request_offset + interval '4 minute',
	now() - ts.request_offset + interval '29 minute',
	'COMPLETED',
	'IMMEDIATE',
	ts.note,
	ts.estimated_price,
	ts.final_price
FROM trip_seed ts
JOIN users c ON lower(c.email) = lower(ts.client_email)
JOIN users d ON lower(d.email) = lower(ts.driver_email)
JOIN vehicles v ON v.driver_id = d.id AND v.active = true
JOIN routes r
  ON lower(r.origin_address) = lower(ts.route_origin)
 AND lower(r.destination_address) = lower(ts.route_destination)
WHERE NOT EXISTS (
	SELECT 1
	FROM trips t
	WHERE t.notes = ts.note
)
LIMIT 10;

-- 4) Ensure trip_driver links exist for seeded financial trips.
INSERT INTO trip_drivers (trip_id, driver_id, status, assigned_at, responded_at)
SELECT
	t.id,
	t.driver_id,
	'ACCEPTED',
	t.request_time + interval '1 minute',
	t.request_time + interval '2 minute'
FROM trips t
WHERE t.notes LIKE 'seed_fin_trip_%'
  AND t.driver_id IS NOT NULL
  AND NOT EXISTS (
	  SELECT 1
	  FROM trip_drivers td
	  WHERE td.trip_id = t.id
  );

-- 5) Seed payments with mixed statuses and methods for dashboard charts/filters.
WITH payment_seed (trip_note, method_type, payment_status, payment_offset) AS (
	VALUES
		('seed_trip_serdedelo_cabacos', 'MB Way', 'PROCESSED', interval '2 day 35 minute'),
		('seed_trip_ponte_lima_viana_castelo', 'Cartao de Credito', 'PENDING', interval '3 hour'),
		('seed_fin_trip_01', 'Cartao de Credito', 'PROCESSED', interval '1 day 40 minute'),
		('seed_fin_trip_02', 'MB Way', 'PROCESSED', interval '2 day 20 minute'),
		('seed_fin_trip_03', 'PayPal', 'FAILED', interval '3 day 15 minute'),
		('seed_fin_trip_04', 'Cartao de Credito', 'PROCESSED', interval '4 day 12 minute'),
		('seed_fin_trip_05', 'MB Way', 'PROCESSED', interval '5 day 18 minute'),
		('seed_fin_trip_06', 'PayPal', 'REFUNDED', interval '6 day 16 minute'),
		('seed_fin_trip_07', 'Cartao de Credito', 'PROCESSED', interval '8 day 14 minute'),
		('seed_fin_trip_08', 'MB Way', 'FAILED', interval '10 day 10 minute'),
		('seed_fin_trip_09', 'PayPal', 'PROCESSED', interval '12 day 11 minute'),
		('seed_fin_trip_10', 'Cartao de Credito', 'PROCESSED', interval '14 day 9 minute')
),
standard_tax AS (
	SELECT id, rate
	FROM tax_rates
	WHERE lower(name) = 'standard iva'
	ORDER BY id
	LIMIT 1
),
eur_currency AS (
	SELECT id
	FROM currencies
	WHERE code = 'EUR'
	ORDER BY id
	LIMIT 1
)
INSERT INTO payments (
	trip_id,
	payment_method_id,
	amount,
	payment_date,
	status,
	tax_rate_id,
	tax_rate_applied,
	net_amount,
	tax_amount,
	billing_nif,
	billing_name,
	currency_id,
	exchange_rate_to_eur
)
SELECT
	t.id,
	pm.id,
	base.base_amount,
	now() - ps.payment_offset,
	ps.payment_status::payment_status,
	st.id,
	st.rate,
	CASE
		WHEN st.rate IS NULL THEN NULL
		ELSE round((base.base_amount / (1 + st.rate))::numeric, 2)
	END,
	CASE
		WHEN st.rate IS NULL THEN NULL
		ELSE round((base.base_amount - (base.base_amount / (1 + st.rate)))::numeric, 2)
	END,
	c.tax_number,
	c.name,
	ec.id,
	1.000000
FROM payment_seed ps
JOIN trips t ON t.notes = ps.trip_note
JOIN users c ON c.id = t.client_id
JOIN LATERAL (
	SELECT pml.id
	FROM payment_methods pml
	WHERE pml.client_id = c.id
	  AND lower(pml.type) = lower(ps.method_type)
	  AND pml.active = true
	ORDER BY pml.id
	LIMIT 1
) pm ON true
LEFT JOIN standard_tax st ON true
LEFT JOIN eur_currency ec ON true
CROSS JOIN LATERAL (
	SELECT coalesce(t.final_price, t.estimated_price, 0)::numeric(10, 2) AS base_amount
) base
WHERE NOT EXISTS (
	SELECT 1
	FROM payments p
	WHERE p.trip_id = t.id
);
