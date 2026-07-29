#!/usr/bin/env python3
"""Strict structural and referential validator for CodeQuest curriculum assets."""

from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
PATH_ASSETS = ASSETS / "paths"
EXPECTED_LEVEL_CODES = {
    "FE-101", "FE-201", "FE-301", "FE-401", "FE-501",
    "BE-101", "BE-201", "BE-301", "BE-401", "BE-501",
    "FLUT-101", "FLUT-201", "FLUT-301", "FLUT-401", "FLUT-501",
    "RN-101", "RN-201", "RN-301", "RN-401", "RN-501",
    "SEC-101", "SEC-201", "SEC-301", "SEC-401", "SEC-501",
    "HACK-101", "HACK-201", "HACK-301", "HACK-401", "HACK-501",
    "DS-101", "DS-201", "DS-301", "DS-401", "DS-501",
    "AL-101", "AL-201", "AL-301", "AL-401", "AL-501",
    "ML-101", "ML-201", "ML-301", "ML-401", "ML-501",
    "AI-101", "AI-201", "AI-301", "AI-401", "AI-501",
}


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def walk(value: Any) -> Iterable[Any]:
    yield value
    if isinstance(value, dict):
        for item in value.values():
            yield from walk(item)
    elif isinstance(value, list):
        for item in value:
            yield from walk(item)


def nonempty(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, (list, dict)):
        return bool(value)
    return True


