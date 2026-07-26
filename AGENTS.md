# Guidelines for AI Contributions to Amaze File Manager

This file provides guidance to AI coding assistants for contributing
to the codebase. These guidelines are designed to produce 
contributions that meet the same high standards we expect from human
contributors. The original reference for this file is the Zulip
CLAUDE.md file, we thank the Zulip team for their work.

## Philosophy

### No detail is too small

Amaze holds itself to a high bar for polish because users depend on
this software daily, and because the project is built to last for
decades. There is no category of "minor issue" that is acceptable
to ship, if something is broken in any context where a user would
encounter it, it must be fixed before merging. The project's
extensive investment in testing, tooling, and review processes exists
precisely so that these issues get caught and fixed, not so that they
can be classified as low-priority and deferred.

The right attitude is: "What could go wrong, and how do I verify that
it doesn't?" not "It looks fine to me." **What isn't tested probably
doesn't work** - this applies to visual changes just as much as to
backend logic.

### Understand before coding

Before writing any code, you must understand:

1. What the existing code does and why, including the relevant help center or
   developer-facing documentation.
2. What problem you're solving, in its full scope.
3. Why your approach is the right solution, and available alternatives.
4. How you will verify that your work is correct, and avoid regressions
   that are plausible for the type of work you're doing.

The answer to "Why is X an improvement?" should never be "I'm not sure."

## Workflow

Follow this workflow for every task: **understand → propose → implement → verify**.

### 1. Understand Before Coding

Before making any changes:

```bash
# Look at existing code patterns
git grep "similar_function_name"
git log --oneline -20 -- path/to/file.py

# Check for related issues on GitHub
```

Always show existing similar code and explain how it works before proposing
changes.

### 2. Propose an Approach

Before writing code, explain the plan:

- Explain your understanding of the problem and all relevant design decisions
- What changes are needed and why
- How the changes fit with existing patterns
- What could break and how to prevent regressions

### 3. Implement in Minimal, Coherent Commits

Each commit should be self-contained, highly readable and reviewable
using `git show --color-moved`, and pass lint/tests independently. If
extracting new files or moving code, always do that in a separate
commit from other changes.

### 4. Verify Before Finalizing

Run tests before making a commit. Always manage your time by running
specific test collections, not the entire test suite.

## Before You Start

### Read the Relevant Documentation

Before working on any area:

- Read the @CONTRIBUTING.md guidelines.
- Read existing code in the area you're modifying.
- Use `git grep` to find similar patterns in the codebase and read those.

### Understand the Code Style

- **Be consistent with existing code.** Look at surrounding code and follow
  the same patterns, as this is a thoughtfully crafted codebase.
- **Use clear, greppable names** for functions, arguments, variables, and
  tests. Future developers will `git grep` for relevant terms when
  researching a problem, so names should communicate purpose clearly.
- Keep everything well factored for maintainability. Avoid duplicating
  code, especially where access control or subtle correctness is involved.
- Prefer writing code that is readable without explanation over heavily
  commented code using clever tricks. Comments should explain "why" when
  the reason isn't obvious, not narrate "what" the code does.
- Comments generally should have a line to themselves.

## Commit Discipline

### Each Commit Must:

1. **Be coherent**: Implement one logical change completely and atomically.
2. **Pass tests**: Include test updates in the same commit as code changes.
3. **Not make Amaze worse**: Work is ordered so no commit has regressions.
4. **Be safe to deploy individually**: Or explain in detail why not.
5. **Be minimal** and **reviewable**: Don't combine moving code with changing
   it in the same commit; make liberal use of small prep commits for
   no-op refactoring that are easy to verify.

### Never:

- Mix multiple separable changes in a single commit.
- Create a commit that "fixes" a mistake from an earlier commit in the same PR;
  always edit Git to fix the original commit.
- Add content in one commit only to remove or move it in the next;
  plan upfront what belongs where and do it right the first time.
- Include debugging code, commented-out code, or temporary TODOs.
- Leave commits that break if a later commit in the PR is dropped.
  When a commit is flagged as potentially droppable, verify all
  earlier commits work correctly without it.

### Commit Message Format

```
subsystem: Summary in 72 characters or less.

The body explains why and how. Include context that helps reviewers
and future developers understand your reasoning, analysis, and
verification of the work above and beyond CI, without repeating
details already well presented in the commit metadata (filenames,
etc.). Explain what the change accomplishes and why it won't break
things one might worry about.

Line-wrap at 68-70 characters, except URLs and verbatim content
(error messages, etc.).

Fixes #123.
```

**Commit summary format:**

- Example: `Fix cursor position after emoji insertion`
- Example: `Refactor immutable cache headers`
- Bad examples: `Fix bug`, `Update code`, `gather_subscriptions was broken`

**Linking issues:**

- `Fixes #123` automatically closes the issue
- `Addresses #123` does not close (for partial fixes)
- In a multi-commit PR, use `Addresses #123` in earlier commits
  and `Fixes #123` in the final commit.

### Rebasing Commits (Non-Interactive)

Since `git rebase -i` requires an interactive editor, use
`GIT_SEQUENCE_EDITOR` to supply the todo list via a script:

1. **Updating the HEAD commit:** If the commit you need to modify is
   already at HEAD, just use `git commit --amend` directly. The
   fixup+rebase workflow below is only needed for non-HEAD commits.

2. **Squashing fixups into existing commits:** Create fixup commits with
   `git commit --fixup=<target-hash>`, then write a shell script that
   outputs the desired todo (with `pick` and `fixup` lines in order)
   and run:

   ```bash
   GIT_SEQUENCE_EDITOR=/path/to/todo-script.sh git rebase -i <base>
   ```

   Note: `--autosquash` alone without `-i` does **not** reorder or
   squash anything.

