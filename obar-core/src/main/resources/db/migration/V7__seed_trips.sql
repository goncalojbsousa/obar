-- Seeds two trips and their routes for admin dashboard usage
-- Trip 1: Serdedelo -> Cabacos
-- Trip 2: Ponte de Lima -> Viana do Castelo

-- Route: Serdedelo -> Cabacos
INSERT INTO routes (
    origin_address,
    destination_address,
    origin_latitude,
    origin_longitude,
    destination_latitude,
    destination_longitude,
    distance_km,
    estimated_duration_min
)
SELECT
    'serdedelo',
    'Cabacos',
    41.525,
    -8.370,
    41.605,
    -8.275,
    15.4,
    24
WHERE NOT EXISTS (
    SELECT 1
    FROM routes
    WHERE lower(origin_address) = 'serdedelo'
      AND lower(destination_address) = 'cabacos'
);

-- Route: Ponte de Lima -> Viana do Castelo
INSERT INTO routes (
    origin_address,
    destination_address,
    origin_latitude,
    origin_longitude,
    destination_latitude,
    destination_longitude,
    distance_km,
    estimated_duration_min
)
SELECT
    'Ponte de Lima',
    'Viana do Castelo',
    41.767,
    -8.584,
    41.693,
    -8.835,
    30.8,
    32
WHERE NOT EXISTS (
    SELECT 1
    FROM routes
    WHERE lower(origin_address) = 'ponte de lima'
      AND lower(destination_address) = 'viana do castelo'
);

-- Trip 1: Completed immediate trip
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
    now() - interval '2 day',
    now() - interval '2 day' + interval '5 minute',
    now() - interval '2 day' + interval '31 minute',
    'COMPLETED',
    'IMMEDIATE',
    'seed_trip_serdedelo_cabacos',
    14.20,
    13.90
FROM users c
JOIN users d ON d.email = 'carlos.driver@gmail.com'
JOIN vehicles v ON v.driver_id = d.id AND v.active = true
JOIN routes r
  ON lower(r.origin_address) = 'serdedelo'
 AND lower(r.destination_address) = 'cabacos'
WHERE c.email = 'custodio@gmail.com'
  AND NOT EXISTS (
      SELECT 1
      FROM trips t
      WHERE t.notes = 'seed_trip_serdedelo_cabacos'
  )
LIMIT 1;

-- Trip 2: In progress scheduled trip
INSERT INTO trips (
    client_id,
    driver_id,
    vehicle_id,
    route_id,
    request_time,
    start_time,
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
    now() - interval '4 hour',
    now() - interval '3 hour 45 minute',
    'IN_PROGRESS',
    'SCHEDULED',
    'seed_trip_ponte_lima_viana_castelo',
    26.50,
    24.80
FROM users c
JOIN users d ON d.email = 'ricardo.driver@gmail.com'
JOIN vehicles v ON v.driver_id = d.id AND v.active = true
JOIN routes r
  ON lower(r.origin_address) = 'ponte de lima'
 AND lower(r.destination_address) = 'viana do castelo'
WHERE c.email = 'humberto@gmail.com'
  AND NOT EXISTS (
      SELECT 1
      FROM trips t
      WHERE t.notes = 'seed_trip_ponte_lima_viana_castelo'
  )
LIMIT 1;

-- Trip driver relation for Trip 1
INSERT INTO trip_drivers (
    trip_id,
    driver_id,
    status,
    assigned_at,
    responded_at
)
SELECT
    t.id,
    d.id,
    'ACCEPTED',
    t.request_time + interval '1 minute',
    t.request_time + interval '2 minute'
FROM trips t
JOIN users d ON d.id = t.driver_id
WHERE t.notes = 'seed_trip_serdedelo_cabacos'
  AND NOT EXISTS (
      SELECT 1
      FROM trip_drivers td
      WHERE td.trip_id = t.id
  );

-- Trip driver relation for Trip 2
INSERT INTO trip_drivers (
    trip_id,
    driver_id,
    status,
    assigned_at,
    responded_at
)
SELECT
    t.id,
    d.id,
    'ACCEPTED',
    t.request_time + interval '1 minute',
    t.request_time + interval '3 minute'
FROM trips t
JOIN users d ON d.id = t.driver_id
WHERE t.notes = 'seed_trip_ponte_lima_viana_castelo'
  AND NOT EXISTS (
      SELECT 1
      FROM trip_drivers td
      WHERE td.trip_id = t.id
  );
