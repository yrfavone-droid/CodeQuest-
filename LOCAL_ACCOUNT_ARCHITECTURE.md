# Local account architecture

Accounts are device-local. `UserProfile` stores a stable local ID, display name, normalized email,
PBKDF2-HMAC-SHA256 password record, timestamps, and profile metadata. `ActiveSession` stores only
the active user ID; it never stores a password or reusable credential.

The shared repository performs transactional creation, sign-in, profile updates, password changes,
and sign-out. All user-owned rows already carry `user_id`, so curriculum remains immutable and
progress is isolated between accounts. Authentication screens resolve to Create Account, Sign In,
or legacy credential setup and never open a browser.

Passwords use 16-byte random salts, 210,000 PBKDF2-HMAC-SHA256 iterations, and 256-bit derived
keys. Parameters are stored with the record so the work factor can be upgraded later.