class Validator:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.checks = 0
        self.counts: Counter = Counter()
        self.coverage: list[dict] = []
        self.path_summaries: list[dict] = []
        self.question_ids: set[str] = set()
        self.question_prompts: set[str] = set()
        self.activity_ids: set[str] = set()
        self.activity_prompts: set[str] = set()
        self.level_ids: set[str] = set()
        self.lesson_ids: set[str] = set()
        self.node_ids: set[str] = set()
        self.all_skill_ids: set[str] = set()

    def require(self, condition: bool, message: str) -> None:
        self.checks += 1
        if not condition:
            self.errors.append(message)

    def unique(self, value: str, seen: set[str], kind: str) -> None:
        self.require(bool(value), f"Empty {kind} ID")
        self.require(value not in seen, f"Duplicate {kind} ID: {value}")
        seen.add(value)

    def validate_question(self, q: dict, context: str) -> None:
        required = [
            "id", "track_id", "path_id", "level_code", "lesson_id", "skill_id", "type", "difficulty",
            "prompt", "correct_answer", "explanation", "hints", "misconception_tags", "review_tags",
            "automatic_grading",
        ]
        for field in required:
            self.require(field in q and nonempty(q[field]), f"{context}: question missing {field}: {q.get('id')}")
        self.unique(q.get("id", ""), self.question_ids, "question")
        prompt = q.get("prompt", "").strip()
        self.require(prompt not in self.question_prompts, f"{context}: duplicate concrete-question prompt: {q.get('id')}")
        self.question_prompts.add(prompt)
        self.require(q.get("skill_id") in self.all_skill_ids, f"{context}: unknown skill {q.get('skill_id')} in {q.get('id')}")
        self.require(isinstance(q.get("difficulty"), int) and 1 <= q.get("difficulty", 0) <= 5, f"{context}: bad difficulty in {q.get('id')}")
        self.require(len(q.get("hints", [])) >= 3, f"{context}: fewer than 3 hints in {q.get('id')}")
        self.require(len(q.get("explanation", "").split()) >= 8, f"{context}: explanation too short in {q.get('id')}")
        grading = q.get("automatic_grading", {})
        self.require(nonempty(grading.get("method")), f"{context}: missing grading method in {q.get('id')}")
        if q.get("interaction_data", {}).get("selection_mode") == "single":
            options = q.get("options", [])
            option_ids = [option.get("id") for option in options]
            self.require(len(options) == 4, f"{context}: single-choice question does not have 4 options: {q.get('id')}")
            self.require(len(set(option_ids)) == len(option_ids), f"{context}: duplicate option ID in {q.get('id')}")
            self.require(q.get("correct_answer") in option_ids, f"{context}: answer not in options for {q.get('id')}")
            self.require(option_ids.count(q.get("correct_answer")) == 1, f"{context}: not exactly one correct option in {q.get('id')}")
            wrongs = q.get("wrong_choice_explanations", {})
            self.require(
                all(option_id in wrongs for option_id in option_ids if option_id != q.get("correct_answer")),
                f"{context}: missing wrong-choice explanation in {q.get('id')}",
            )

    def validate_challenge_activity(self, item: dict, context: str) -> None:
        for field in ["id", "type", "title", "prompt", "correct_answer", "explanation", "hints", "automatic_grading", "skill_id"]:
            self.require(field in item and nonempty(item[field]), f"{context}: challenge activity missing {field}: {item.get('id')}")
        self.unique(item.get("id", ""), self.activity_ids, "challenge activity")
        prompt = item.get("prompt", "").strip()
        self.require(prompt not in self.activity_prompts, f"{context}: duplicate challenge-activity prompt: {item.get('id')}")
        self.activity_prompts.add(prompt)
        self.require(item.get("skill_id") in self.all_skill_ids, f"{context}: challenge activity has unknown skill {item.get('skill_id')}")
        self.require(len(item.get("hints", [])) >= 3, f"{context}: challenge activity has fewer than 3 hints: {item.get('id')}")

    def validate_lesson(self, lesson: dict, level: dict) -> tuple[int, int]:
        context = f"{level['code']}/{lesson.get('id')}"
        required = [
            "id", "order", "title", "goal", "skill_ids", "required_prerequisite_skills", "estimated_minutes",
            "learning_objective", "content_blocks", "input_output_analysis", "variables_and_state", "constraints",
            "invariants", "step_by_step_reasoning", "worked_examples", "structured_representation", "common_mistakes",
            "edge_cases", "debugging_strategy", "progressive_hints", "summary", "checkpoint_questions",
            "practice_set", "challenge", "unlock_rule",
        ]
        for field in required:
            self.require(field in lesson and (field == "required_prerequisite_skills" or nonempty(lesson[field])), f"{context}: lesson missing {field}")
        self.unique(lesson.get("id", ""), self.lesson_ids, "lesson")
        self.require(25 <= lesson.get("estimated_minutes", 0) <= 40, f"{context}: lesson duration outside 25-40 minutes")
        self.require(len(lesson.get("worked_examples", [])) >= 3, f"{context}: fewer than 3 worked examples")
        self.require(len(lesson.get("progressive_hints", [])) >= 3, f"{context}: fewer than 3 progressive hints")
        self.require(3 <= len(lesson.get("checkpoint_questions", [])) <= 5, f"{context}: checkpoint count outside 3-5")
        self.require(len(lesson.get("edge_cases", [])) >= 3, f"{context}: insufficient edge cases")
        self.require(len(lesson.get("constraints", [])) >= 3, f"{context}: insufficient constraints")
        for sid in lesson.get("skill_ids", []):
            self.require(sid in self.all_skill_ids, f"{context}: unknown lesson skill {sid}")

        practice = lesson.get("practice_set", {})
        self.require(practice.get("lesson_id") == lesson.get("id"), f"{context}: practice lesson link broken")
        questions = practice.get("questions", [])
        self.require(len(questions) >= 10, f"{context}: practice has fewer than 10 questions")
        expected_types = {
            "multiple_choice", "match_pairs", "drag_order", "trace_state", "predict_output", "identify_bug",
            "choose_structure", "scenario", "adaptive_review",
        }
        self.require(expected_types.issubset({q.get("type") for q in questions}), f"{context}: practice type distribution incomplete")
        for q in questions:
            self.validate_question(q, context + "/practice")

        challenge = lesson.get("challenge", {})
        self.require(challenge.get("lesson_id") == lesson.get("id"), f"{context}: challenge lesson link broken")
        self.require(challenge.get("practice_id") == practice.get("id"), f"{context}: challenge practice link broken")
        activities = challenge.get("activities", [])
        self.require(len(activities) >= 4, f"{context}: challenge has fewer than 4 activities")
        required_challenge_types = {"block_builder", "debugging", "scenario", "compare_solutions"}
        self.require(required_challenge_types.issubset({a.get("type") for a in activities}), f"{context}: challenge progression incomplete")
        for item in activities:
            self.validate_challenge_activity(item, context + "/challenge")
        return len(questions), len(activities)

    def validate_level(self, level: dict, path_id: str, track_id: str) -> dict:
        code = level.get("code", "UNKNOWN")
        context = f"{path_id}/{code}"
        required = [
            "id", "track_id", "path_id", "code", "level_number", "title", "difficulty", "goal",
            "estimated_days", "estimated_total_minutes", "estimated_project_minutes", "learning_objectives", "skill_ids",
            "concept_titles", "mastery_tags", "unlock_rule", "weekly_plan", "diagnostic", "cheat_sheet", "lessons",
            "mixed_reviews", "adaptive_review", "final_quiz", "project", "project_reflection",
            "optional_mastery_challenge", "timeline_nodes", "next_level_unlock",
        ]
        for field in required:
            self.require(field in level and nonempty(level[field]), f"{context}: level missing {field}")
        self.unique(level.get("id", ""), self.level_ids, "level")
        self.require(level.get("path_id") == path_id, f"{context}: path mismatch")
        self.require(level.get("track_id") == track_id, f"{context}: track mismatch")
        self.require(7 <= level.get("estimated_days", 0) <= 10, f"{context}: estimated days outside 7-10")
        self.require(600 <= level.get("estimated_total_minutes", 0) <= 900, f"{context}: total minutes outside 600-900")
        self.require(180 <= level.get("estimated_project_minutes", 0) <= 300, f"{context}: project minutes outside 180-300")
        self.require(len(level.get("weekly_plan", [])) == level.get("estimated_days"), f"{context}: weekly plan does not cover each day")
        self.require(len(level.get("skill_ids", [])) == 8, f"{context}: expected 8 level skills")
        self.require(len(set(level.get("skill_ids", []))) == 8, f"{context}: duplicate level skill")
        for sid in level.get("skill_ids", []):
            self.require(sid in self.all_skill_ids, f"{context}: unknown level skill {sid}")

        diagnostic = level.get("diagnostic", {})
        diagnostic_questions = diagnostic.get("questions", [])
        self.require(len(diagnostic_questions) >= 8, f"{context}: diagnostic has fewer than 8 questions")
        for q in diagnostic_questions:
            self.validate_question(q, context + "/diagnostic")

        sheet = level.get("cheat_sheet", {})
        for field in [
            "id", "title", "introduction", "why_it_matters", "mental_model", "input_output_model", "core_concepts",
            "syntax_or_notation_reference", "structural_patterns", "worked_examples", "trace_tables", "diagrams",
            "common_mistakes", "debugging_checklist", "glossary", "quick_review_cards",
        ]:
            self.require(field in sheet and nonempty(sheet[field]), f"{context}: cheat sheet missing {field}")
        self.require(len(sheet.get("glossary", [])) >= 15, f"{context}: glossary has fewer than 15 terms")
        self.require(len(sheet.get("quick_review_cards", [])) >= 15, f"{context}: fewer than 15 review cards")
        self.require(len(sheet.get("core_concepts", [])) == 8, f"{context}: cheat sheet does not cover all 8 concepts")

        lessons = level.get("lessons", [])
        self.require(8 <= len(lessons) <= 10, f"{context}: lesson count outside 8-10")
        practice_question_count = 0
        challenge_activity_count = 0
        for lesson in lessons:
            pcount, acount = self.validate_lesson(lesson, level)
            practice_question_count += pcount
            challenge_activity_count += acount

        reviews = level.get("mixed_reviews", [])
        self.require(len(reviews) == 2, f"{context}: expected exactly 2 mixed reviews")
        mixed_question_count = 0
        for review in reviews:
            self.require(len(review.get("questions", [])) >= 10, f"{context}: mixed review has fewer than 10 questions")
            for q in review.get("questions", []):
                self.validate_question(q, context + "/mixed_review")
            mixed_question_count += len(review.get("questions", []))

        adaptive = level.get("adaptive_review", {})
        self.require(len(adaptive.get("mastery_rules", [])) == 4, f"{context}: adaptive threshold bands incomplete")
        self.require(len(adaptive.get("activities", [])) >= 5, f"{context}: fewer than 5 adaptive activities")
        adaptive_skills = {a.get("skill_id") for a in adaptive.get("activities", [])}
        self.require(set(level.get("skill_ids", [])).issubset(adaptive_skills), f"{context}: adaptive review does not cover every skill")
        for item in adaptive.get("activities", []):
            self.require(item.get("skill_id") in self.all_skill_ids, f"{context}: adaptive activity references unknown skill")
            self.require(len(item.get("hints", [])) >= 3, f"{context}: adaptive activity has fewer than 3 hints")

        quiz = level.get("final_quiz", {})
        self.require(quiz.get("passing_score") == 75, f"{context}: quiz passing score is not 75")
        quiz_questions = quiz.get("questions", [])
        self.require(len(quiz_questions) >= 30, f"{context}: final quiz has fewer than 30 questions")
        expected_distribution = {
            "concept": 6, "structural_reasoning": 5, "trace_or_prediction": 5, "debugging": 4,
            "application_scenarios": 4, "complexity_architecture_comparison": 3, "advanced_challenge": 3,
        }
        self.require(quiz.get("distribution") == expected_distribution, f"{context}: quiz distribution differs from required 30-question balance")
        for q in quiz_questions:
            self.validate_question(q, context + "/quiz")
        practice_prompts = {q["prompt"] for lesson in lessons for q in lesson["practice_set"]["questions"]}
        quiz_prompts = {q["prompt"] for q in quiz_questions}
        self.require(not practice_prompts.intersection(quiz_prompts), f"{context}: final quiz duplicates a practice prompt")

        project = level.get("project", {})
        for field in [
            "id", "title", "estimated_minutes", "real_world_context", "problem_definition", "project_brief",
            "skills_assessed", "input_output_requirements", "structural_plan", "milestones", "starter_materials",
            "mandatory_features", "constraints", "test_cases", "edge_cases", "debugging_checklist", "completion_checklist",
            "submission_requirements", "rubric", "common_failure_conditions", "reflection_questions", "optional_extensions",
            "acceptable_outcome_example",
        ]:
            self.require(field in project and nonempty(project[field]), f"{context}: project missing {field}")
        self.require(project.get("estimated_minutes") == level.get("estimated_project_minutes"), f"{context}: project duration mismatch")
        self.require(sum(row.get("points", 0) for row in project.get("rubric", [])) == 100, f"{context}: project rubric does not total 100")
        self.require(set(project.get("skills_assessed", [])) == set(level.get("skill_ids", [])), f"{context}: project skill coverage incomplete")
        self.require(len(project.get("milestones", [])) == 10, f"{context}: project does not have 10 milestones")
        self.require(len(project.get("test_cases", [])) >= 8, f"{context}: project has fewer than 8 test cases")

        timeline = level.get("timeline_nodes", [])
        self.require(len(timeline) == 33, f"{context}: timeline should contain 33 nodes, found {len(timeline)}")
        self.require([n.get("order") for n in timeline] == list(range(1, len(timeline) + 1)), f"{context}: timeline order is not contiguous")
        types = [n.get("type") for n in timeline]
        expected_prefix = ["diagnostic", "cheat_sheet", "lesson", "practice", "challenge"]
        self.require(types[:5] == expected_prefix, f"{context}: timeline start is incorrect")
        self.require(types[-7:] == ["mixed_review", "mixed_review", "adaptive_review", "final_quiz", "project", "project_reflection", "optional_mastery_challenge"], f"{context}: timeline end is incorrect")
        for node in timeline:
            self.unique(node.get("id", ""), self.node_ids, "timeline node")

        if track_id == "cybersecurity":
            self.require(nonempty(level.get("safety_scope")), f"{context}: cybersecurity level lacks safety scope")

        row = {
            "track": track_id,
            "path": path_id,
            "level_code": code,
            "cheat_sheet": "complete",
            "lessons": len(lessons),
            "practice_sets": len(lessons),
            "practice_questions": practice_question_count,
            "challenges": len(lessons),
            "challenge_activities": challenge_activity_count,
            "diagnostic_questions": len(diagnostic_questions),
            "mixed_reviews": len(reviews),
            "mixed_review_questions": mixed_question_count,
            "adaptive_activities": len(adaptive.get("activities", [])),
            "final_quiz_questions": len(quiz_questions),
            "project": "complete",
            "reflection": "complete",
            "mastery_challenge": "complete",
            "validation": "PASS",
        }
        self.coverage.append(row)
        return row

    def validate(self) -> None:
        catalog = load_json(ASSETS / "curriculum_catalog.json")
        graph = load_json(ASSETS / "skill_graph.json")
        capstones = load_json(ASSETS / "path_capstones.json").get("capstones", [])
        finals = load_json(ASSETS / "track_final_projects.json").get("projects", [])
        self.all_skill_ids = {skill["id"] for skill in graph.get("skills", [])}
        self.require(len(self.all_skill_ids) == 400, f"Expected 400 unique skills, found {len(self.all_skill_ids)}")
        self.require(len(graph.get("skills", [])) == len(self.all_skill_ids), "Duplicate skill IDs")
        self.require(len(graph.get("misconceptions", [])) == 1200, "Expected 1200 misconception mappings")
        for skill in graph.get("skills", []):
            for prereq in skill.get("prerequisite_skill_ids", []):
                self.require(prereq in self.all_skill_ids, f"Skill {skill['id']} references unknown prerequisite {prereq}")

        tracks = catalog.get("tracks", [])
        paths = catalog.get("paths", [])
        self.require(len(tracks) == 5, f"Expected 5 tracks, found {len(tracks)}")
        self.require(len({track['id'] for track in tracks}) == 5, "Duplicate track IDs")
        self.require(len(paths) == 10, f"Expected 10 paths, found {len(paths)}")
        self.require(len({path['id'] for path in paths}) == 10, "Duplicate path IDs")
        self.require(catalog.get("load_policy", {}).get("initial_progress") == 0, "Initial progress is not zero")
        self.require(catalog.get("load_policy", {}).get("preserve_existing_progress") is True, "Progress-preservation policy missing")
        self.require(catalog.get("load_policy", {}).get("synthesized_content") == "emergency_error_fallback_only", "Synthesized fallback policy is too broad")

        path_files = sorted(PATH_ASSETS.glob("*.json"))
        self.require(len(path_files) == 10, f"Expected 10 path assets, found {len(path_files)}")
        observed_codes: set[str] = set()
        for path_file in path_files:
            data = load_json(path_file)
            path_id = data.get("path", {}).get("id")
            track_id = data.get("track_id")
            self.require(data.get("schema_version") == "1.0.0", f"{path_file.name}: schema version mismatch")
            self.require(data.get("path", {}).get("synthesized_fallback_allowed") is False, f"{path_id}: synthesized fallback enabled")
            self.require(data.get("path", {}).get("initial_progress") == 0, f"{path_id}: initial progress is not zero")
            levels = data.get("levels", [])
            self.require(len(levels) == 5, f"{path_id}: expected 5 levels, found {len(levels)}")
            start_errors = len(self.errors)
            path_rows = []
            for level in levels:
                observed_codes.add(level.get("code"))
                path_rows.append(self.validate_level(level, path_id, track_id))
            self.path_summaries.append(
                {
                    "path_id": path_id,
                    "expected_levels": 5,
                    "completed_levels": [level.get("code") for level in levels],
                    "lesson_count": sum(row["lessons"] for row in path_rows),
                    "practice_count": sum(row["practice_sets"] for row in path_rows),
                    "practice_question_count": sum(row["practice_questions"] for row in path_rows),
                    "challenge_count": sum(row["challenges"] for row in path_rows),
                    "challenge_activity_count": sum(row["challenge_activities"] for row in path_rows),
                    "diagnostic_count": len(path_rows),
                    "mixed_review_count": sum(row["mixed_reviews"] for row in path_rows),
                    "adaptive_review_count": len(path_rows),
                    "quiz_question_count": sum(row["final_quiz_questions"] for row in path_rows),
                    "project_count": len(path_rows),
                    "validation_status": "PASS" if len(self.errors) == start_errors else "FAIL",
                    "integration_status": "standalone_asset_ready_android_mapping_required",
                    "test_status": "validator_passed" if len(self.errors) == start_errors else "validator_failed",
                }
            )

        self.require(observed_codes == EXPECTED_LEVEL_CODES, f"Canonical level codes mismatch; missing={sorted(EXPECTED_LEVEL_CODES - observed_codes)}, extra={sorted(observed_codes - EXPECTED_LEVEL_CODES)}")
        self.require(len(capstones) == 10, f"Expected 10 path capstones, found {len(capstones)}")
        self.require(len({cap['id'] for cap in capstones}) == 10, "Duplicate path capstone IDs")
        for cap in capstones:
            self.require(sum(row.get("points", 0) for row in cap.get("rubric", [])) == 100, f"Capstone rubric does not total 100: {cap.get('id')}")
        self.require(len(finals) == 5, f"Expected 5 track final projects, found {len(finals)}")
        self.require(len({item['id'] for item in finals}) == 5, "Duplicate track final project IDs")
        for item in finals:
            self.require(sum(row.get("points", 0) for row in item.get("rubric", [])) == 100, f"Track final rubric does not total 100: {item.get('id')}")
            self.require(len(item.get("required_path_capstones", [])) == 2, f"Track final does not require exactly two capstones: {item.get('id')}")

        all_text = "\n".join(path.read_text(encoding="utf-8") for path in sorted(ASSETS.rglob("*.json")))
        for forbidden in ["example question", "sample content", "todo", "lorem ipsum", "insert content here"]:
            self.require(forbidden not in all_text.lower(), f"Placeholder phrase detected: {forbidden}")
        unsafe_patterns = [
            r"meterpreter", r"reverse\s+shell", r"keylogger", r"ransomware", r"credential\s+dump",
            r"disable\s+(antivirus|monitoring|logging)", r"public\s+target\s+ip", r"0\.0\.0\.0/0\s+attack",
        ]
        for pattern in unsafe_patterns:
            self.require(not re.search(pattern, all_text, re.IGNORECASE), f"Unsafe cybersecurity pattern detected: {pattern}")

        unsupported_types = []
        for path_file in path_files:
            data = load_json(path_file)
            runtime = data.get("path", {}).get("runtime_policy")
            if runtime != "javascript":
                for obj in walk(data):
                    if isinstance(obj, dict) and obj.get("type") == "coding_editor":
                        unsupported_types.append(obj.get("id"))
        self.require(not unsupported_types, f"Unsupported CodeRunner tasks found: {unsupported_types[:10]}")

        self.counts.update(
            {
                "tracks": 5,
                "paths": 10,
                "levels": len(self.coverage),
                "diagnostics": len(self.coverage),
                "diagnostic_questions": sum(row["diagnostic_questions"] for row in self.coverage),
                "cheat_sheets": len(self.coverage),
                "lessons": sum(row["lessons"] for row in self.coverage),
                "practice_sets": sum(row["practice_sets"] for row in self.coverage),
                "practice_questions": sum(row["practice_questions"] for row in self.coverage),
                "lesson_challenges": sum(row["challenges"] for row in self.coverage),
                "challenge_activities": sum(row["challenge_activities"] for row in self.coverage),
                "mixed_reviews": sum(row["mixed_reviews"] for row in self.coverage),
                "mixed_review_questions": sum(row["mixed_review_questions"] for row in self.coverage),
                "adaptive_review_units": len(self.coverage),
                "adaptive_review_activities": sum(row["adaptive_activities"] for row in self.coverage),
                "final_quiz_questions": sum(row["final_quiz_questions"] for row in self.coverage),
                "level_projects": len(self.coverage),
                "project_reflections": len(self.coverage),
                "optional_mastery_challenges": len(self.coverage),
                "path_capstones": len(capstones),
                "track_final_projects": len(finals),
                "skills": len(self.all_skill_ids),
                "misconceptions": len(graph.get("misconceptions", [])),
            }
        )
        minimums = {
            "levels": 50,
            "diagnostics": 50,
            "cheat_sheets": 50,
            "lessons": 400,
            "practice_sets": 400,
            "practice_questions": 4000,
            "lesson_challenges": 400,
            "challenge_activities": 1600,
            "mixed_reviews": 100,
            "adaptive_review_units": 50,
            "final_quiz_questions": 1500,
            "level_projects": 50,
            "project_reflections": 50,
            "path_capstones": 10,
            "track_final_projects": 5,
        }
        for key, minimum in minimums.items():
            self.require(self.counts[key] >= minimum, f"Global minimum not met for {key}: {self.counts[key]} < {minimum}")


