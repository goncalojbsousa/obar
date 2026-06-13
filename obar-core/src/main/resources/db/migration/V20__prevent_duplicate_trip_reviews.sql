ALTER TABLE reviews
    ADD CONSTRAINT uq_reviews_trip_reviewer_type UNIQUE (trip_id, reviewer_type);
