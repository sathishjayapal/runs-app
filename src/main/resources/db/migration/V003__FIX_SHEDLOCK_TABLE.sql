-- Fix shedlock table schema to match ShedLock JDBC provider requirements
-- The name column must be VARCHAR(64) to store lock names like "garminFitImportJob"
-- Previous schema incorrectly used BIGINT which prevented lock acquisition

DROP TABLE shedlock;

CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);