# Final Acceptance Checklist

## Migration and integrity

- [ ] Existing users can authenticate.
- [ ] Legacy progress maps correctly for a tested sample and aggregate counts reconcile.
- [ ] No production data was deleted.
- [ ] Rollback was tested.
- [ ] Legacy content remains recoverable until owner approval.

## Learning product

- [ ] Diagnostic, path, lesson, practice, review, checkpoint, and project flows work.
- [ ] Mastery changes only from persisted evidence.
- [ ] Locks and recommendations explain their reason.
- [ ] No fake data or nonfunctional controls are visible.

## Content

- [ ] All published lessons pass schema, technical, editorial, and accessibility review.
- [ ] Exactly 10,000 problem slots exist.
- [ ] Every problem claimed complete contains validated prompt, answer, explanation, hints, provenance, and required tests.
- [ ] Duplicate, correctness, and execution QA reports pass.
- [ ] Five original books render to exactly 100 pages each.
- [ ] Twenty optional deep dives are available in PDF and text.

## Engineering

- [ ] Untrusted code is isolated with time, memory, process, filesystem, and network controls.
- [ ] Lint, typecheck, tests, security checks, accessibility checks, and production build pass.
- [ ] Responsive layouts work on phone, tablet, and desktop.
- [ ] Error monitoring, job monitoring, content defect reporting, and real analytics are active.
- [ ] Secrets are stored only in the deployment secret system.

## Release

- [ ] Staged rollout or feature flags are configured.
- [ ] Backup and rollback instructions are current.
- [ ] Owner has approved retiring legacy routes or content.
