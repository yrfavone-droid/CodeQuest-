#!/usr/bin/env python3
"""Build the complete standalone CodeQuest Academy curriculum package."""

from __future__ import annotations

import json
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from curriculum_blueprint import LEVELS, PATHS, TRACKS


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
PATH_ASSETS = ASSETS / "paths"
SCHEMA_DIR = ROOT / "schema"
REPORTS = ROOT / "reports"
TESTS = ROOT / "tests"
INTEGRATION = ROOT / "integration"
VERSION = "1.0.0"


def slug(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


def dump(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def difficulty_for(level_number: int) -> str:
    return ["beginner", "foundational", "intermediate", "advanced", "advanced"][level_number - 1]


def days_for(level_number: int) -> int:
    return [7, 8, 8, 9, 10][level_number - 1]


def minutes_for(level_number: int) -> int:
    return [720, 760, 800, 850, 900][level_number - 1]


def project_minutes_for(level_number: int) -> int:
    return [180, 210, 240, 270, 300][level_number - 1]


def skill_id(level_code: str, lesson_number: int) -> str:
    return f"{level_code}-S{lesson_number:02d}"


def level_id(path_id: str, code: str) -> str:
    return f"{path_id}:{code}"


def misconception_id(level_code: str, lesson_number: int, suffix: str) -> str:
    return f"{level_code}-M{lesson_number:02d}-{suffix}"


def path_profile(level: dict) -> dict:
    return PATHS[level["path_id"]]


def safety_note(profile: dict) -> str | None:
    return profile.get("safety")


def make_options(correct: str, wrongs: list[str], variant: int) -> tuple[list[dict], str, dict]:
    texts = [correct] + wrongs[:3]
    shift = variant % 4
    texts = texts[shift:] + texts[:shift]
    letters = ["A", "B", "C", "D"]
    options = [{"id": letter, "text": text} for letter, text in zip(letters, texts)]
    correct_id = next(item["id"] for item in options if item["text"] == correct)
    wrong_explanations = {
        item["id"]: (
            "This choice breaks the stated input-output contract or skips the structural rule that must be preserved."
        )
        for item in options
        if item["id"] != correct_id
    }
    return options, correct_id, wrong_explanations


def common_question_fields(
    level: dict,
    lesson_number: int,
    qid: str,
    qtype: str,
    difficulty: int,
    prompt: str,
) -> dict:
    profile = path_profile(level)
    lesson_id = f"{level['code']}-L{lesson_number:02d}"
    result = {
        "id": qid,
        "track_id": profile["track_id"],
        "path_id": level["path_id"],
        "level_id": level_id(level["path_id"], level["code"]),
        "level_code": level["code"],
        "lesson_id": lesson_id,
        "skill_id": skill_id(level["code"], lesson_number),
        "type": qtype,
        "difficulty": difficulty,
        "prompt": prompt,
        "misconception_tags": [
            misconception_id(level["code"], lesson_number, "contract"),
            misconception_id(level["code"], lesson_number, "state"),
        ],
        "review_tags": [slug(level["topics"][lesson_number - 1]["title"]), "structural_reasoning"],
        "xp": 8 + difficulty * 2,
    }
    if safety_note(profile):
        result["safety_scope"] = safety_note(profile)
    return result


def make_mcq(
    level: dict,
    lesson_number: int,
    qid: str,
    qtype: str,
    difficulty: int,
    prompt: str,
    correct: str,
    wrongs: list[str],
    explanation: str,
    variant: int,
) -> dict:
    item = common_question_fields(level, lesson_number, qid, qtype, difficulty, prompt)
    options, correct_id, wrong_explanations = make_options(correct, wrongs, variant)
    item.update(
        {
            "interaction_data": {"selection_mode": "single", "shuffle_options": True},
            "options": options,
            "correct_answer": correct_id,
            "explanation": explanation,
            "wrong_choice_explanations": wrong_explanations,
            "hints": [
                "First identify the input, required output, and the boundary where the decision is made.",
                "Now name the state or relationship that must remain valid after the operation.",
                f"Use this governing rule: {level['topics'][lesson_number - 1]['principle']}",
            ],
            "automatic_grading": {
                "method": "single_choice",
                "accepted_answers": [correct_id],
                "case_sensitive": True,
            },
        }
    )
    return item


def make_match_question(level: dict, lesson_number: int, qid: str, variant: int) -> dict:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    pairs = [
        {"left": "Input", "right": f"The facts entering the {topic['title']} decision"},
        {"left": "Output", "right": "The observable result promised by the contract"},
        {"left": "Constraint", "right": "A rule that limits which states or solutions are valid"},
        {"left": "Representation", "right": profile["representation"]},
    ]
    item = common_question_fields(
        level,
        lesson_number,
        qid,
        "match_pairs",
        min(5, 1 + level["number"]),
        f"Match each reasoning element to its role while planning {topic['title']}.",
    )
    item.update(
        {
            "interaction_data": {
                "left_items": [p["left"] for p in pairs],
                "right_items": [p["right"] for p in reversed(pairs)],
                "shuffle_right": True,
            },
            "correct_answer": pairs,
            "explanation": (
                f"The match separates what enters the problem, what must leave it, what restricts valid work, "
                f"and how {profile['representation']} makes the relationships visible."
            ),
            "wrong_choice_explanations": {
                "input_output_swap": "Inputs are provided facts; outputs are the results the system must produce.",
                "constraint_representation_swap": "A constraint limits validity, while a representation makes structure visible.",
            },
            "hints": [
                "Begin with the two endpoints: what is given and what must be produced.",
                "A constraint says what is allowed; it is not the diagram or structure itself.",
                f"The intended representation is {profile['representation']}.",
            ],
            "automatic_grading": {
                "method": "unordered_pairs",
                "accepted_pairs": pairs,
                "require_all_pairs": True,
            },
        }
    )
    return item


def make_order_question(level: dict, lesson_number: int, qid: str, qtype: str = "drag_order") -> dict:
    topic = level["topics"][lesson_number - 1]
    steps = [
        f"State the input and output contract for {topic['title']}",
        "Model the variables, relationships, and valid boundaries",
        f"Apply the rule: {topic['principle']}",
        "Trace a normal case and a boundary case",
        "Reject an invalid case and record why",
    ]
    item = common_question_fields(
        level,
        lesson_number,
        qid,
        qtype,
        min(5, 1 + level["number"]),
        f"Arrange the steps into a defensible workflow for {topic['title']}.",
    )
    item.update(
        {
            "interaction_data": {"blocks": list(reversed(steps)), "shuffle_blocks": True},
            "correct_answer": steps,
            "explanation": (
                "The workflow defines success before implementation, makes state and constraints visible, applies the "
                "governing rule, and verifies both accepted and rejected cases."
            ),
            "wrong_choice_explanations": {
                "test_before_contract": "A test cannot be judged until the promised output and constraints are known.",
                "rule_before_model": "Applying a rule before identifying state can update the wrong value or boundary.",
            },
            "hints": [
                "The contract must come before the implementation rule.",
                "Model valid state before transforming it.",
                "Finish by checking both a valid boundary and an invalid input.",
            ],
            "automatic_grading": {"method": "ordered_list", "accepted_order": steps},
        }
    )
    return item


def make_practice_questions(level: dict, lesson_number: int) -> list[dict]:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    base = f"{level['code']}-L{lesson_number:02d}-P"
    wrongs = [
        "Implement immediately and infer the requirements from the first successful result.",
        "Store every intermediate value as global state so all parts can modify it.",
        "Ignore boundary cases because the normal case already demonstrates correctness.",
    ]
    items = [
        make_mcq(
            level,
            lesson_number,
            f"{base}Q01",
            "multiple_choice",
            1,
            f"Which statement is the most reliable rule for {topic['title']}?",
            topic["principle"],
            wrongs,
            f"This is the defining principle for the lesson: {topic['principle']}",
            lesson_number,
        ),
        make_mcq(
            level,
            lesson_number,
            f"{base}Q02",
            "multiple_choice",
            2,
            f"A learner is planning {profile['context']}. What should be made explicit before applying {topic['title']}?",
            "The input, required output, changing state, constraints, and success evidence.",
            [
                "Only the final visual appearance; internal state can remain undefined.",
                "Only the preferred tool name; the data contract can be decided later.",
                "Only a normal case; rejected inputs and boundaries do not affect the design.",
            ],
            "Explicit contracts and state models prevent an implementation from solving a different problem than the one intended.",
            lesson_number + 1,
        ),
        make_match_question(level, lesson_number, f"{base}Q03", lesson_number),
        make_order_question(level, lesson_number, f"{base}Q04"),
    ]

    trace_correct = "accepted = 2; rejected = 1; final state = verified"
    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q05",
            "trace_state",
            2,
            (
                f"Trace a {topic['title']} check. The state starts accepted=0, rejected=0. "
                "A normal case passes, a documented boundary case passes, and an out-of-contract case is rejected. "
                "After all three are checked, the process marks itself verified. What is the final state?"
            ),
            trace_correct,
            [
                "accepted = 3; rejected = 0; final state = verified",
                "accepted = 1; rejected = 2; final state = incomplete",
                "accepted = 2; rejected = 1; final state = untested",
            ],
            "Two cases satisfy the contract and one does not. Completing the three planned checks moves the process to verified.",
            lesson_number + 2,
        )
    )
    items[-1]["interaction_data"]["trace_rows"] = [
        {"step": 0, "case": "none", "accepted": 0, "rejected": 0, "state": "planned"},
        {"step": 1, "case": "normal", "accepted": 1, "rejected": 0, "state": "checking"},
        {"step": 2, "case": "boundary", "accepted": 2, "rejected": 0, "state": "checking"},
        {"step": 3, "case": "out-of-contract", "accepted": 2, "rejected": 1, "state": "verified"},
    ]

    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q06",
            "predict_output",
            3,
            (
                f"A reasoning pipeline for {topic['title']} receives the ordered states "
                "[unmodeled, modeled, rule-applied, edge-checked]. It returns the last state only when every transition "
                "follows the documented order. What does it return?"
            ),
            "edge-checked",
            ["unmodeled", "modeled", "invalid-transition"],
            "All four states occur in the required order, so the output is the final state, edge-checked.",
            lesson_number + 3,
        )
    )
    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q07",
            "identify_bug",
            3,
            (
                f"In {profile['context']}, an implementation of {topic['title']} works for one normal input but changes "
                "an unrelated state field and has no boundary check. What is the root reasoning bug?"
            ),
            "The transition violates state isolation and was never checked against the full contract.",
            [
                "The implementation has too few global variables.",
                "The normal input should be repeated instead of adding a boundary case.",
                "The output should be hidden so state changes cannot be observed.",
            ],
            "A correct transition changes only documented state and must be verified at boundaries, not merely on one convenient input.",
            lesson_number + 4,
        )
    )
    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q08",
            "choose_structure",
            3,
            f"Which representation best supports reasoning about {topic['title']} in this path?",
            profile["representation"].capitalize() + ".",
            [
                "An unlabelled screenshot with no state or relationship information.",
                "A single global variable that combines every input and output.",
                "An unordered list of tool names without a problem contract.",
            ],
            f"This path models solutions through {profile['representation']}, which exposes the relationships the lesson needs.",
            lesson_number + 5,
        )
    )
    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q09",
            "scenario",
            4,
            (
                f"A teammate proposes two plans for {topic['title']}: Plan A states the contract, models state, applies the "
                "lesson rule, and tests edges. Plan B starts implementation and records only successful outputs. Which plan is defensible?"
            ),
            "Plan A, because its assumptions and correctness evidence are inspectable.",
            [
                "Plan B, because fewer recorded cases always means lower complexity.",
                "Plan B, because successful outputs prove every invalid input is safe.",
                "Both plans, because contracts and edge cases do not affect correctness.",
            ],
            "Plan A follows the structural reasoning cycle and produces evidence that another person can review.",
            lesson_number + 6,
        )
    )
    items.append(
        make_mcq(
            level,
            lesson_number,
            f"{base}Q10",
            "adaptive_review",
            4,
            (
                f"A learner's mastery for {topic['title']} is 0.62. Which next activity follows the adaptive rules and best "
                "addresses the learner's incomplete state model?"
            ),
            "Guided practice with visible state steps and progressive hints.",
            [
                "Skip directly to challenge mode with all hints removed.",
                "Mark the skill mastered because 0.62 is above one half.",
                "Repeat the same final answer without showing state transitions.",
            ],
            "A score from 0.55 to below 0.75 receives guided practice with hints; visible state steps target the named weakness.",
            lesson_number + 7,
        )
    )
    assert len(items) == 10
    return items