3. **Rewording commit messages:** Use `git format-patch` to export
   commits as patch files, edit the message headers in the patch
   files, then reapply:

   ```bash
   git format-patch <base> -o /tmp/patches/
   # Edit the commit message in each /tmp/patches/000N-*.patch file
   # (the message is between the Subject: line and the --- line)
   git reset --hard <base>
   git am /tmp/patches/*.patch
   ```

## Testing Requirements

Automated code generation must include tests for all components
to prevent regressions.

There are three kinds of tests:
- Unit tests: Test single functions, simple functionality, very
  easy to run, if at all possible, these should be used to test
  functionality.
- Headless tests: Robolectric is used for testing anything that
  doesn't require a phone, it allows for extensive mocking and 
  UI interaction simulation without the full UI overhead.
- Emulator tests: Using Espresso and other tools, very heavy to
  run, create these tests as a last resort.

### Before Submitting:

Check that the new features, or fixed bugs are being properly tested
for regressions.

### Testing Philosophy:

- Write end-to-end tests when possible verifying what's important.
- A good failing test before implementing is good practice so your
  test and code can jointly verify each other.
- Remember to always assert state is correctly updated, not just "success".

## Self-Review Checklist

Before finalizing, verify:

- [ ] The PR addresses all points described in the issue
- [ ] All relevant tests pass locally
- [ ] Code follows existing patterns in the codebase
- [ ] Names (functions, variables, tests) are clear and greppable
- [ ] Commit messages, comments, and PR description are well done
- [ ] Each commit is a minimal coherent idea
- [ ] No debugging code or unnecessary comments remain
- [ ] Type annotations are complete and correct
- [ ] User-facing strings are in strings.xml
- [ ] User-facing error messages are clear and actionable
- [ ] No secrets or credentials are hardcoded
- [ ] Documentation is updated if behavior changes
- [ ] Refactoring is complete (`git grep` for remaining occurrences)
- [ ] Security audit of changes

Always output a recommend pull request summary+description that
follow's Amaze's guidelines once you finish preparing a series of
commits.

## Common Pitfalls

### Treating Known Issues as Acceptable

A common failure mode is discovering a problem during verification
and then noting it as a known limitation rather than fixing it.

**Mitigation:** When you find any issue during verification, fix it
before presenting the work. If a fix would require a design decision,
raise it as a question rather than shipping the broken state.

### Missing Test Updates

Tests must be in the same commit as the code they test.

**Mitigation:** Include test updates in each commit. Show what tests need to
change.

### Verbose Commit Messages

Commits are concise, say everything that's important for a reviewer to
understand about the motivation for the work and changes, and nothing more.
Avoid wordiness and details obvious to someone who is looking at the
commit and its metadata (lists of filenames, etc).

**Mitigation:** Keep summary under 72 characters. Body should explain why,
not what.

### Mixing Concerns

Multiple changes in one commit makes review difficult.

**Mitigation:** Each commit should do exactly one thing. Plan
necessary refactoring and preparatory commits in advance of functional
changes. You can split into good commits after the fact, but it's much
faster and easier to just plan and write them well the first time.

## Pull Request Guidelines

### PR Description Should:

When opening a pull request, prefix the PR title with `[ai]` (e.g.,
`[ai] compose: Fix cursor position after emoji insertion.`). Use
`upstream/main` as the base branch.

Output the PR description in a markdown code block so that formatting
(bold, headers, checkboxes, etc.) copy-pastes correctly into GitHub.

1. Start with a `Fixes: #...` line linking the issue being addressed.
2. Explain **why** the change is needed, not just what changed.
3. Describe how you tested the change, using checkbox format for the
   test plan (e.g., `- [x] ./tools/test-backend ...`).
4. Include screenshots for UI changes.
5. Link to relevant issues or discussions.
6. Call out any open questions, concerns, or decisions you are uncertain
   about, so they can be resolved during review.
7. Include the self-review checklist from
   `.github/pull_request_template.md` using checkbox format (`- [x]` /
   `- [ ]`), checking off all applicable items.

### PR Description Should Not:

- Regurgitate information visible from the diff
- Make claims you haven't double-checked
- Express more certainty than is justified given the evidence

## When to Pause and Discuss

Recommend pausing for discussion when:

- The approach involves security-sensitive code
- Database migrations are needed
- The change affects many files (>10)
- Performance implications are unclear
- The feature design isn't fully specified
- The API or data model design isn't fully specified
- Existing tests are failing for unclear reasons

## Task-Specific Approaches

### For Bug Fixes

1. Show the relevant code and explain what's happening
2. Brainstorm theories for how the bug might be possible
3. Analyze and propose a fix with a clear explanation
4. Write tests that would have caught this bug if possible
5. Format as a single commit following commit guidelines
6. Audit for whether the bug may exist elsewhere or might be
   re-introduced and propose appropriate changes to address if so.

### For New Features

1. Show similar existing features in the codebase
2. Propose an implementation approach before coding
3. Implement in minimal, coherent commits
4. Each commit must pass tests independently

### For Refactoring

1. Show the current implementation
2. Explain what makes it problematic
3. Propose the refactoring approach
4. Implement in commits that each leave the codebase working
5. No behavior changes unless explicitly discussed
6. Verify completeness: use `git grep` to find all occurrences and
   confirm nothing was missed

## Repository Structure Quick Reference

- `app` contains the main app
- `commons_compress_7z` is the 7z compression and decompression library 
- `file_operations` is a rust library which contain some of the low level
  operations needed by Amaze
- `portscanner` is a network library used by various systems in Amaze
- `scripts` is a directory that contains general tools for maintaining the 
  app
- `fastlane` is a directory that maintains the resources for FDroid and other
  open source app repositories 
