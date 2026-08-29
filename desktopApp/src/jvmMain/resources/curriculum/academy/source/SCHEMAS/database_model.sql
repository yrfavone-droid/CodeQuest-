-- Logical PostgreSQL model. The implementation agent must adapt this additively
-- to the existing CodeQuest schema. Never run destructive production statements.

create table if not exists ai_tracks (
  id text primary key, slug text unique not null, title text not null,
  position integer not null, status text not null default 'draft',
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);

create table if not exists ai_modules (
  id text primary key, track_id text not null references ai_tracks(id),
  slug text not null, title text not null, position integer not null,
  status text not null default 'draft', unique(track_id, slug)
);

create table if not exists ai_objectives (
  id text primary key, module_id text not null references ai_modules(id),
  code text unique not null, title text not null, level text not null,
  prerequisite_objective_ids jsonb not null default '[]'::jsonb
);

create table if not exists ai_lessons (
  id uuid primary key, objective_id text not null references ai_objectives(id),
  slug text unique not null, current_version integer not null default 1,
  status text not null default 'draft', created_at timestamptz not null default now()
);

create table if not exists ai_lesson_versions (
  lesson_id uuid not null references ai_lessons(id), version integer not null,
  content jsonb not null, author_id uuid, reviewer_id uuid,
  created_at timestamptz not null default now(), primary key (lesson_id, version)
);

create table if not exists ai_problems (
  id text primary key, objective_id text not null references ai_objectives(id),
  problem_type text not null, difficulty text not null,
  current_version integer not null default 1, status text not null default 'planned',
  created_at timestamptz not null default now()
);

create table if not exists ai_problem_versions (
  problem_id text not null references ai_problems(id), version integer not null,
  content jsonb not null, canonical_answer jsonb not null,
  author_id uuid, reviewer_id uuid, created_at timestamptz not null default now(),
  primary key (problem_id, version)
);

create table if not exists ai_problem_tests (
  id uuid primary key, problem_id text not null references ai_problems(id),
  visibility text not null check (visibility in ('public','hidden')),
  input jsonb, expected_output jsonb, weight numeric not null default 1
);

create table if not exists ai_attempts (
  id uuid primary key, user_id uuid not null, problem_id text not null references ai_problems(id),
  answer jsonb, correct boolean, hints_used integer not null default 0,
  execution jsonb, created_at timestamptz not null default now()
);

create table if not exists ai_mastery (
  user_id uuid not null, objective_id text not null references ai_objectives(id),
  score numeric not null default 0, evidence_count integer not null default 0,
  last_evidence_at timestamptz, next_review_at timestamptz,
  primary key (user_id, objective_id)
);

create table if not exists ai_review_queue (
  user_id uuid not null, objective_id text not null references ai_objectives(id),
  due_at timestamptz not null, reason text not null,
  primary key (user_id, objective_id)
);

create table if not exists ai_books (
  id text primary key, slug text unique not null, title text not null,
  current_version integer not null default 1, status text not null default 'draft'
);

create table if not exists ai_knowledge_files (
  id text primary key, slug text unique not null, title text not null,
  current_version integer not null default 1, status text not null default 'draft',
  core_required boolean not null default false
);

create table if not exists ai_content_qa (
  content_type text not null, content_id text not null, version integer not null,
  check_name text not null, status text not null, details jsonb,
  checked_at timestamptz not null default now(),
  primary key (content_type, content_id, version, check_name)
);

create table if not exists ai_legacy_mappings (
  legacy_content_type text not null, legacy_content_id text not null,
  new_content_type text not null, new_content_id text not null,
  migration_version text not null, created_at timestamptz not null default now(),
  primary key (legacy_content_type, legacy_content_id)
);
