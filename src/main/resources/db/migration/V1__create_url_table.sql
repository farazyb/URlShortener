-- Matches the URL entity mapping. The increment must equal the entity's
-- allocationSize (50) or Hibernate's schema validation fails at startup.
CREATE SEQUENCE url_sequence START WITH 100000 INCREMENT BY 50;

CREATE TABLE url
(
    id           BIGINT PRIMARY KEY,
    original_url VARCHAR(255),
    short_url    VARCHAR(255),
    createdat    TIMESTAMP(6) WITH TIME ZONE
);
