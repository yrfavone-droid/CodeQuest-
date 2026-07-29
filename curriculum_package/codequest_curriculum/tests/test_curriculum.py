from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from validate_curriculum import Validator  # noqa: E402


class CurriculumValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.validator = Validator()
        cls.validator.validate()

    def test_complete_validation_passes(self) -> None:
        self.assertEqual([], self.validator.errors)
        self.assertGreaterEqual(self.validator.checks, 200_000)

    def test_exact_core_counts(self) -> None:
        expected = {
            "tracks": 5,
            "paths": 10,
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
        for key, value in expected.items():
            self.assertEqual(value, self.validator.counts[key], key)

    def test_every_level_passes_coverage(self) -> None:
        self.assertEqual(50, len(self.validator.coverage))
        for row in self.validator.coverage:
            self.assertEqual("PASS", row["validation"])
            self.assertEqual(8, row["lessons"])
            self.assertEqual(80, row["practice_questions"])
            self.assertEqual(32, row["challenge_activities"])
            self.assertEqual(30, row["final_quiz_questions"])

    def test_manifest_records_standalone_boundary(self) -> None:
        manifest = json.loads((ROOT / "curriculum_build_manifest.json").read_text(encoding="utf-8"))
        self.assertEqual("PASS", manifest["status"])
        self.assertEqual(10, len(manifest["paths"]))
        self.assertIn("Android project was not present", manifest["integration_note"])


if __name__ == "__main__":
    unittest.main()

