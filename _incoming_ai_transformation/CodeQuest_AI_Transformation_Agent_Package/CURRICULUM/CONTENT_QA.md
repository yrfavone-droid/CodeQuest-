# Content Quality and Validation Gates

## Status pipeline

planned -> drafted -> automated_checked -> technical_review -> editorial_review -> pilot -> published -> archived

## Automated checks

- JSON schema passes.
- Required fields are present and nonempty.
- IDs and slugs are unique.
- Exact and near-duplicate checks pass.
- Answer and explanation agree.
- Coding examples compile or execute in the target runtime.
- Public and hidden tests pass the canonical solution and reject known wrong solutions.
- Numeric answers meet stated tolerances.
- Markdown, math, code fences, links, and images render.
- Reading level and terminology match the intended level.
- No unsupported claims, unsafe code, leaked secrets, or copyrighted copied passages.

## Human review

Technical reviewer checks correctness, assumptions, edge cases, and solution quality. Editorial reviewer checks clarity, inclusivity, accessibility, and consistency. Pilot data is used to revise ambiguous or misleading items.

## Release gates

- zero known wrong answers;
- zero broken hidden tests;
- duplicate rate below 1 percent within a module;
- all advanced items have prerequisites;
- all published content has reviewer and version metadata;
- defect reporting and rollback work in production.
