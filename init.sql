-- Runs App Database Initialization
-- Flyway migrations will run automatically when the app starts
-- This file ensures the database exists and enables required extensions

-- Enable pgvector for AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Comment: ShedLock table is created by Flyway migration
-- See: src/main/resources/db/migration/V003__FIX_SHEDLOCK_TABLE.sql
