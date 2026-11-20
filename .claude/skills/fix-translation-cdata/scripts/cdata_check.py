#!/usr/bin/env python3
"""Audit and verify HTML-in-CDATA Android string resources.

Usage (Python 3.8+, standard library only):
    python3 cdata_check.py audit  <res-dir>
    python3 cdata_check.py verify <before.xml> <after.xml> [<source values/strings.xml>]

"audit" lists every translation of a string that is CDATA-wrapped in the
source file and reports the ones whose CDATA or markup is broken.

"verify" proves that an edited translation file only changed markup: the
file outside <string> bodies is byte-identical, and every changed string
still has exactly the same user-visible text. Exit code 0 means PASS.

Parsing is regex based on purpose: broken translation files are often not
well-formed XML, and those are exactly the ones that need reading.
"""

import re
import sys
import xml.etree.ElementTree as ElementTree
from collections import Counter
from pathlib import Path

STRING = re.compile(r'(<string\s+name="([^"]+)"[^>]*?(?<!/)>)(.*?)(</string>)', re.DOTALL)

HTML_TAGS = (
    r"html|head|body|h[1-6]|p|br|ul|ol|li|b|i|u|em|strong|small|big|font|a|span|div|tt"
    r"|strike|s|sub|sup|blockquote|img|hr|del|cite|dfn"
)

# A tag written as <x>, <x&gt; or &lt;x&gt; (the last two are the broken forms).
TAG = re.compile(
    r"(?:<|&lt;)\s*(/?)\s*(" + HTML_TAGS + r")\b([^<>]*?)(/?)\s*(?:>|&gt;)", re.IGNORECASE
)

ESCAPED_TAG = re.compile(
    r"&lt;\s*/?\s*(?:" + HTML_TAGS + r")\b|<\s*/?\s*(?:" + HTML_TAGS + r")\b[^<>]*?&gt;",
    re.IGNORECASE,
)

HREF = re.compile(r"""href\s*=\s*(?:\\?"|&quot;|')?([^"'\\&\s>]+)""", re.IGNORECASE)

ENTITY = re.compile(r"&(#x[0-9a-fA-F]+|#[0-9]+|quot|apos|lt|gt|nbsp|amp);")
NAMED_ENTITIES = {"quot": '"', "apos": "'", "lt": "<", "gt": ">", "nbsp": " ", "amp": "&"}

CDATA_START = "<![CDATA["
CDATA_END = "]]>"


# ------------------------------------------------------------------ helpers


def read(path):
    return Path(path).read_text(encoding="utf-8")


def strings(content):
    """Map string name -> raw body, exactly as written in the file."""
    return {m.group(2): m.group(3) for m in STRING.finditer(content)}


def skeleton(content):
    """The file with every <string> body blanked, to detect edits elsewhere."""
    return STRING.sub(lambda m: m.group(1) + "\u2026" + m.group(4), content)


def has_cdata(body):
    return body.strip().startswith(CDATA_START)


def strip_cdata(body):
    trimmed = body.strip()
    if not trimmed.startswith(CDATA_START):
        return body
    trimmed = trimmed[len(CDATA_START) :]
    end = trimmed.rfind(CDATA_END)
    return trimmed[:end] if end >= 0 else trimmed


def decode_android_escapes(text):
    out = []
    i = 0
    while i < len(text):
        char = text[i]
        if char != "\\" or i + 1 >= len(text):
            out.append(char)
            i += 1
            continue
        escaped = text[i + 1]
        if escaped in "nt":
            out.append(" ")
            i += 2
        elif escaped == "u" and re.fullmatch(r"[0-9a-fA-F]{4}", text[i + 2 : i + 6]):
            out.append(chr(int(text[i + 2 : i + 6], 16)))
            i += 6
        else:
            out.append(escaped)
            i += 2
    return "".join(out)


def decode_entities(text):
    def replace(match):
        entity = match.group(1)
        if entity.startswith("#x"):
            return chr(int(entity[2:], 16))
        if entity.startswith("#"):
            return chr(int(entity[1:]))
        return NAMED_ENTITIES[entity]

    return ENTITY.sub(replace, text)


def visible_text(body):
    """What a user reads: markup removed, Android escapes and entities
    decoded, whitespace collapsed (HTML rendering collapses it as well)."""
    text = TAG.sub(" ", strip_cdata(body))
    text = decode_android_escapes(text)
    text = decode_entities(text)
    return re.sub(r"\s+", " ", text).strip()


def tag_counts(body):
    counts = Counter()
    for match in TAG.finditer(strip_cdata(body)):
        name = match.group(2).lower()
        if name == "br":
            counts["<br/>"] += 1
        elif match.group(1):
            counts["</" + name + ">"] += 1
        else:
            counts["<" + name + ">"] += 1
    return counts


def tag_diff(source_body, translated_body):
    expected = tag_counts(source_body)
    actual = tag_counts(translated_body)
    return ", ".join(
        f"{tag} source={expected[tag]} translation={actual[tag]}"
        for tag in sorted(set(expected) | set(actual))
        if expected[tag] != actual[tag]
    )


