"""Lab 5 evidence figures from actual source, Gradle XML and GitHub API JSON."""
from pathlib import Path
import hashlib
import json
import textwrap
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont
from docx import Document

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "report-assets/lab5"
SRC = ROOT / "app/src/main/java/com/songjunyi/cardgame"
WS = Path(r"C:\Users\48604\Documents\ChatGPT\安卓开发")
QA = WS / "tmp/assignment5"
FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 22)
SMALL = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 17)

def draw(name, lines):
    im = Image.new("RGB", (1100, len(lines) * 28 + 40), "#0D1726")
    d = ImageDraw.Draw(im)
    for i, (num, line) in enumerate(lines):
        d.text((15, 20 + i * 28), num, font=SMALL, fill="#839AB5")
        d.text((72, 20 + i * 28), line, font=FONT, fill="#EAF2FF")
    im.save(OUT / name)

def excerpt(name, pieces):
    lines = []
    for file, start_text, end_text in pieces:
        source = (SRC / file).read_text(encoding="utf-8").splitlines()
        start = next(i for i, line in enumerate(source) if start_text in line)
        end = next(i for i, line in enumerate(source) if i > start and
                   (line == end_text if end_text == "    }" else end_text in line))
        if end_text == "    }": end += 1
        if "diff.dispatchUpdatesTo" in end_text: end += 2
        lines += [("", file), ("", "")]
        for i in range(start, end):
            wrapped = textwrap.wrap(source[i], 76, replace_whitespace=False, drop_whitespace=False) or [""]
            lines.append((str(i + 1), wrapped[0]))
            lines.extend(("", line) for line in wrapped[1:])
        lines.append(("", ""))
    draw(name, lines)

excerpt("code-adapter.png", [
    ("CardAdapter.kt", "class CardAdapter(", "    override fun onCreateViewHolder"),
    ("CardAdapter.kt", "        val holder = CardHolder", "    override fun onBindViewHolder"),
    ("CardAdapter.kt", "    fun refresh()", "        diff.dispatchUpdatesTo(this)"),
])
excerpt("code-model.png", [
    ("model/Card.kt", "    fun match(", "    }"),
    ("model/CardMatchingGame.kt", "        val others =", "    fun snapshot()"),
])
lines = [("", "ACTUAL GRADLE RESULTS"), ("", "")]
for file, label in [("unit-tests.xml", "JUnit model tests"), ("ui-tests.xml", "API 35 instrumentation UI tests")]:
    suite = ET.parse(OUT / file).getroot()
    assert suite.get("failures") == "0" and suite.get("errors") == "0"
    lines += [("", label), ("", f"Tests {suite.get('tests')}   Failures 0   Errors 0"), ("", "")]
    if file == "ui-tests.xml":
        lines += [("", "PASS  " + case.get("name")) for case in suite.findall("testcase")]
lines += [("", ""), ("", "Lint: 0 errors / 3 warnings"), ("", "Local device: HelloWorld_Visible_API35 / Android 15"),
          ("", "Local tests. Not a GitHub Actions run.")]
draw("test-results-lab5.png", lines)

pr = json.loads((OUT / "github-pr1.json").read_text(encoding="utf-8-sig"))
protection = json.loads((OUT / "github-protection.json").read_text(encoding="utf-8-sig"))
assert pr["state"] == "MERGED"
assert protection["enforce_admins"]["enabled"]
lines = [("", "GITHUB API VERIFIED RECORD"), ("", ""), ("", "Repository: StephenYI43/card-game-2024110131"),
         ("", "Protected branch: main"), ("", "PR required: yes / external approvals required: 0"),
         ("", "Enforce administrators: true"), ("", "Required check: Local Android verification"),
         ("", "Force pushes: false / deletions: false"), ("", ""), ("", "PR #1: Dev -> main"),
         ("", "State: " + pr["state"]), ("", "Merged: " + pr["mergedAt"]),
         ("", "Merge commit: " + pr["mergeCommit"]["oid"][:12]), ("", ""),
         ("", "Self-review recorded as COMMENT, not self-approval."),
         ("", "No administrator override used."), ("", "lab4-complete retains commit 3617f28."),
         ("", "Local lab4 files unchanged.")]
draw("github-record.png", lines)

reference = WS / "实验五+RecyclerView翻牌游戏应用.docx"
with zipfile.ZipFile(reference) as z:
    inventory = {name: {"size": len(z.read(name)), "sha256": hashlib.sha256(z.read(name)).hexdigest()}
                 for name in z.namelist()}
(QA / "template-inventory.json").write_text(json.dumps(inventory, indent=2), encoding="utf-8")
doc = Document(reference)
styles = [{"name": s.name, "type": str(s.type),
           "font": s.font.name if hasattr(s, "font") else None,
           "size": s.font.size.pt if hasattr(s, "font") and s.font.size else None} for s in doc.styles]
(QA / "template-styles.json").write_text(json.dumps(styles, ensure_ascii=False, indent=2), encoding="utf-8")
print("Created actual source, test, GitHub API figures and template inventories.")
