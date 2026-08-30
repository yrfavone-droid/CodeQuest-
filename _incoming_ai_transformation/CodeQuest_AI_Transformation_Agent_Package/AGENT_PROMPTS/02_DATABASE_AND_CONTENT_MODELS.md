# Prompt 02 - Build Database and Content Models

Adapt SCHEMAS/database_model.sql and the JSON schemas to the existing database. Use additive migrations, indexes, authorization policies, version history, draft/review/publish states, and rollback. Add automated migration tests and a dry-run report. Never delete legacy tables or overwrite progress. Seed only track/module/objective metadata and planned content slots; label them accurately. Do not seed fake attempts, users, mastery, or analytics. Run schema validation, authorization tests, and rollback tests.
