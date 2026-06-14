-- Seeded historical trips should not create a review backlog when a demo
-- client signs in. Real trips remain eligible for the normal review flow.
WITH seed_trips AS (
    SELECT
        t.id,
        t.client_id,
        t.driver_id,
        t.end_time,
        substring(t.notes FROM '([0-9]+)$')::int AS seed_number
    FROM
        trips t
    WHERE
        t.status = 'COMPLETED'
        AND t.client_id IS NOT NULL
        AND t.driver_id IS NOT NULL
        AND (t.notes LIKE 'seed_bulk_trip_%'
            OR t.notes LIKE 'seed_fin_trip_%'
            OR t.notes LIKE 'seed_trip_%'))
INSERT INTO reviews(trip_id, reviewer_id, reviewed_id, rating, comment, created_at, reviewer_type)
SELECT
    t.id,
    t.client_id,
    t.driver_id,
    CASE WHEN coalesce(t.seed_number, t.id) % 11 = 0 THEN
        3
    WHEN coalesce(t.seed_number, t.id) % 4 = 0 THEN
        4
    ELSE
        5
    END,
    'Avaliacao gerada para o historico de demonstracao.',
    coalesce(t.end_time, now()) + interval '20 minutes',
    'CLIENT'
FROM
    seed_trips t
ON CONFLICT (trip_id,
    reviewer_type)
    DO NOTHING;

-- Keep the cached driver statistics aligned with the completed seed reviews.
UPDATE
    users driver
SET
    total_trips = stats.completed_trips,
    average_rating = coalesce(stats.average_rating, driver.average_rating)
FROM (
    SELECT
        d.id AS driver_id,
        count(DISTINCT t.id) FILTER (WHERE t.status = 'COMPLETED')::int AS completed_trips,
        round(avg(r.rating)::numeric, 2)::float AS average_rating
    FROM
        users d
    LEFT JOIN trips t ON t.driver_id = d.id
    LEFT JOIN reviews r ON r.trip_id = t.id
        AND r.reviewed_id = d.id
        AND r.reviewer_type = 'CLIENT'
WHERE
    d.type = 'DRIVER'
GROUP BY
    d.id) stats
WHERE
    driver.id = stats.driver_id;

