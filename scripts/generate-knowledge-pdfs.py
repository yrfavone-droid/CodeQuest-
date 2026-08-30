"""Build faithful, readable PDFs from the supplied Knowledge Library Markdown briefs."""

from __future__ import annotations

import html
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import KeepTogether, Paragraph, SimpleDocTemplate, Spacer


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIRECTORY = ROOT / "desktopApp" / "src" / "jvmMain" / "resources" / "curriculum" / "academy" / "source" / "DEEP_DIVES" / "prompts"
OUTPUT_DIRECTORY = ROOT / "desktopApp" / "src" / "jvmMain" / "resources" / "curriculum" / "academy" / "documents" / "knowledge"


def make_styles():
    base = getSampleStyleSheet()
    return {
        "cover": ParagraphStyle(
            "cover", parent=base["Title"], fontName="Helvetica-Bold", fontSize=21,
            leading=26, textColor=colors.HexColor("#2D1B16"), alignment=TA_CENTER,
            spaceAfter=12,
        ),
        "eyebrow": ParagraphStyle(
            "eyebrow", parent=base["Normal"], fontName="Helvetica-Bold", fontSize=9,
            leading=12, textColor=colors.HexColor("#D95D2A"), alignment=TA_CENTER,
            spaceAfter=18,
        ),
        "heading": ParagraphStyle(
            "heading", parent=base["Heading2"], fontName="Helvetica-Bold", fontSize=15,
            leading=20, textColor=colors.HexColor("#B94A1D"), spaceBefore=14, spaceAfter=7,
        ),
        "body": ParagraphStyle(
            "body", parent=base["BodyText"], fontName="Helvetica", fontSize=10.5,
            leading=15, textColor=colors.HexColor("#443A35"), spaceAfter=8,
        ),
        "bullet": ParagraphStyle(
            "bullet", parent=base["BodyText"], fontName="Helvetica", fontSize=10.5,
            leading=15, leftIndent=14, firstLineIndent=-9, textColor=colors.HexColor("#443A35"),
            spaceAfter=5,
        ),
        "note": ParagraphStyle(
            "note", parent=base["BodyText"], fontName="Helvetica-Oblique", fontSize=9,
            leading=13, textColor=colors.HexColor("#6F625A"), alignment=TA_CENTER,
            spaceBefore=18,
        ),
    }


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#E9D7CC"))
    canvas.line(doc.leftMargin, 13 * mm, A4[0] - doc.rightMargin, 13 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#7A6C63"))
    canvas.drawString(doc.leftMargin, 8 * mm, "CodeQuest AI Academy - supplied knowledge brief")
    canvas.drawRightString(A4[0] - doc.rightMargin, 8 * mm, f"Page {doc.page}")
    canvas.restoreState()


def clean(text: str) -> str:
    return html.escape(text.strip()).replace("\n", "<br/>")


def build_pdf(markdown_path: Path) -> None:
    output_path = OUTPUT_DIRECTORY / f"{markdown_path.stem}.pdf"
    lines = markdown_path.read_text(encoding="utf-8").splitlines()
    title = next((line[2:].strip() for line in lines if line.startswith("# ")), markdown_path.stem)
    styles = make_styles()
    document = SimpleDocTemplate(
        str(output_path), pagesize=A4, leftMargin=23 * mm, rightMargin=23 * mm,
        topMargin=23 * mm, bottomMargin=21 * mm, title=title, author="CodeQuest AI Academy",
    )
    story = [
        Paragraph("CODEQUEST AI ACADEMY", styles["eyebrow"]),
        Paragraph(clean(title), styles["cover"]),
        Paragraph(
            "This downloadable PDF faithfully packages the supplied Knowledge Library authoring brief. "
            "It is source material, not a claim that a complete course book has been authored.",
            styles["note"],
        ),
        Spacer(1, 7 * mm),
    ]
    for raw_line in lines:
        line = raw_line.strip()
        if not line or line.startswith("# "):
            continue
        if line.startswith("## "):
            story.append(Paragraph(clean(line[3:]), styles["heading"]))
        elif line.startswith("- "):
            story.append(Paragraph(f"- {clean(line[2:])}", styles["bullet"]))
        else:
            match = re.match(r"([A-Za-z ]+):\s*(.*)", line)
            if match:
                text = f"<b>{clean(match.group(1))}:</b> {clean(match.group(2))}"
            else:
                text = clean(line)
            story.append(Paragraph(text, styles["body"]))
    document.build(story, onFirstPage=footer, onLaterPages=footer)


def main() -> None:
    OUTPUT_DIRECTORY.mkdir(parents=True, exist_ok=True)
    source_files = sorted(SOURCE_DIRECTORY.glob("*.md"))
    if len(source_files) != 20:
        raise RuntimeError(f"Expected 20 supplied knowledge files, found {len(source_files)}")
    for source_file in source_files:
        build_pdf(source_file)
    output_files = sorted(OUTPUT_DIRECTORY.glob("*.pdf"))
    if len(output_files) != len(source_files):
        raise RuntimeError("Knowledge PDF output count does not match the supplied files")
    print(f"Created {len(output_files)} knowledge PDFs in {OUTPUT_DIRECTORY}")


if __name__ == "__main__":
    main()