def hrefs(body):
    return HREF.findall(body)


def well_formed_error(content):
    try:
        ElementTree.fromstring(content.encode("utf-8"))
        return None
    except ElementTree.ParseError as error:
        return str(error)


def line_of(content, name):
    index = content.find(f'name="{name}"')
    return -1 if index < 0 else content.count("\n", 0, index) + 1


def first_difference(before, after):
    i = 0
    while i < len(before) and i < len(after) and before[i] == after[i]:
        i += 1
    start = max(0, i - 40)
    return (
        f"  before: ...{before[start:i + 60]}...\n"
        f"  after:  ...{after[start:i + 60]}..."
    )


# -------------------------------------------------------------------- audit


def audit(res_dir):
    res_dir = Path(res_dir)
    source = strings(read(res_dir / "values" / "strings.xml"))
    cdata_source = {name for name, body in source.items() if has_cdata(body)}
    print("Source strings using CDATA: " + ", ".join(sorted(cdata_source)))

    problems = 0
    for path in sorted(res_dir.glob("values-*/strings.xml")):
        content = read(path)
        translated = strings(content)
        lines = []
        for name, body in translated.items():
            if name not in source:
                continue
            source_cdata = name in cdata_source
            issues = []
            if source_cdata and not has_cdata(body):
                issues.append("MISSING_CDATA")
            if not source_cdata and has_cdata(body):
                issues.append("UNEXPECTED_CDATA")
            if ESCAPED_TAG.search(strip_cdata(body)):
                issues.append(
                    "ESCAPED_MARKUP" if source_cdata else "ESCAPED_MARKUP_OUTSIDE_CDATA_SCOPE"
                )
            if has_cdata(body) and CDATA_END in strip_cdata(body):
                issues.append("NESTED_CDATA_END")
            if source_cdata:
                diff = tag_diff(source[name], body)
                if diff:
                    issues.append(f"MARKUP_DIFFERS({diff})")
            if issues:
                lines.append(f"  {name} line {line_of(content, name)}: {' '.join(issues)}")

        xml_error = well_formed_error(content)
        if lines or (xml_error and cdata_source & translated.keys()):
            print(f"\n{path.relative_to(res_dir)}")
            if xml_error:
                print(f"  NOT_WELL_FORMED_XML: {xml_error}")
            for line in lines:
                print(line)
            problems += len(lines)

    print(f"\n{problems} string(s) need attention.")
    return 0 if problems == 0 else 1


# ------------------------------------------------------------------- verify


def verify(before_path, after_path, source_path=None):
    before = read(before_path)
    after = read(after_path)
    failures = 0

    xml_error = well_formed_error(after)
    if xml_error:
        print(f"FAIL not well-formed XML: {xml_error}")
        failures += 1

    before_skeleton = skeleton(before)
    after_skeleton = skeleton(after)
    if before_skeleton != after_skeleton:
        print(
            "FAIL file changed outside <string> bodies (names, attributes, comments or"
            " other entries):\n" + first_difference(before_skeleton, after_skeleton)
        )
        failures += 1

    source_strings = strings(read(source_path)) if source_path else {}
    before_strings = strings(before)
    changed = 0
    for name, new_body in strings(after).items():
        old_body = before_strings.get(name)
        if old_body is None or old_body == new_body:
            continue
        changed += 1

        old_text = visible_text(old_body)
        new_text = visible_text(new_body)
        if old_text != new_text:
            print(f"FAIL {name}: user-visible text changed\n{first_difference(old_text, new_text)}")
            failures += 1
        else:
            print(f"ok   {name}: visible text identical")

        lost_links = [link for link in hrefs(old_body) if link not in hrefs(new_body)]
        if lost_links:
            print(f"FAIL {name}: link target(s) removed: {lost_links}")
            failures += 1

        source_body = source_strings.get(name)
        if source_body is not None and has_cdata(source_body):
            if not has_cdata(new_body):
                print(f"FAIL {name}: source uses CDATA but the fix does not")
                failures += 1
            if ESCAPED_TAG.search(strip_cdata(new_body)):
                print(f"FAIL {name}: still contains escaped markup (&lt; or &gt;)")
                failures += 1
            diff = tag_diff(source_body, new_body)
            if diff:
                print(f"note {name}: markup differs from source: {diff}")

    verdict = "PASS" if failures == 0 else "FAIL"
    print(f"{verdict}: {changed} string(s) changed, {failures} problem(s).")
    return 0 if failures == 0 else 1


def main(argv):
    if len(argv) >= 3 and argv[1] == "audit":
        return audit(argv[2])
    if len(argv) >= 4 and argv[1] == "verify":
        return verify(argv[2], argv[3], argv[4] if len(argv) >= 5 else None)
    print(
        "usage: cdata_check.py audit <res-dir>\n"
        "       cdata_check.py verify <before.xml> <after.xml> [<source.xml>]",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
