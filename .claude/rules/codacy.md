# Pass Codacy Static Analysis

Every PR runs the "Codacy Static Code Analysis" check, and it fails on
a single new issue of any severity, including style notices. Most of
Amaze is Android code (Kotlin, Java and XML resources under
`*/src/`), so that is where new issues matter most, but no changed
file in any language may introduce one.

## Android code (Kotlin and Java)

- Start from the project's own checks: `./gradlew spotlessCheck`
  (ktlint for Kotlin, google-java-format and unused import removal
  for Java, license headers), which CI enforces, and `./gradlew
  :app:lint`, which does not fail the build (`abortOnError false`), so
  read its report for warnings in the files you changed. Fix
  formatting with `./gradlew spotlessApply`, and never weaken the
  ktlint settings in `.editorconfig` to make a check pass.
- Codacy also runs its own analyzers for Kotlin and Java on top of
  those. Its rule set is configured on Codacy, not in this repo, so
  check the repository's Code patterns page on app.codacy.com when you
  are unsure whether a construct is allowed.
- Write code that common analyzer rules accept from the start:
  - no unused imports, variables, parameters or private members;
  - no empty or swallowed `catch` blocks: handle, rethrow or log the
    exception;
  - no wildcard imports;
  - no `!!` in Kotlin where a safe call, `requireNotNull` or an early
    return states the intent;
  - named constants instead of unexplained literals;
  - short, focused functions, because Codacy reports per-file
    complexity increases on every PR.
- Avoid the Android security patterns analyzers flag: SQL built by
  string concatenation (use selection arguments), hardcoded secrets,
  logging file paths or credentials, world-readable or
  world-writable files, and enabling JavaScript or file access in a
  `WebView` without need.
- Keep resource XML valid and consistent with the source
  `values/strings.xml`, including CDATA-wrapped HTML strings.

## Other files

Scripts and tooling (Python, shell, workflows) are checked too. For
Python, that includes Bandit (security) and pydocstyle, configured to
put a multi-line docstring's summary on the second line (D213). Run
`pydocstyle <file>` and `bandit <file>` on changed Python files.

## Always

- Fix the cause instead of silencing the rule. Do not add
  suppression annotations or comments (`@Suppress`,
  `@SuppressWarnings`, `@SuppressLint`, `// NOPMD`, `# nosec`,
  `# noqa`, ...) or ask for a rule to be disabled unless a
  maintainer agrees.
- When a fix would add a dependency, check first whether the code
  promises to avoid that dependency, and prefer an equivalent fix
  with what is already available (the Android SDK, existing project
  dependencies or the language's standard library).
- When the check fails, read its findings with file and line from
  `https://api.github.com/repos/TeamAmaze/AmazeFileManager/check-runs/<check_run_id>/annotations`,
  where `<check_run_id>` is the number at the end of the check's URL,
  and fix every one of them in the commit that introduced it.
- Before finalizing, confirm the change adds no new Codacy static
  analysis issues.
