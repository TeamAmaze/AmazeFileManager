---
name: fix-translation-cdata
description: Repair broken <![CDATA[...]]> wrappers and the HTML inside them in Android translated string resources (app/src/main/res/values-*/strings.xml), using the source values/strings.xml markup as the template, without ever altering the user-facing translated text. Use this whenever a translation shows raw tags like <b> or <html> in the UI, a Transifex sync dropped or escaped CDATA (&lt;p&gt;, <p&gt;), a values-* strings.xml is not well-formed XML, aapt2 fails on a translation, or someone asks to clean up, audit, align or "fix the HTML" of translated strings, even if they never say "CDATA".
---

# Fix CDATA and HTML in Android translations

Some source strings in `app/src/main/res/values/strings.xml` hold HTML
inside `<![CDATA[...]]>`. The app passes them to
`HtmlCompat.fromHtml(...)` (for example `smb_instructions` in
`GeneralDialogCreation.java`), so the HTML only works if it reaches
`getString()` intact. Transifex round-trips often damage the translated
copies:

| Damage | What happens at runtime |
|---|---|
| CDATA dropped, real tags left (`<html><body>…<b>…`) | aapt2 parses the tags as string styling; `getString()` returns plain text, so `fromHtml` gets no markup and line breaks/links/colour vanish |
| Tags half-escaped (`<html&gt;`) | The file is **not well-formed XML**; the resource build can fail |
| Tags fully escaped (`&lt;b&gt;`) outside CDATA | Usually renders, but differs from the source convention and is fragile |
| `\n` used where the source has `<br/>` | `fromHtml` collapses newlines into spaces, so paragraphs run together |
| Markup drifted from source (missing `<p>`, extra `</p>`) | Broken layout, unbalanced tags |

## The one rule: the translated text is untouchable

Translators own the words. The skill's job is to repair the *container*
(the CDATA wrapper, the tags, how attribute quotes are written) and
never to touch what a user reads. That means:

- Don't translate, re-translate, correct spelling or grammar, fix
  punctuation, add missing sentences from the source, or drop sentences
  that the source no longer has. If the translation is outdated or
  still in English, leave the words alone. That's a job for Transifex,
  not for this skill.
- Characters the translator wrote stay as they are, though their
  *encoding* may change if the new context needs it. For example, `'`
  in an attribute becomes `&apos;`, and a `\'` in the text may stay
  `\'`, because inside CDATA it still renders as `'`.
- Keep inline formatting the translator put on their own words (an
  extra `<b>`), and keep every link (`<a href>`): dropping a link loses
  information. Report these differences instead of "fixing" them.

If a correct structure would require changing words (for example, the
translation merges two source paragraphs into one sentence), keep the
words and pick the closest structure. If none is acceptable, stop and
ask the user. Don't ship text changes.

`scripts/cdata_check.main.kts verify` enforces this rule mechanically. Never
finish without a PASS from it.

## Workflow

Run everything from the repository root. The script is a Kotlin script
that uses only the Kotlin standard library and the JDK. It needs the
`kotlin` command line runner (Kotlin 1.9 or newer, for example
`sdk install kotlin` or `brew install kotlin`); the first run compiles
it, which takes a few seconds.

### 1. Audit

```bash
kotlin .claude/skills/fix-translation-cdata/scripts/cdata_check.main.kts audit app/src/main/res
```

For each translation, this prints the strings whose source uses CDATA
and that are damaged:

- `MISSING_CDATA`: the source has CDATA, the translation doesn't.
- `ESCAPED_MARKUP`: `&lt;tag` or `<tag…&gt;` appears.
- `MARKUP_DIFFERS(...)`: per-tag counts differ from the source. This is
  informational: an extra `<a>` or `<b>` may be legitimate (see the
  rule above), while a missing `<br/>` or an unbalanced `<p>` is real
  damage.
- `NOT_WELL_FORMED_XML`: the whole file fails to parse. Fix this first.
- `ESCAPED_MARKUP_OUTSIDE_CDATA_SCOPE`: escaped tags in a string that is
  *not* CDATA in the source (such as `<u>Need help?</u>`). This is out
  of scope, so mention it to the user but only fix it if they ask.

If the user named specific languages or strings, limit your work to
those.

### 2. Snapshot before editing

The verify step compares against the untouched file, so save it first:

```bash
mkdir -p <scratch>/cdata
cp app/src/main/res/values-<lang>/strings.xml <scratch>/cdata/<lang>.before.xml
```

