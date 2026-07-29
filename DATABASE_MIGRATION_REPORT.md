# Database migration report

The migration preserves the existing `UserProfile.user_id` and all rows referencing it. New local
credential columns are nullable so old offline or provider-era profiles remain readable. Startup
adds missing columns and a partial normalized-email index idempotently, creates `ActiveSession` if
needed, records migration version `2` in `SchemaMigration`, and never deletes curriculum or learner
rows.

Profiles without a password hash are routed once to `LegacyCredentialSetup`. Submitting local name,
email, and password attaches credentials to the existing ID in one transaction; repeated startup
does not duplicate the profile or rerun setup. A missing/corrupt active session is cleared by
normal routing while all other profiles and progress remain intact.