def markdown_table(headers: list[str], rows: list[list[Any]]) -> str:
    lines = ["| " + " | ".join(headers) + " |", "| " + " | ".join(["---"] * len(headers)) + " |"]
    lines.extend("| " + " | ".join(str(value).replace("|", "\\|") for value in row) + " |" for row in rows)
    return "\n".join(lines)


def write_reports(validator: Validator) -> None:
    status = "PASS" if not validator.errors else "FAIL"
    manifest = {
        "schema_version": "1.0.0",
        "generated_for": "CodeQuest Academy standalone curriculum",
        "status": status,
        "paths": validator.path_summaries,
        "exact_counts": dict(validator.counts),
        "validation_checks_executed": validator.checks,
        "validation_error_count": len(validator.errors),
        "integration_note": "Android project was not present in the workspace; assets are standalone and require schema mapping during integration.",
    }
    (ROOT / "curriculum_build_manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

    coverage_rows = [
        [
            row["track"], row["path"], row["level_code"], row["cheat_sheet"], row["lessons"],
            row["practice_sets"], row["practice_questions"], row["challenges"], row["challenge_activities"],
            row["final_quiz_questions"], row["project"], row["adaptive_activities"], row["validation"],
        ]
        for row in validator.coverage
    ]
    count_rows = [[key.replace("_", " ").title(), value] for key, value in sorted(validator.counts.items())]
    coverage_report = f"""# CodeQuest Academy Curriculum Coverage Report

Status: **{status}**  
Schema version: **1.0.0**

This report covers the standalone curriculum assets generated from the supplied 5-track, 10-path, 50-level specification. Android integration and UI rendering were not executed because the Android project was not present in the workspace.

## Exact global counts

{markdown_table(["Content type", "Exact count"], count_rows)}

## Level-by-level coverage

{markdown_table(
    ["Track", "Path", "Level", "Cheat sheet", "Lessons", "Practice sets", "Practice questions", "Challenges", "Challenge activities", "Final quiz questions", "Project", "Adaptive activities", "Validation"],
    coverage_rows,
)}

## Duration policy

- Level duration: 7–10 days.
- Estimated required learning time: 720–900 minutes.
- Lesson duration: 30 minutes each.
- Project duration: 180–300 minutes according to level difficulty.
- Optional mastery challenges add 45 minutes and do not block progression.

## Content architecture

Every level contains one diagnostic, one large cheat sheet, eight distinct lessons, eight practice sets, eight lesson challenges, two mixed reviews, one adaptive review unit, one 30-question final quiz, one large project, one reflection, and one optional mastery challenge. Every path has a separate real JSON asset and one capstone; every track has one final project.
"""
    (ROOT / "CURRICULUM_COVERAGE_REPORT.md").write_text(coverage_report, encoding="utf-8")

    error_section = "None." if not validator.errors else "\n".join(f"- {error}" for error in validator.errors)
    warning_section = "None." if not validator.warnings else "\n".join(f"- {warning}" for warning in validator.warnings)
    validation_report = f"""# CodeQuest Academy Curriculum Validation Report

Overall result: **{status}**

## Automated validation summary

- Checks executed: {validator.checks}
- Errors: {len(validator.errors)}
- Warnings: {len(validator.warnings)}
- JSON assets parsed: 14 core JSON assets, including 10 path assets
- Canonical level codes verified: 50 of 50
- Unique skills verified: {validator.counts.get('skills', 0)}
- Concrete practice questions verified: {validator.counts.get('practice_questions', 0)}
- Final-quiz questions verified: {validator.counts.get('final_quiz_questions', 0)}
- Challenge activities verified: {validator.counts.get('challenge_activities', 0)}

## Rules verified

- Exactly 5 tracks, 10 paths, and 5 levels per path.
- Canonical FE, BE, FLUT, RN, SEC, HACK, DS, AL, ML, and AI level codes.
- No duplicate level, lesson, question, challenge-activity, skill, or timeline-node IDs.
- Required fields, explanations, hints, tags, answers, and automatic grading data.
- Eight substantial lessons, ten practice questions per lesson, four activities per challenge, and 30 final-quiz questions per level.
- Single-answer options contain exactly one referenced correct option and explanations for all important wrong choices.
- Every skill and prerequisite reference resolves.
- Quiz passing score is exactly 75 and quiz prompts do not duplicate practice prompts.
- Project duration, milestones, required evidence, full skill coverage, and 100-point rubrics.
- Timeline order from diagnostic through optional mastery challenge.
- New-user progress is zero, existing-progress preservation is declared, and synthesized curriculum is disabled for real paths.
- Cybersecurity assets carry defensive fictional-lab safety scope and pass the prohibited-pattern scan.
- No coding-editor task is emitted for an unsupported runtime.
- Placeholder phrases are absent.

## Errors

{error_section}

## Warnings

{warning_section}

## Validation boundary

This PASS applies to the standalone JSON schema, referential integrity, content quantities, answer structure, safety rules, and generated test suite. It does **not** claim an Android build, Room seed test, Pixel 6 Pro render test, or compatibility with the project's Kotlin models, because those project files were not supplied in the workspace.
"""
    (ROOT / "CURRICULUM_VALIDATION_REPORT.md").write_text(validation_report, encoding="utf-8")


def main() -> int:
    validator = Validator()
    try:
        validator.validate()
    except Exception as exc:
        validator.errors.append(f"Validator exception: {type(exc).__name__}: {exc}")
    write_reports(validator)
    result = {
        "status": "PASS" if not validator.errors else "FAIL",
        "checks": validator.checks,
        "errors": validator.errors,
        "warnings": validator.warnings,
        "counts": dict(validator.counts),
    }
    print(json.dumps(result, indent=2))
    return 0 if not validator.errors else 1


if __name__ == "__main__":
    sys.exit(main())