(`git show HEAD:app/src/main/res/values-<lang>/strings.xml` also works if
the file has no uncommitted changes.)

### 3. Rebuild each damaged string

Read the source string and the translated string side by side, then
rewrite **only the body of that one `<string>`**. Leave the opening tag,
its attributes, the other strings, comments and whitespace outside the
body byte-for-byte identical, because the verifier checks this.

Take the structure from the source and the words from the translation:

1. Wrap the body in `<![CDATA[` … `]]>`, with the same layout as the
   source (the source puts the HTML on indented lines inside the CDATA).
2. Use the source's tag skeleton: the same `<html>/<body>/<h1>/<ul>/<li>/<p>`
   nesting and the same `<br/>` breaks. Map each translated paragraph
   or list item to its source counterpart, in order.
3. Turn half-escaped and escaped tags into real tags (`<b&gt;` → `<b>`,
   `&lt;/p&gt;` → `</p>`).
4. Where the translator used `\n` or `\n\n` as a line break, use the
   source's `<br/>` or `<br/><br/>` at that spot. A bare `\n` that is
   only source formatting can become a real newline or be dropped.
5. Write attribute quotes the way the source does (`style=&quot;margin-left: 40px&quot;`,
   `color=&apos;#ff6347&apos;`) and keep attribute values unchanged.
6. Make sure the content never contains `]]>`, which would end the
   CDATA early.

Example (vi, `grant_all_files_permission`):

```xml
<!-- before: no CDATA, \n instead of <br/> -->
<string name="grant_all_files_permission"><html><body>Amaze cần quyền truy cập tất cả tệp để tiếp tục.
        \nỞ màn hình tiếp theo, vui lòng chọn <b>Amaze</b> và chọn tuỳ chọn <b>Cho phép quản lý tất cả tệp</b>.
            \n\n<font color='#ff6347'><i>Việc huỷ hộp thoại này sẽ thoát ứng dụng.</i></font></body></html></string>

<!-- after: source skeleton, identical Vietnamese text, translator's extra <b>Amaze</b> kept -->
<string name="grant_all_files_permission"><![CDATA[
        <html>
            <body>
                Amaze cần quyền truy cập tất cả tệp để tiếp tục.<br/><br/>
                Ở màn hình tiếp theo, vui lòng chọn <b>Amaze</b> và chọn tuỳ chọn <b>Cho phép quản lý tất cả tệp</b>.<br/><br/>
                <font color=&apos;#ff6347&apos;><i>Việc huỷ hộp thoại này sẽ thoát ứng dụng.</i></font>
            </body>
        </html>
    ]]></string>
```

### 4. Verify every edited file

```bash
kotlin .claude/skills/fix-translation-cdata/scripts/cdata_check.main.kts verify \
  <scratch>/cdata/<lang>.before.xml \
  app/src/main/res/values-<lang>/strings.xml \
  app/src/main/res/values/strings.xml
```

It fails (exit code 1) if:

- the file is not well-formed XML
- anything outside the `<string>` bodies changed
- the visible text of any changed string differs (it prints where)
- a link target disappeared
- a source-CDATA string still lacks CDATA or still has escaped markup

Visible text means the body with tags removed, `\n`/`\'`/`\"` and
entities decoded, and whitespace collapsed. So adding `<br/>` or
re-indenting passes, while changing even one letter or punctuation mark
fails. `note` lines list the remaining markup differences from the
source. Read each one and either fix it or confirm it is the translator's
own formatting.

If verify reports a text change, undo that edit. Don't adjust the text
to make it pass.

Then rerun the audit to confirm that the strings you handled are gone
from it, and check that resources still compile:

```bash
./gradlew :app:mergePlayDebugResources
```

### 5. Report

Tell the user:

- which `values-*/strings.xml` files and strings you changed, and what
  was wrong with each (one line per string)
- the verify result for each file
- any `note` differences you left on purpose, and why
- any out-of-scope findings from the audit
- that these files are synced from Transifex (`transifex.yml`), so the
  same markup should be fixed there too, or the next pull will bring
  the damage back
- how to find this themselves next time: run the `audit` command above,
  or look for `values-*` strings whose source in `values/strings.xml`
  starts with `<![CDATA[` but whose translation doesn't, or that contain
  `&gt;`

If the user wants commits, follow `AGENTS.md`: make one commit per
coherent fix, with a summary like
`translations: Restore CDATA HTML in vi grant_all_files_permission.`,
and include the verify output summary in the body.
