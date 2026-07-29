# Curriculum architecture

Curriculum JSON is immutable, versioned, and packaged in shared Compose resources. The JVM reader
loads the ten path assets, validates the catalog manifest, preserves nested lesson practice and
challenge objects, and seeds only normalized curriculum rows. Learner state stays in SQLite
(`NodeProgress`, attempts, activities, drafts, mastery, settings) and is keyed by the active local
profile.

The learning map is built from real timeline nodes, in order: diagnostic, cheat sheet, each
lesson/practice/challenge chain, two mixed reviews, adaptive review, final quiz, project,
reflection, and optional mastery challenge. Required-node state controls unlocking; optional
mastery never blocks progression. The immutable asset IDs are retained, including underscore track
IDs and all level/node IDs.