def make_challenge(level: dict, lesson_number: int) -> dict:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    challenge_id = f"{level['code']}-L{lesson_number:02d}-CH"
    activities = []

    ordered = [
        "Declare the contract and permitted boundary",
        "Create the smallest sufficient state model",
        f"Apply {topic['title']} using its governing rule",
        "Trace normal, boundary, and rejected cases",
        "Record the output and remaining assumption",
    ]
    activities.append(
        {
            "id": f"{challenge_id}-A01",
            "type": "block_builder",
            "title": "Build the reasoning pipeline",
            "prompt": f"Build an ordered pipeline for applying {topic['title']} to {profile['context']}.",
            "interaction_data": {"blocks": list(reversed(ordered)), "shuffle_blocks": True},
            "correct_answer": ordered,
            "explanation": "The pipeline exposes the contract, limits state, applies the rule, tests evidence, and records uncertainty in that order.",
            "hints": ["Start with the contract.", "State must be modeled before it changes.", "Testing and recording evidence belong at the end."],
            "automatic_grading": {"method": "ordered_list", "accepted_order": ordered},
            "skill_id": skill_id(level["code"], lesson_number),
            "difficulty": min(5, 2 + level["number"] // 2),
        }
    )
    activities.append(
        {
            "id": f"{challenge_id}-A02",
            "type": "debugging",
            "title": "Repair a violated invariant",
            "prompt": (
                f"A fictional implementation of {topic['title']} accepts an out-of-contract input and overwrites a valid "
                "previous state. Select the repair that validates before mutation and preserves the previous state on rejection."
            ),
            "interaction_data": {
                "options": [
                    {"id": "A", "text": "Mutate first, then hide any error message."},
                    {"id": "B", "text": "Validate the candidate, compute a new state, commit only if valid, otherwise retain the previous state."},
                    {"id": "C", "text": "Delete the constraint so every input becomes valid."},
                    {"id": "D", "text": "Retry the same mutation until it appears to work."},
                ]
            },
            "correct_answer": "B",
            "explanation": "Validation before commit makes rejection side-effect-free and preserves the state invariant.",
            "wrong_choice_explanations": {
                "A": "Hiding evidence does not repair invalid state.",
                "C": "Removing a requirement changes the problem rather than solving it.",
                "D": "Repeating an invalid transition can compound the damage.",
            },
            "hints": ["Protect the previous valid state.", "Separate validation from commit.", "Only a validated candidate may replace current state."],
            "automatic_grading": {"method": "single_choice", "accepted_answers": ["B"]},
            "skill_id": skill_id(level["code"], lesson_number),
            "difficulty": min(5, 2 + level["number"] // 2),
        }
    )
    activities.append(
        {
            "id": f"{challenge_id}-A03",
            "type": "scenario",
            "title": "Apply the lesson under constraints",
            "prompt": (
                f"Design the decision record for {topic['title']} with exactly four fields: input evidence, state before, "
                "state after, and constraint result. The boundary case must pass and the invalid case must leave state unchanged."
            ),
            "interaction_data": {
                "required_fields": ["input_evidence", "state_before", "state_after", "constraint_result"],
                "cases": ["normal", "boundary", "invalid"],
            },
            "correct_answer": {
                "normal": "pass_and_commit",
                "boundary": "pass_and_commit",
                "invalid": "reject_and_preserve",
            },
            "explanation": "The record distinguishes evidence, transition, and validation while ensuring rejected input cannot mutate valid state.",
            "hints": ["Use all four required fields.", "A boundary value can be valid.", "Rejection preserves state_before."],
            "automatic_grading": {
                "method": "structured_object",
                "required_keys": ["normal", "boundary", "invalid"],
                "accepted_object": {"normal": "pass_and_commit", "boundary": "pass_and_commit", "invalid": "reject_and_preserve"},
            },
            "skill_id": skill_id(level["code"], lesson_number),
            "difficulty": min(5, 3 + level["number"] // 2),
        }
    )
    activities.append(
        {
            "id": f"{challenge_id}-A04",
            "type": "compare_solutions",
            "title": "Transfer the rule to a new case",
            "prompt": (
                f"Compare Solution A, which follows '{topic['principle']}', with Solution B, which has no explicit "
                "constraint or rejected-state behavior. Choose the solution that transfers safely to a larger input and justify it."
            ),
            "interaction_data": {"options": ["solution_a", "solution_b", "equivalent"]},
            "correct_answer": "solution_a",
            "explanation": "Solution A retains the lesson's governing relationship and defines invalid behavior, so scaling does not depend on an unspoken normal-case assumption.",
            "wrong_choice_explanations": {
                "solution_b": "Missing constraints make its apparent success depend on unverified inputs.",
                "equivalent": "The solutions differ in correctness evidence and rejected-state behavior.",
            },
            "hints": ["Compare assumptions, not surface length.", "Look for invalid-input behavior.", f"Use the rule: {topic['principle']}"],
            "automatic_grading": {"method": "single_choice", "accepted_answers": ["solution_a"]},
            "skill_id": skill_id(level["code"], lesson_number),
            "difficulty": min(5, 3 + level["number"] // 2),
        }
    )

    for activity in activities:
        activity.update(
            {
                "track_id": profile["track_id"],
                "path_id": level["path_id"],
                "level_code": level["code"],
                "lesson_id": f"{level['code']}-L{lesson_number:02d}",
                "misconception_tags": [misconception_id(level["code"], lesson_number, "transfer")],
            }
        )
        if safety_note(profile):
            activity["safety_scope"] = safety_note(profile)

    return {
        "id": challenge_id,
        "lesson_id": f"{level['code']}-L{lesson_number:02d}",
        "practice_id": f"{level['code']}-L{lesson_number:02d}-PR",
        "title": f"{topic['title']} Transfer Challenge",
        "estimated_minutes": 10,
        "activities": activities,
        "unlock_rule": {"requires": [f"{level['code']}-L{lesson_number:02d}-PR"], "condition": "completed"},
    }


def make_worked_examples(level: dict, lesson_number: int) -> list[dict]:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    return [
        {
            "title": "Normal case: contract to verified output",
            "scenario": f"A learner applies {topic['title']} while building {profile['context']}.",
            "input": "One well-formed input that satisfies the documented preconditions.",
            "reasoning": [
                "Name the observable output before choosing an operation.",
                "Represent only the state and relationships needed for that output.",
                topic["principle"],
                "Trace the transition and compare the result with the contract.",
            ],
            "output": "A valid state transition with the promised result and recorded evidence.",
        },
        {
            "title": "Boundary case: valid at the limit",
            "scenario": "The input is exactly at the smallest or largest documented valid boundary.",
            "input": "A boundary value explicitly included by the constraint.",
            "reasoning": [
                "Write the inclusive or exclusive boundary symbolically.",
                "Evaluate the predicate before changing state.",
                "Apply the same rule used for ordinary valid input.",
                "Verify that the output remains representable and accessible.",
            ],
            "output": "The boundary case is accepted once and produces a valid output without special hidden state.",
        },
        {
            "title": "Rejected case: preserve the invariant",
            "scenario": "An input violates one required relationship or arrives in an unsupported state.",
            "input": "An out-of-contract value paired with a previously valid state.",
            "reasoning": [
                "Identify the failed constraint and stop before commit.",
                "Keep the previous valid state unchanged.",
                "Return a specific, non-sensitive explanation or recovery path.",
                "Record the case as evidence for a regression check.",
            ],
            "output": "A controlled rejection, preserved prior state, and actionable feedback.",
        },
    ]


def make_lesson(level: dict, lesson_number: int) -> dict:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    sid = skill_id(level["code"], lesson_number)
    lid = f"{level['code']}-L{lesson_number:02d}"
    prereqs = []
    if lesson_number > 1:
        prereqs.append(skill_id(level["code"], lesson_number - 1))
    elif level["number"] > 1:
        prev_code = f"{profile['prefix']}-{(level['number'] - 1) * 100 + 1}"
        prereqs.append(skill_id(prev_code, 8))
    trace_rows = [
        {"step": 0, "input_status": "received", "state": "previous-valid", "constraint": "not-checked", "output": "none"},
        {"step": 1, "input_status": "classified", "state": "candidate", "constraint": "checking", "output": "none"},
        {"step": 2, "input_status": "validated", "state": "candidate", "constraint": "passed", "output": "pending"},
        {"step": 3, "input_status": "committed", "state": "new-valid", "constraint": "passed", "output": "contract-result"},
    ]
    practice = {
        "id": f"{lid}-PR",
        "lesson_id": lid,
        "title": f"Practice: {topic['title']}",
        "estimated_minutes": 14,
        "questions": make_practice_questions(level, lesson_number),
        "unlock_rule": {"requires": [lid], "condition": "completed"},
    }
    challenge = make_challenge(level, lesson_number)
    lesson = {
        "id": lid,
        "order": lesson_number,
        "title": topic["title"],
        "goal": f"Model, apply, trace, and defend {topic['title']} within {profile['context']}.",
        "skill_ids": [sid],
        "required_prerequisite_skills": prereqs,
        "estimated_minutes": 30,
        "learning_objective": (
            f"Given a normal, boundary, or invalid case, the learner will use {topic['title']} to produce a valid result "
            "and explain the invariant or constraint that makes the result correct."
        ),
        "content_blocks": [
            {
                "type": "beginner_explanation",
                "title": "Start with the relationship",
                "body": (
                    f"{topic['title']} is easier when you stop treating it as a tool to memorize. Begin with what enters "
                    f"the problem, what must leave it, and what is allowed to change. {topic['principle']}"
                ),
            },
            {
                "type": "technical_explanation",
                "title": "Formal structure",
                "body": (
                    f"Represent the task as a transition F(input, current_state) -> (output, next_state). For {topic['title']}, "
                    "F is valid only when its preconditions hold, its output matches the declared shape, and every stated "
                    "invariant remains true. Validation occurs before an irreversible commit."
                ),
            },
            {
                "type": "mental_model",
                "title": "See the system before the syntax",
                "body": f"Use {profile['representation']}. Draw the boundary, label inputs and outputs, then trace one transition at a time.",
            },
        ],
        "input_output_analysis": {
            "inputs": [
                f"The data or event that activates {topic['title']}",
                "The current valid state",
                "The documented constraints and environment",
            ],
            "outputs": [
                "A value, decision, or visible state matching the contract",
                "A preserved or deliberately updated next state",
                "Evidence explaining acceptance or rejection",
            ],
        },
        "variables_and_state": [
            {"name": "candidate_input", "role": "The untrusted or unverified incoming value", "lifetime": "one decision"},
            {"name": "current_state", "role": "The last state known to satisfy all invariants", "lifetime": "until a valid commit"},
            {"name": "constraint_result", "role": "The explicit validation outcome", "lifetime": "one transition"},
            {"name": "next_state", "role": "A proposed state computed without corrupting current_state", "lifetime": "candidate then committed"},
        ],
        "constraints": [
            "Inputs must match the declared shape and permitted boundary.",
            "A rejected input must not corrupt the previous valid state.",
            "The output must be representable by the current application workflow.",
            "Every external or user-provided value is validated before use.",
        ],
        "invariants": [
            f"The governing relationship remains true: {topic['principle']}",
            "At most one documented commit occurs for one accepted transition.",
            "Invalid input leaves the last valid state unchanged.",
        ],
        "step_by_step_reasoning": [
            "Restate the problem as a testable input-output contract.",
            "List state variables and separate current state from a candidate update.",
            "Write constraints as predicates with visible boundary choices.",
            f"Apply the structural rule for {topic['title']}.",
            "Trace the normal case one transition at a time.",
            "Trace an exact boundary and one invalid case.",
            "Compare observed output and preserved invariants with the contract.",
            "Record the first divergent state if the check fails.",
        ],
        "worked_examples": make_worked_examples(level, lesson_number),
        "structured_representation": {
            "type": "trace_table",
            "columns": ["step", "input_status", "state", "constraint", "output"],
            "rows": trace_rows,
            "caption": f"A validate-before-commit trace for {topic['title']}.",
        },
        "common_mistakes": [
            "Beginning implementation before defining the required output.",
            "Changing state before the input or transition has been validated.",
            f"Memorizing the label '{topic['title']}' without applying its governing relationship.",
            "Testing only one convenient normal case and assuming boundaries behave identically.",
        ],
        "edge_cases": [
            "The smallest valid input or empty valid structure, when allowed.",
            "The largest documented input, count, depth, size, or duration.",
            "A duplicate, repeated event, retry, or already-completed transition.",
            "Malformed, missing, stale, unauthorized, or otherwise out-of-contract input.",
        ],
        "debugging_strategy": [
            "Freeze the first input that produces a wrong observable result.",
            "Write the expected trace beside the observed trace.",
            "Locate the first row where state, constraint, or output diverges.",
            "Repair the rule or boundary responsible for that first divergence.",
            "Rerun the failing case plus one normal, one boundary, and one rejection case.",
        ],
        "progressive_hints": [
            "Label the input and promised output before naming any tool or syntax.",
            "Separate current_state from next_state and write the validity predicate.",
            f"Apply this rule, then trace the boundary: {topic['principle']}",
        ],
        "summary": (
            f"{topic['title']} is mastered when the learner can model its inputs and state, apply the rule '{topic['principle']}', "
            "and prove the result with normal, boundary, and rejected traces."
        ),
        "checkpoint_questions": [
            {"prompt": "What must be written before implementation begins?", "answer": "The input-output contract and constraints."},
            {"prompt": "Why keep current_state separate from next_state?", "answer": "So invalid candidates can be rejected without corrupting valid state."},
            {"prompt": "What does a boundary trace test?", "answer": "Whether inclusive, exclusive, size, or lifetime limits match the contract."},
            {"prompt": f"State the governing rule for {topic['title']}.", "answer": topic["principle"]},
        ],
        "unlock_rule": {
            "requires": [f"{level['code']}-CS"] if lesson_number == 1 else [f"{level['code']}-L{lesson_number - 1:02d}-CH"],
            "condition": "completed",
        },
        "practice_set": practice,
        "challenge": challenge,
    }
    if safety_note(profile):
        lesson["safety_scope"] = safety_note(profile)
    return lesson


def make_cheat_sheet(level: dict) -> dict:
    profile = path_profile(level)
    concepts = level["topics"]
    glossary = [
        {"term": topic["title"], "definition": topic["principle"]} for topic in concepts
    ] + [
        {"term": "Input", "definition": "Information or an event provided to a system or reasoning step."},
        {"term": "Output", "definition": "The observable result promised by the problem contract."},
        {"term": "State", "definition": "Information retained between events that can influence later behavior."},
        {"term": "Constraint", "definition": "A rule limiting which inputs, states, or solutions are valid."},
        {"term": "Invariant", "definition": "A statement that remains true before and after every valid transition."},
        {"term": "Boundary case", "definition": "A valid or invalid case located exactly at a documented limit."},
        {"term": "Trace", "definition": "A stepwise record of inputs, state changes, decisions, and outputs."},
        {"term": "Contract", "definition": "An explicit agreement about accepted inputs, outputs, effects, and failures."},
    ]
    quick_cards = []
    for index, topic in enumerate(concepts, 1):
        quick_cards.append({"front": f"{index}. What is the core rule of {topic['title']}?", "back": topic["principle"]})
        quick_cards.append(
            {
                "front": f"How should you verify {topic['title']}?",
                "back": "Trace a normal case, an exact boundary, and an invalid case while checking the stated invariant.",
            }
        )
    examples = []
    for index in [1, 3, 5, 7]:
        topic = concepts[index - 1]
        examples.append(
            {
                "title": f"Structure {index}: {topic['title']}",
                "problem": f"Determine a safe next state for {profile['context']} under one normal and one boundary input.",
                "model": {"input": "candidate + current state", "rule": topic["principle"], "output": "validated next state"},
                "trace": ["receive", "classify", "validate", "transform", "verify", "commit"],
                "result": "Commit only after the output and invariant match the contract.",
            }
        )
    sheet = {
        "id": f"{level['code']}-CS",
        "title": f"{level['title']} — Structural Cheat Sheet",
        "introduction": (
            f"This sheet turns {level['title']} into visible relationships. It is a reference for planning, tracing, "
            "debugging, and explaining—not a list to memorize."
        ),
        "why_it_matters": (
            f"The level contributes directly to a {profile['artifact']}. Each concept helps the learner move from an "
            "ambiguous task to a contract, a representation, a verified transition, and an explainable result."
        ),
        "mental_model": {
            "summary": f"Represent the work as {profile['representation']} before translating it into implementation details.",
            "reasoning_cycle": [
                "Understand the problem",
                "Name inputs and outputs",
                "Model variables, state, constraints, and relationships",
                "Decompose into transitions",
                "Predict before execution",
                "Trace normal and edge cases",
                "Translate the verified structure",
                "Test and explain the result",
            ],
        },
        "input_output_model": {
            "notation": "F(input, current_state | constraints) -> (output, next_state, evidence)",
            "input_questions": ["What is provided?", "What may be absent or malformed?", "Which values are trusted?"],
            "output_questions": ["What is observable?", "What shape and state are promised?", "How is failure represented?"],
        },
        "core_concepts": [
            {"order": i, "title": topic["title"], "rule": topic["principle"], "skill_id": skill_id(level["code"], i)}
            for i, topic in enumerate(concepts, 1)
        ],
        "syntax_or_notation_reference": [
            {"notation": "x -> y", "meaning": "x is transformed into y"},
            {"notation": "P(x)", "meaning": "a predicate deciding whether x satisfies a constraint"},
            {"notation": "S_t", "meaning": "system state before event t"},
            {"notation": "S_(t+1) = T(S_t, event)", "meaning": "the next state produced by a transition"},
            {"notation": "[start, end)", "meaning": "a half-open range including start and excluding end"},
            {"notation": "A -> B -> C", "meaning": "an ordered pipeline or dependency path"},
            {"notation": "node --relation--> node", "meaning": "a labelled relationship in a tree or graph"},
            {"notation": "expected == observed", "meaning": "a verification check at an observable boundary"},
        ],
        "structural_patterns": [
            {"name": topic["title"], "when_to_use": f"When the task depends on {slug(topic['title']).replace('_', ' ')}.", "rule": topic["principle"]}
            for topic in concepts
        ],
        "worked_examples": examples,
        "trace_tables": [
            {
                "title": "Valid transition",
                "columns": ["step", "input", "state_before", "constraint", "state_after", "output"],
                "rows": [
                    [0, "candidate", "valid", "unchecked", "valid", "none"],
                    [1, "candidate", "valid", "pass", "candidate-valid", "pending"],
                    [2, "candidate", "candidate-valid", "verified", "new-valid", "contract-result"],
                ],
            },
            {
                "title": "Rejected transition",
                "columns": ["step", "input", "state_before", "constraint", "state_after", "output"],
                "rows": [
                    [0, "invalid", "valid", "unchecked", "valid", "none"],
                    [1, "invalid", "valid", "fail", "valid", "specific-rejection"],
                ],
            },
        ],
        "diagrams": [
            {
                "type": "directed_flow",
                "nodes": ["input", "validate", "model", "transform", "verify", "output"],
                "edges": [
                    ["input", "validate"], ["validate", "model"], ["model", "transform"],
                    ["transform", "verify"], ["verify", "output"],
                ],
            },
            {
                "type": "state_transition",
                "nodes": ["previous_valid", "candidate", "new_valid", "rejected"],
                "edges": [
                    {"from": "previous_valid", "to": "candidate", "label": "compute"},
                    {"from": "candidate", "to": "new_valid", "label": "constraint passes"},
                    {"from": "candidate", "to": "rejected", "label": "constraint fails"},
                    {"from": "rejected", "to": "previous_valid", "label": "preserve"},
                ],
            },
        ],
        "common_mistakes": [
            "Starting with syntax before defining the input-output contract.",
            "Combining current state and a candidate update so rejection cannot be safe.",
            "Treating an observed normal result as proof for every boundary.",
            "Choosing a structure because it is familiar rather than because its operations match the constraints.",
            "Ignoring empty, missing, duplicate, maximum-size, repeated, or out-of-order cases.",
            "Hiding failures instead of representing loading, empty, rejected, and error states explicitly.",
            "Optimizing before measuring the actual bottleneck or risk.",
            "Explaining what the implementation does without explaining why its invariant proves correctness.",
        ],
        "debugging_checklist": [
            "Can I restate the exact expected output?",
            "Are input shape, units, identity, and trust level explicit?",
            "Which state is allowed to change?",
            "Which invariant should hold after every step?",
            "What is the first observed step that differs from the expected trace?",
            "Is the boundary inclusive or exclusive?",
            "Can an invalid or repeated event mutate valid state?",
            "Are asynchronous or external results stale, missing, or out of order?",
            "Does the representation match the operation mix and constraints?",
            "Did the repair pass the original failure and nearby regression cases?",
        ],
        "glossary": glossary,
        "quick_review_cards": quick_cards,
        "estimated_minutes": 20,
        "unlock_rule": {"requires": [f"{level['code']}-DIAG"], "condition": "completed"},
    }
    if safety_note(profile):
        sheet["safety_scope"] = safety_note(profile)
    return sheet


def make_assessment_mcq(level: dict, lesson_number: int, qid: str, qtype: str, mode: str, variant: int) -> dict:
    topic = level["topics"][lesson_number - 1]
    profile = path_profile(level)
    category_prompts = {
        "multiple_choice": f"In the {mode}, which rule should govern a decision about {topic['title']}?",
        "flowchart_completion": f"In the {mode}, which missing step completes the flow input -> model -> [missing] -> trace -> verify for {topic['title']}?",
        "trace_table": f"In the {mode}, a normal and boundary case pass while an invalid case is rejected without mutation. Which trace summary is correct for {topic['title']}?",
        "debugging": f"In the {mode}, the first divergence for {topic['title']} occurs when state changes before validation. Which repair targets the cause?",
        "scenario": f"In the {mode}, which plan applies {topic['title']} safely to {profile['context']}?",
        "complexity_comparison": f"In the {mode}, which comparison is defensible when choosing between two structures for {topic['title']}?",
        "block_builder": f"In the {mode}, which compact block sequence correctly transfers {topic['title']} to a new constrained case?",
    }
    correct_by_type = {
        "multiple_choice": topic["principle"],
        "flowchart_completion": f"Apply the governing relationship for {topic['title']}",
        "trace_table": "accepted=2, rejected=1, previous valid state preserved on rejection",
        "debugging": "Validate the candidate first, then commit one valid next state.",
        "scenario": "Define the contract, model state and constraints, apply the rule, then test normal, boundary, and rejected cases.",
        "complexity_comparison": "Compare the operations, guarantees, state size, and constraints that actually dominate the task.",
        "block_builder": "contract -> state model -> rule -> boundary trace -> verified output",
    }
    wrong_by_type = {
        "multiple_choice": [
            "A successful normal case makes constraints and rejected behavior unnecessary.",
            "Every value should be global so unrelated parts can modify the transition.",
            "The representation should be chosen only by visual familiarity.",
        ],
        "flowchart_completion": ["Commit an unvalidated candidate", "Remove the output contract", "Skip directly to deployment"],
        "trace_table": [
            "accepted=3, rejected=0, invalid input committed",
            "accepted=1, rejected=2, boundary discarded without checking",
            "accepted=2, rejected=1, previous state erased on rejection",
        ],
        "debugging": [
            "Hide the observed difference.",
            "Repeat the same invalid mutation.",
            "Delete the constraint that caught the problem.",
        ],
        "scenario": [
            "Implement first and infer the goal from whatever output appears.",
            "Test only one convenient input and treat it as proof.",
            "Merge all states so individual transitions cannot be traced.",
        ],
        "complexity_comparison": [
            "Always choose the solution with more named tools.",
            "Ignore memory, risk, and worst-case behavior.",
            "Compare source-code length only, regardless of operations.",
        ],
        "block_builder": [
            "commit -> guess contract -> ignore trace",
            "normal case -> deploy -> remove constraints",
            "global state -> hidden failure -> retry forever",
        ],
    }
    explanation_by_type = {
        "multiple_choice": f"The answer states the precise relationship that defines this skill: {topic['principle']}",
        "flowchart_completion": "After state is modeled, the rule transforms it; tracing and verification then test that transformation.",
        "trace_table": "Two inputs satisfy the documented contract, and rejection is side-effect-free, so the previous valid state remains intact.",
        "debugging": "The first divergence is a premature mutation, so validate-before-commit repairs the cause rather than hiding a later symptom.",
        "scenario": "This plan makes assumptions visible and produces evidence across normal, edge, and failure behavior.",
        "complexity_comparison": "A meaningful comparison is based on required operations, state, constraints, and guarantees rather than superficial size.",
        "block_builder": "The sequence moves from contract to representation to rule application and evidence before accepting the output.",
    }
    item = make_mcq(
        level,
        lesson_number,
        qid,
        qtype,
        min(5, max(1, level["number"] + (variant % 2))),
        category_prompts[qtype],
        correct_by_type[qtype],
        wrong_by_type[qtype],
        explanation_by_type[qtype],
        variant,
    )
    item["assessment_mode"] = mode
    return item


def make_diagnostic(level: dict) -> dict:
    questions = []
    for i in range(12):
        lesson_number = i % 8 + 1
        questions.append(
            make_assessment_mcq(
                level,
                lesson_number,
                f"{level['code']}-DIAG-Q{i + 1:02d}",
                ["multiple_choice", "flowchart_completion", "trace_table"][i % 3],
                "diagnostic assessment",
                i,
            )
        )
    return {
        "id": f"{level['code']}-DIAG",
        "title": f"{level['title']} Diagnostic",
        "estimated_minutes": 15,
        "purpose": "Measure prior reasoning without blocking the level; results seed skill mastery and review recommendations.",
        "attempt_policy": "first_attempt_records_baseline",
        "questions": questions,
        "mastery_effect": "diagnostic_only_no_progress_penalty",
        "unlock_rule": {"requires": [], "condition": "level_available"},
    }


def make_mixed_review(level: dict, review_number: int) -> dict:
    questions = []
    types = ["multiple_choice", "flowchart_completion", "trace_table", "debugging", "scenario", "complexity_comparison"]
    for i in range(12):
        lesson_number = (i * 3 + review_number) % 8 + 1
        questions.append(
            make_assessment_mcq(
                level,
                lesson_number,
                f"{level['code']}-MR{review_number}-Q{i + 1:02d}",
                types[i % len(types)],
                f"mixed review {review_number}",
                20 + review_number * 20 + i,
            )
        )
    return {
        "id": f"{level['code']}-MR{review_number}",
        "title": f"Mixed Review {review_number}",
        "estimated_minutes": 15,
        "questions": questions,
        "coverage_skill_ids": [skill_id(level["code"], i) for i in range(1, 9)],
        "unlock_rule": {
            "requires": [f"{level['code']}-L08-CH"] if review_number == 1 else [f"{level['code']}-MR1"],
            "condition": "completed",
        },
    }


def make_adaptive_review(level: dict) -> dict:
    activities = []
    for i, topic in enumerate(level["topics"], 1):
        activities.append(
            {
                "id": f"{level['code']}-AR-A{i:02d}",
                "skill_id": skill_id(level["code"], i),
                "title": f"Repair the mental model: {topic['title']}",
                "remedial_explanation": (
                    f"Return to the input-output relationship before recalling terminology. {topic['principle']} "
                    "Draw current state and candidate state separately, then trace the exact boundary that caused the error."
                ),
                "activity_type": "adaptive_review",
                "prompt": f"Complete a guided normal-boundary-invalid trace for {topic['title']} and state which invariant survives rejection.",
                "correct_answer": "Invalid input preserves the previous valid state; valid input commits exactly one verified next state.",
                "hints": [
                    "Write current state before reading the candidate.",
                    "Evaluate the constraint before commit.",
                    "On rejection, compare state_after with state_before.",
                ],
                "automatic_grading": {
                    "method": "key_concepts",
                    "required_concepts": ["preserve previous state", "validate before commit", "one verified next state"],
                },
                "misconception_tags": [
                    misconception_id(level["code"], i, "contract"),
                    misconception_id(level["code"], i, "state"),
                    misconception_id(level["code"], i, "transfer"),
                ],
            }
        )
    return {
        "id": f"{level['code']}-AR",
        "title": "Adaptive Weak-Skill Review",
        "estimated_minutes": 15,
        "mastery_rules": [
            {"minimum": 0.0, "maximum_exclusive": 0.55, "mode": "remedial_explanation_and_easier_questions"},
            {"minimum": 0.55, "maximum_exclusive": 0.75, "mode": "guided_practice_with_hints"},
            {"minimum": 0.75, "maximum_exclusive": 0.90, "mode": "normal_progression"},
            {"minimum": 0.90, "maximum_inclusive": 1.0, "mode": "challenge_mode"},
        ],
        "selection_rule": "Select activities for the three lowest mastery skills; include prerequisite repair when mastery is below 0.55.",
        "activities": activities,
        "unlock_rule": {"requires": [f"{level['code']}-MR2"], "condition": "completed"},
    }


def make_final_quiz(level: dict) -> dict:
    distribution = [
        ("multiple_choice", 6),
        ("flowchart_completion", 5),
        ("trace_table", 5),
        ("debugging", 4),
        ("scenario", 4),
        ("complexity_comparison", 3),
        ("block_builder", 3),
    ]
    questions = []
    qn = 1
    for qtype, count in distribution:
        for i in range(count):
            # Step by three through the eight skills. Because 3 and 8 are coprime,
            # every category visits distinct lesson skills before any repeat.
            lesson_number = ((qn - 1) * 3) % 8 + 1
            questions.append(
                make_assessment_mcq(
                    level,
                    lesson_number,
                    f"{level['code']}-QUIZ-Q{qn:02d}",
                    qtype,
                    "final quiz",
                    100 + qn,
                )
            )
            qn += 1
    assert len(questions) == 30
    return {
        "id": f"{level['code']}-QUIZ",
        "title": f"{level['title']} Final Quiz",
        "estimated_minutes": 45,
        "passing_score": 75,
        "question_order": "randomizable",
        "option_order": "randomizable_when_safe",
        "coverage_skill_ids": [skill_id(level["code"], i) for i in range(1, 9)],
        "distribution": {
            "concept": 6,
            "structural_reasoning": 5,
            "trace_or_prediction": 5,
            "debugging": 4,
            "application_scenarios": 4,
            "complexity_architecture_comparison": 3,
            "advanced_challenge": 3,
        },
        "questions": questions,
        "retry_policy": {
            "enabled": True,
            "after_failure": "recommend lowest-mastery adaptive activities before a new randomized attempt",
            "preserve_best_score": True,
        },
        "unlock_rule": {"requires": [f"{level['code']}-AR"], "condition": "completed"},
    }


def make_project(level: dict) -> dict:
    profile = path_profile(level)
    topics = level["topics"]
    project_id = f"{level['code']}-PROJECT"
    project_title = f"{level['title']}: {profile['artifact'].title()}"
    rubric = [
        {"category": "Problem model and structural plan", "points": 15, "evidence": "inputs, outputs, constraints, diagram, and milestones"},
        {"category": "Functional correctness", "points": 25, "evidence": "mandatory behavior matches the project contract"},
        {"category": "Architecture and state reasoning", "points": 15, "evidence": "clear responsibilities, transitions, and dependency boundaries"},
        {"category": "Testing and edge cases", "points": 15, "evidence": "normal, boundary, invalid, repeated, and failure cases"},
        {"category": "Accessibility, safety, and user recovery", "points": 10, "evidence": "usable states, safe handling, and recovery feedback"},
        {"category": "Documentation and submission quality", "points": 10, "evidence": "setup, decisions, limitations, evidence, and reproducible instructions"},
        {"category": "Reflection and justified improvement", "points": 10, "evidence": "first divergence, repair, trade-off, and next iteration"},
    ]
    project = {
        "id": project_id,
        "title": project_title,
        "estimated_minutes": project_minutes_for(level["number"]),
        "progression_stage": [
            "highly_guided_with_blocks_and_starter_structure",
            "guided_with_missing_sections",
            "partially_scaffolded_implementation",
            "open_ended_with_architecture_guidance",
            "production_style_full_path_project",
        ][level["number"] - 1],
        "real_world_context": (
            f"A small team needs {profile['context']} that remains understandable under normal, boundary, invalid, "
            "and recovery conditions. The learner is responsible for the reasoning model and verifiable outcome."
        ),
        "problem_definition": (
            f"Create a level-appropriate {profile['artifact']} that demonstrates all eight skills from {level['title']}. "
            "The artifact must show how inputs become outputs, how state changes, which constraints are enforced, and how failures recover."
        ),
        "project_brief": (
            "Begin with diagrams, tables, and contracts. Build the smallest complete version, test it against the supplied "
            "case categories, record the first divergent state, repair it, and submit both the artifact and reasoning evidence."
        ),
        "skills_assessed": [skill_id(level["code"], i) for i in range(1, 9)],
        "input_output_requirements": {
            "inputs": ["at least two valid cases", "one exact boundary", "one malformed or unsupported case", "one repeated or recovery event"],
            "outputs": ["primary task result", "visible loading/empty/error or rejected state", "test evidence", "decision record"],
        },
        "structural_plan": {
            "required_views": ["input-output table", "state-transition model", "dependency or relationship diagram", "test matrix"],
            "governing_rules": [topic["principle"] for topic in topics],
        },
        "milestones": [
            {"number": 1, "title": "Understand the problem", "deliverable": "one-paragraph restatement and user outcome"},
            {"number": 2, "title": "Identify inputs, outputs, and constraints", "deliverable": "contract table with valid and invalid examples"},
            {"number": 3, "title": "Design the structure", "deliverable": "tree, graph, pipeline, state diagram, or trace model"},
            {"number": 4, "title": "Build the first version", "deliverable": "smallest end-to-end normal flow"},
            {"number": 5, "title": "Test normal cases", "deliverable": "at least three passing normal-case records"},
            {"number": 6, "title": "Test edge cases", "deliverable": "boundary, empty or missing, maximum, and repeated-event evidence"},
            {"number": 7, "title": "Debug errors", "deliverable": "expected-versus-observed trace and root-cause repair"},
            {"number": 8, "title": "Improve quality", "deliverable": "one accessibility, safety, performance, or clarity improvement"},
            {"number": 9, "title": "Submit the project", "deliverable": "artifact, tests, README, screenshots or structured evidence, and limitations"},
            {"number": 10, "title": "Reflect on the solution", "deliverable": "completed project reflection"},
        ],
        "starter_materials": {
            "contract_template": ["actor", "input", "precondition", "transition", "output", "failure", "evidence"],
            "state_template": ["state_name", "allowed_event", "next_state", "guard", "visible_feedback"],
            "test_template": ["case_id", "input", "expected", "observed", "pass", "notes"],
            "scaffold_policy": [
                "Level 1 supplies every heading and ordered milestone.",
                "Level 2 leaves validation and recovery sections for the learner.",
                "Level 3 leaves architecture and core transitions partially open.",
                "Level 4 supplies contracts but not implementation organization.",
                "Level 5 supplies only constraints, acceptance criteria, and review checkpoints.",
            ][level["number"] - 1],
        },
        "mandatory_features": [
            f"Demonstrate {topic['title']} and cite its governing rule in the decision record." for topic in topics
        ],
        "constraints": [
            "Use only the languages, components, or structural interactions supported by the target application environment.",
            "Do not hide invalid input, unavailable data, or failure behind a successful-looking state.",
            "Keep current valid state recoverable until a candidate transition passes validation.",
            "Do not embed secrets, real credentials, personal data, or private endpoints.",
            "Make all important output and error states accessible and testable.",
            "Record assumptions and unsupported cases explicitly.",
            "The submitted result must be reproducible from the included instructions.",
        ],
        "test_cases": [
            {"id": "TC01", "category": "normal", "condition": "first ordinary valid input", "expected": "one correct output and valid next state"},
            {"id": "TC02", "category": "normal", "condition": "second valid input with different data", "expected": "same contract with data-specific result"},
            {"id": "TC03", "category": "boundary", "condition": "smallest valid or empty-valid input", "expected": "accepted only if contract includes it"},
            {"id": "TC04", "category": "boundary", "condition": "largest documented valid input", "expected": "bounded completion without corrupted state"},
            {"id": "TC05", "category": "invalid", "condition": "missing or malformed required input", "expected": "specific rejection and preserved state"},
            {"id": "TC06", "category": "repeat", "condition": "same event or submission repeated", "expected": "documented idempotent or duplicate behavior"},
            {"id": "TC07", "category": "failure", "condition": "dependency or data unavailable", "expected": "visible recoverable failure state"},
            {"id": "TC08", "category": "recovery", "condition": "valid retry after a controlled failure", "expected": "successful recovery without duplicate harm"},
        ],
        "edge_cases": ["empty or minimum", "maximum documented size", "duplicate or retry", "out-of-order or stale result", "malformed or unauthorized input"],
        "debugging_checklist": [
            "Freeze a reproducible failing input.",
            "Compare expected and observed traces.",
            "Find the first divergent state.",
            "Check the constraint at that transition.",
            "Repair cause before symptoms.",
            "Rerun the failure and adjacent cases.",
            "Confirm state recovery and accessible feedback.",
            "Record the limitation if the case remains unsupported.",
        ],
        "completion_checklist": [
            "All eight level skills appear in the artifact or evidence.",
            "The input-output contract is complete.",
            "The structural model matches the implementation or decision flow.",
            "All eight required test categories have evidence.",
            "The rubric self-score totals 100 possible points.",
            "No secret, real credential, or sensitive personal data is included.",
            "Submission instructions reproduce the result.",
            "Reflection names one failure, repair, and justified improvement.",
        ],
        "submission_requirements": [
            "Project artifact or repository reference",
            "README with setup or review instructions",
            "Input-output contract",
            "Structural diagram or trace table",
            "Completed test matrix",
            "Known limitations and safe-use boundaries",
            "Rubric self-assessment",
            "Project reflection",
        ],
        "rubric": rubric,
        "common_failure_conditions": [
            "The result handles only one normal case.",
            "State changes before validation.",
            "A level skill is named but not demonstrated.",
            "The diagram disagrees with the implemented flow.",
            "A failed dependency leaves a false success state.",
            "Submission evidence cannot be reproduced.",
        ],
        "reflection_questions": [
            "Which assumption changed after the first trace?",
            "Where did expected and observed state first diverge?",
            "Which invariant prevented the most serious failure?",
            "What trade-off did you make among clarity, cost, performance, accessibility, safety, or flexibility?",
            "What evidence would you collect before a wider release?",
        ],
        "optional_extensions": [
            "Add a second representation and compare which relationships become easier to inspect.",
            "Add property-based or generated boundary cases without duplicating existing tests.",
            "Measure one performance, quality, or risk indicator and justify an improvement threshold.",
            "Design a migration or compatibility plan for one changed requirement.",
        ],
        "acceptable_outcome_example": (
            "A submission may use the supplied contract and state templates, implement one complete primary journey, "
            "show all required failure states, and provide eight passing test records plus one documented limitation. "
            "The exact implementation remains the learner's work."
        ),
        "unlock_rule": {"requires": [f"{level['code']}-QUIZ"], "condition": "score_at_least", "value": 75},
    }
    if safety_note(profile):
        project["safety_scope"] = safety_note(profile)
        project["constraints"].extend(
            [
                "Use only the fictional isolated lab and supplied fictional data.",
                "No live target, public system, real account, harmful payload, persistence, evasion, credential capture, or destructive action is permitted.",
                "Stop immediately when scope, ownership, monitoring, or impact is uncertain.",
            ]
        )
    return project


def make_project_reflection(level: dict) -> dict:
    return {
        "id": f"{level['code']}-REFLECTION",
        "title": "Project Reflection",
        "estimated_minutes": 10,
        "prompts": [
            {"id": "R1", "prompt": "Restate the final input-output contract in one sentence.", "minimum_words": 20},
            {"id": "R2", "prompt": "Describe the first state where expected and observed behavior diverged.", "minimum_words": 30},
            {"id": "R3", "prompt": "Explain the repair and the invariant or constraint it restored.", "minimum_words": 40},
            {"id": "R4", "prompt": "Name one design trade-off and the evidence behind your choice.", "minimum_words": 35},
            {"id": "R5", "prompt": "What is the safest and most valuable next improvement?", "minimum_words": 25},
        ],
        "completion_rule": "all_prompts_answered_and_project_submitted",
        "unlock_rule": {"requires": [f"{level['code']}-PROJECT"], "condition": "submitted"},
    }


def make_mastery_challenge(level: dict) -> dict:
    profile = path_profile(level)
    return {
        "id": f"{level['code']}-MASTERY",
        "title": "Optional Mastery Transfer",
        "estimated_minutes": 45,
        "optional": True,
        "prompt": (
            f"Transfer the level's eight governing rules to a new version of {profile['context']} with one changed "
            "constraint, one new failure state, and one stricter evidence requirement."
        ),
        "required_outputs": [
            "revised input-output contract",
            "before-and-after structural diagram",
            "change-impact table across all eight skills",
            "normal, boundary, invalid, and recovery traces",
            "trade-off explanation and remaining limitation",
        ],
        "success_criteria": [
            "The changed constraint is applied consistently.",
            "No unaffected invariant is silently removed.",
            "The new failure state has visible recovery behavior.",
            "Evidence distinguishes old and new behavior.",
        ],
        "unlock_rule": {"requires": [f"{level['code']}-PROJECT"], "condition": "submitted"},
    }


def make_weekly_plan(level: dict) -> list[dict]:
    plan = [
        {"day": 1, "activities": [f"{level['code']}-DIAG", f"{level['code']}-CS", f"{level['code']}-L01", f"{level['code']}-L01-PR", f"{level['code']}-L01-CH"]},
        {"day": 2, "activities": [f"{level['code']}-L02", f"{level['code']}-L02-PR", f"{level['code']}-L02-CH", f"{level['code']}-L03", f"{level['code']}-L03-PR", f"{level['code']}-L03-CH"]},
        {"day": 3, "activities": [f"{level['code']}-L04", f"{level['code']}-L04-PR", f"{level['code']}-L04-CH", f"{level['code']}-L05", f"{level['code']}-L05-PR", f"{level['code']}-L05-CH"]},
        {"day": 4, "activities": [f"{level['code']}-L06", f"{level['code']}-L06-PR", f"{level['code']}-L06-CH", "targeted_weak_skill_replay"]},
        {"day": 5, "activities": [f"{level['code']}-L07", f"{level['code']}-L07-PR", f"{level['code']}-L07-CH", f"{level['code']}-L08", f"{level['code']}-L08-PR", f"{level['code']}-L08-CH"]},
        {"day": 6, "activities": [f"{level['code']}-MR1", f"{level['code']}-MR2", f"{level['code']}-AR", "integrated_structural_transfer"]},
        {"day": 7, "activities": [f"{level['code']}-QUIZ", "project_contract", "project_milestone_1", "project_milestone_2"]},
    ]
    for day in range(8, days_for(level["number"]) + 1):
        if day == 8:
            acts = ["project_structure", "project_first_version", "project_normal_tests"]
        elif day == 9:
            acts = ["project_edge_tests", "project_debugging", "project_quality_improvement"]
        else:
            acts = ["project_submission", f"{level['code']}-REFLECTION", f"{level['code']}-MASTERY"]
        plan.append({"day": day, "activities": acts})
    if days_for(level["number"]) == 7:
        plan[-1]["activities"].extend(["project_guided_build", "project_tests", "project_submission", f"{level['code']}-REFLECTION"])
    elif days_for(level["number"]) == 8:
        plan[-1]["activities"].extend(["project_edge_tests", "project_submission", f"{level['code']}-REFLECTION", f"{level['code']}-MASTERY"])
    elif days_for(level["number"]) == 9:
        plan[-1]["activities"].extend(["project_submission", f"{level['code']}-REFLECTION", f"{level['code']}-MASTERY"])
    return plan


def make_timeline(level: dict) -> list[dict]:
    nodes = [
        {"id": f"{level['code']}-DIAG", "type": "diagnostic", "content_ref": f"{level['code']}-DIAG", "required": True},
        {"id": f"{level['code']}-CS", "type": "cheat_sheet", "content_ref": f"{level['code']}-CS", "required": True},
    ]
    for i in range(1, 9):
        nodes.extend(
            [
                {"id": f"{level['code']}-L{i:02d}", "type": "lesson", "content_ref": f"{level['code']}-L{i:02d}", "required": True},
                {"id": f"{level['code']}-L{i:02d}-PR", "type": "practice", "content_ref": f"{level['code']}-L{i:02d}-PR", "required": True},
                {"id": f"{level['code']}-L{i:02d}-CH", "type": "challenge", "content_ref": f"{level['code']}-L{i:02d}-CH", "required": True},
            ]
        )
    nodes.extend(
        [
            {"id": f"{level['code']}-MR1", "type": "mixed_review", "content_ref": f"{level['code']}-MR1", "required": True},
            {"id": f"{level['code']}-MR2", "type": "mixed_review", "content_ref": f"{level['code']}-MR2", "required": True},
            {"id": f"{level['code']}-AR", "type": "adaptive_review", "content_ref": f"{level['code']}-AR", "required": True},
            {"id": f"{level['code']}-QUIZ", "type": "final_quiz", "content_ref": f"{level['code']}-QUIZ", "required": True},
            {"id": f"{level['code']}-PROJECT", "type": "project", "content_ref": f"{level['code']}-PROJECT", "required": True},
            {"id": f"{level['code']}-REFLECTION", "type": "project_reflection", "content_ref": f"{level['code']}-REFLECTION", "required": True},
            {"id": f"{level['code']}-MASTERY", "type": "optional_mastery_challenge", "content_ref": f"{level['code']}-MASTERY", "required": False},
        ]
    )
    for order, node in enumerate(nodes, 1):
        node["order"] = order
    return nodes


def make_level(level: dict) -> dict:
    profile = path_profile(level)
    skills = [skill_id(level["code"], i) for i in range(1, 9)]
    prerequisites = []
    if level["number"] > 1:
        prev_code = f"{profile['prefix']}-{(level['number'] - 1) * 100 + 1}"
        prerequisites = [skill_id(prev_code, i) for i in range(1, 9)]
    lessons = [make_lesson(level, i) for i in range(1, 9)]
    result = {
        "id": level_id(level["path_id"], level["code"]),
        "track_id": profile["track_id"],
        "path_id": level["path_id"],
        "code": level["code"],
        "level_number": level["number"],
        "title": level["title"],
        "difficulty": difficulty_for(level["number"]),
        "goal": f"Apply eight structural skills to produce and defend a level-appropriate {profile['artifact']}.",
        "estimated_days": days_for(level["number"]),
        "estimated_total_minutes": minutes_for(level["number"]),
        "estimated_project_minutes": project_minutes_for(level["number"]),
        "prerequisite_skill_ids": prerequisites,
        "learning_objectives": [
            f"Model and apply {topic['title']} using explicit inputs, outputs, state, constraints, and evidence."
            for topic in level["topics"]
        ],
        "skill_ids": skills,
        "concept_titles": [topic["title"] for topic in level["topics"]],
        "mastery_tags": [slug(topic["title"]) for topic in level["topics"]] + ["decomposition", "trace_reasoning", "edge_cases"],
        "unlock_rule": (
            {"requires": [], "condition": "path_available"}
            if level["number"] == 1
            else {
                "requires": [f"{profile['prefix']}-{(level['number'] - 1) * 100 + 1}-PROJECT"],
                "condition": "submitted",
                "preserve_existing_progress": True,
            }
        ),
        "weekly_plan": make_weekly_plan(level),
        "diagnostic": make_diagnostic(level),
        "cheat_sheet": make_cheat_sheet(level),
        "lessons": lessons,
        "mixed_reviews": [make_mixed_review(level, 1), make_mixed_review(level, 2)],
        "adaptive_review": make_adaptive_review(level),
        "final_quiz": make_final_quiz(level),
        "project": make_project(level),
        "project_reflection": make_project_reflection(level),
        "optional_mastery_challenge": make_mastery_challenge(level),
        "timeline_nodes": make_timeline(level),
        "next_level_unlock": {"condition": "project_submitted", "reflection_required_for_completion_badge": True, "mastery_challenge_required": False},
    }
    if safety_note(profile):
        result["safety_scope"] = safety_note(profile)
    return result


def make_path_capstone(path_id: str, levels: list[dict]) -> dict:
    profile = PATHS[path_id]
    return {
        "id": f"{profile['prefix']}-PATH-CAPSTONE",
        "track_id": profile["track_id"],
        "path_id": path_id,
        "title": f"{profile['title']} Path Capstone",
        "estimated_minutes": 600,
        "context": f"Design, build, evaluate, and present a complete {profile['artifact']} for a fictional community organization.",
        "brief": (
            "Integrate the five level projects into one coherent system. Begin with user outcomes and constraints, draw the "
            "complete structure, define interfaces between parts, implement or model the core journey, test failures and "
            "edge cases, measure quality, document safety and limitations, and present evidence against the rubric."
        ),
        "skills_assessed": [sid for level in levels for sid in level["skill_ids"]],
        "required_deliverables": [
            "problem and stakeholder statement",
            "input-output and data contracts",
            "architecture, state, and trust-boundary diagrams",
            "working or structurally complete core journey",
            "normal, edge, failure, recovery, and accessibility test evidence",
            "risk, privacy, ethics, and limitation review",
            "deployment or handoff plan",
            "ten-minute demonstration and written reflection",
        ],
        "milestones": ["scope", "design review", "first vertical slice", "test review", "quality review", "final submission"],
        "rubric": [
            {"category": "Problem framing and constraints", "points": 15},
            {"category": "Integrated architecture", "points": 20},
            {"category": "Correct core journey", "points": 20},
            {"category": "Testing and evidence", "points": 15},
            {"category": "Safety, accessibility, and responsibility", "points": 15},
            {"category": "Documentation and communication", "points": 15},
        ],
        "unlock_rule": {"requires": [levels[-1]["project"]["id"]], "condition": "submitted"},
        "safety_scope": profile.get("safety", "legal, privacy-aware, and limited to authorized fictional or learner-owned systems"),
    }


def make_track_final(track: dict, path_capstones: list[dict]) -> dict:
    return {
        "id": f"{track['id']}-FINAL",
        "track_id": track["id"],
        "title": f"{track['title']} Track Final Project",
        "estimated_minutes": 900,
        "brief": (
            "Combine the two path perspectives into one end-to-end product or professional portfolio. Define the user "
            "problem, connect the path boundaries through explicit contracts, demonstrate one integrated journey, test "
            "cross-boundary failures, evaluate quality and risk, and prepare a reproducible handoff."
        ),
        "required_path_capstones": [item["id"] for item in path_capstones],
        "deliverables": [
            "integrated system or assessment portfolio",
            "cross-path architecture and data-flow model",
            "requirements-to-evidence matrix",
            "test, risk, accessibility, privacy, and recovery evidence",
            "deployment, maintenance, or remediation roadmap",
            "final demonstration and decision-focused report",
        ],
        "rubric": [
            {"category": "Integration and contracts", "points": 25},
            {"category": "Technical or analytical quality", "points": 25},
            {"category": "Evidence and testing", "points": 20},
            {"category": "Safety, ethics, accessibility, and limitations", "points": 15},
            {"category": "Communication and handoff", "points": 15},
        ],
        "unlock_rule": {"requires": [item["id"] for item in path_capstones], "condition": "all_completed"},
    }


def make_schema() -> dict:
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "https://codequest.academy/schema/curriculum-1.0.0.json",
        "title": "CodeQuest Academy Standalone Curriculum Path",
        "type": "object",
        "required": ["schema_version", "track_id", "path", "levels"],
        "properties": {
            "schema_version": {"const": VERSION},
            "track_id": {"type": "string", "minLength": 1},
            "path": {
                "type": "object",
                "required": ["id", "title", "track_id", "level_codes"],
                "properties": {
                    "id": {"type": "string"},
                    "title": {"type": "string"},
                    "track_id": {"type": "string"},
                    "level_codes": {"type": "array", "minItems": 5, "maxItems": 5, "uniqueItems": True},
                },
            },
            "levels": {
                "type": "array",
                "minItems": 5,
                "maxItems": 5,
                "items": {
                    "type": "object",
                    "required": [
                        "id", "code", "level_number", "title", "estimated_days", "estimated_total_minutes",
                        "diagnostic", "cheat_sheet", "lessons", "mixed_reviews", "adaptive_review", "final_quiz",
                        "project", "project_reflection", "optional_mastery_challenge", "timeline_nodes",
                    ],
                    "properties": {
                        "estimated_days": {"type": "integer", "minimum": 7, "maximum": 10},
                        "estimated_total_minutes": {"type": "integer", "minimum": 600, "maximum": 900},
                        "lessons": {"type": "array", "minItems": 8, "maxItems": 10},
                        "mixed_reviews": {"type": "array", "minItems": 2, "maxItems": 2},
                        "final_quiz": {"type": "object"},
                        "project": {"type": "object"},
                    },
                },
            },
        },
    }


def build_all() -> dict:
    if ASSETS.exists():
        shutil.rmtree(ASSETS)
    PATH_ASSETS.mkdir(parents=True, exist_ok=True)
    SCHEMA_DIR.mkdir(parents=True, exist_ok=True)
    REPORTS.mkdir(parents=True, exist_ok=True)
    TESTS.mkdir(parents=True, exist_ok=True)
    INTEGRATION.mkdir(parents=True, exist_ok=True)

    built_by_path: dict[str, list[dict]] = defaultdict(list)
    skills = []
    misconceptions = []
    for level_spec in LEVELS:
        level = make_level(level_spec)
        built_by_path[level_spec["path_id"]].append(level)
        for i, topic in enumerate(level_spec["topics"], 1):
            sid = skill_id(level_spec["code"], i)
            prereqs = []
            if i > 1:
                prereqs.append(skill_id(level_spec["code"], i - 1))
            elif level_spec["number"] > 1:
                prefix = PATHS[level_spec["path_id"]]["prefix"]
                prev_code = f"{prefix}-{(level_spec['number'] - 1) * 100 + 1}"
                prereqs.append(skill_id(prev_code, 8))
            skills.append(
                {
                    "id": sid,
                    "track_id": PATHS[level_spec["path_id"]]["track_id"],
                    "path_id": level_spec["path_id"],
                    "level_code": level_spec["code"],
                    "title": topic["title"],
                    "definition": topic["principle"],
                    "prerequisite_skill_ids": prereqs,
                    "mastery_evidence": ["practice accuracy", "challenge transfer", "quiz result", "project evidence"],
                }
            )
            for suffix, description in [
                ("contract", "Begins implementation without an explicit input-output contract."),
                ("state", "Mutates or merges state before validating the candidate transition."),
                ("transfer", "Memorizes the label but cannot apply the governing relationship to a new case."),
            ]:
                misconceptions.append(
                    {
                        "id": misconception_id(level_spec["code"], i, suffix),
                        "skill_id": sid,
                        "description": description,
                        "repair_strategy": "Return to the contract, draw state before and after, and trace normal, boundary, and rejected cases.",
                    }
                )

    index_paths = []
    path_capstones = []
    for path_id, profile in PATHS.items():
        levels = sorted(built_by_path[path_id], key=lambda x: x["level_number"])
        asset = {
            "schema_version": VERSION,
            "track_id": profile["track_id"],
            "path": {
                "id": path_id,
                "title": profile["title"],
                "track_id": profile["track_id"],
                "level_codes": [level["code"] for level in levels],
                "runtime_policy": profile["runtime"],
                "synthesized_fallback_allowed": False,
                "initial_progress": 0,
                "progress_migration_policy": "append_only_ids_and_version_aware_seeding",
            },
            "levels": levels,
        }
        filename = f"{path_id}.json"
        dump(PATH_ASSETS / filename, asset)
        index_paths.append(
            {
                "id": path_id,
                "track_id": profile["track_id"],
                "title": profile["title"],
                "asset": f"paths/{filename}",
                "level_codes": [level["code"] for level in levels],
                "load_mode": "real_asset_required",
            }
        )
        path_capstones.append(make_path_capstone(path_id, levels))

    finals = []
    for track in TRACKS:
        caps = [cap for cap in path_capstones if cap["path_id"] in track["paths"]]
        finals.append(make_track_final(track, caps))

    catalog = {
        "schema_version": VERSION,
        "tracks": TRACKS,
        "paths": index_paths,
        "load_policy": {
            "real_assets_required": True,
            "synthesized_content": "emergency_error_fallback_only",
            "initial_progress": 0,
            "preserve_existing_progress": True,
        },
    }
    dump(ASSETS / "curriculum_catalog.json", catalog)
    dump(ASSETS / "skill_graph.json", {"schema_version": VERSION, "skills": skills, "misconceptions": misconceptions})
    dump(ASSETS / "path_capstones.json", {"schema_version": VERSION, "capstones": path_capstones})
    dump(ASSETS / "track_final_projects.json", {"schema_version": VERSION, "projects": finals})
    dump(SCHEMA_DIR / "curriculum.schema.json", make_schema())

    return {
        "built_by_path": built_by_path,
        "catalog": catalog,
        "skills": skills,
        "misconceptions": misconceptions,
        "path_capstones": path_capstones,
        "track_finals": finals,
    }


if __name__ == "__main__":
    result = build_all()
    print(
        json.dumps(
            {
                "tracks": len(TRACKS),
                "paths": len(result["built_by_path"]),
                "levels": sum(len(items) for items in result["built_by_path"].values()),
                "skills": len(result["skills"]),
            },
            indent=2,
        )
    )
