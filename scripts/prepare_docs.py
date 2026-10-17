#!/usr/bin/env python3
"""Build localized documentation sources from the two canonical READMEs."""
from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
base = "https://github.com/Alox1d/dateparser-kotlin/blob/main/"
for lang, filename in (("en", "README.md"), ("ru", "README.ru.md")):
    text = (root / filename).read_text()
    def link(match):
        label, url = match.group(1), match.group(2)
        if url == "README.md": url = "../en/index.md"
        elif url == "README.ru.md": url = "../ru/index.md"
        elif url.startswith("website/assets/"):
            # A README path is repository-relative; a localized MkDocs path is not.
            url = "../assets/" + url.removeprefix("website/assets/")
        elif not re.match(r"https?://|#", url): url = base + url
        return "[" + label + "](" + url + ")"
    text = re.sub(r"\[([^]]+)\]\(([^)]+)\)", link, text)
    title = "DateParser Kotlin - English and Russian natural language date parser" if lang == "en" else "DateParser Kotlin - парсер дат и времени из текста"
    text = "---\n" + "title: " + title + "\n---\n\n" + text
    target = root / "website" / lang / "index.md"
    if "--check" in sys.argv:
        if not target.exists() or target.read_text() != text: raise SystemExit("Stale docs: " + str(target))
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
