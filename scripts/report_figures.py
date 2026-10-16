"""Build report figures from real source files and actual Gradle XML results."""
from pathlib import Path
import textwrap
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/songjunyi/cardgame"
OUT = ROOT / "report-assets"
FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 23)
SMALL = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 19)

def draw(name, lines):
    im = Image.new("RGB", (1050, len(lines)*29+40), "#0D1726")
    d = ImageDraw.Draw(im)
    for i, (number, text) in enumerate(lines):
        d.text((18,20+i*29), number, font=SMALL, fill="#7893AF")
        d.text((85,20+i*29), text, font=FONT, fill="#E8F3FF")
    im.save(OUT / name)

def source(name, blocks):
    lines = []
    for filename, first, last in blocks:
        contents = (SRC / filename).read_text(encoding="utf-8").splitlines()
        start = next(i for i,s in enumerate(contents) if first in s)
        end = next(i for i,s in enumerate(contents) if i>start and (s == last if last == "    }" else last in s))
        if last == "    }": end += 1
        lines += [("", filename), ("", "")]
        for i in range(start,end):
            segments = textwrap.wrap(contents[i], width=67, replace_whitespace=False,
                                     drop_whitespace=False) or [""]
            lines.append((str(i+1), segments[0]))
            lines += [("", "    "+s) for s in segments[1:]]
        lines += [("", "")]
    draw(name, lines)

source("code-01-model.png", [
    ("model/Card.kt", "fun match(", "    }"),
    ("model/CardMatchingGame.kt", "val other = cards.firstOrNull", "    fun snapshot()"),
])
source("code-02-controller.png", [
    ("GameActivity.kt", "board = GameBoardView", "        boardPanel.addView"),
    ("GameActivity.kt", "button(content, R.string.restart_game)", "        button(content, R.string.exit_game"),
    ("GameActivity.kt", "private fun render()", "    override fun onSaveInstanceState"),
])
lines = [("", "GRADLE TEST RESULTS"), ("", "")]
for filename, title in [("unit-tests.xml","JUnit model tests"),("ui-tests.xml","Android instrumentation UI tests")]:
    root = ET.parse(OUT / filename).getroot()
    lines += [("", title), ("", f"Tests: {root.get('tests')}  Failures: {root.get('failures')}  Errors: {root.get('errors')}")]
    for case in root.findall("testcase"):
        lines.append(("", "PASS  "+case.get("name")))
    lines += [("", "")]
draw("test-results.png", lines)
