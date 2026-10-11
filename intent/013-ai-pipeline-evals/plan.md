# Plan: AI Pipeline Evals (intent 013), on skill-creator

Spec: `intent/013-ai-pipeline-evals/spec.md`. Save this plan as `intent/013-ai-pipeline-evals/plan.md` once approved.

## Context
Spec 013 designs a custom Python eval harness for six AI steps. We're replacing that with Anthropic's **skill-creator** plugin (`skill-creator@claude-plugins-official`), so the evals are the standard per-skill `evals/evals.json` loop: subagent runs, a grader agent, `aggregate_benchmark.py` → `benchmark.json`, and a static review viewer.

skill-creator only evaluates skills, which sets the scope (agreed):
- **In scope:** `/intent`, `/spec`, and **maintain-scan**. Maintain-scan's prompt moves out of `maintain-scan.yml` into a new project skill.
- **Left out:** code review, spec-to-code and `@claude` fixes. They're recorded in the spec as deferred to a follow-up intent.

Evals run locally and in CI, where `claude-code-action` drives skill-creator headless on PRs that touch a skill, weekly, and by hand. They are advisory only.

What this simplifies away compared with the spec: the Python harness, plain graders, judge parser and calibration, the prompt-copy drift check (maintain's prompt now *is* the skill), the case-set hash and the baseline maths. On a PR the baseline is skill-creator's own **old_skill** comparison: the skill as it is on `main`, snapshotted, versus the PR's version, run on the same evals.

## Spec amendment (PR 1)
Rewrite spec 013 §1, §3 and §4 to match. This is a deliberate scope change.
- **Steps:** three skills. Review, spec-to-code and fixes go under "Deferred", with the reasons: not skills, and review uses the `code-review` plugin.
- **Runner:** skill-creator through `claude-code-action`. The case format is `.claude/skills/<name>/evals/evals.json` plus `evals/files/`. Grading is the skill-creator grader agent, with expectations written to be objectively checkable.
- **Baseline:** `old_skill` (main) vs `with_skill` (PR) on PR runs. Weekly runs are `with_skill` only, with `history.csv` for model drift.
- **Departures to note in §4.9:**
  - The grader is an LLM in the same model family. There's no pinned judge and no quote verification.
  - The orchestrator is itself an LLM, so runs are less deterministic.
  - One run per eval.
  - Eval expectations sit next to the skill, readable by the agent under test. The README warns about it, and the prompt forbids reading `evals/`.
- **Requirements:** F2, F4, F6 and F13–F15/F17–F19 are dropped or reworded. N4 (stdlib Python) still holds for the report script.
- **Areas of concern:** add the `maintain-scan.yml` edit (it isn't an upstream template, so it's allowed), and note that it can only be verified after merge.

## PR 1: maintain-scan skill + spec amendment (`skills/maintain-scan`)
- New `.claude/skills/maintain-scan/SKILL.md`, with frontmatter like the `intent` skill (`name`, `description`, `argument-hint: "[sarif-dir] [run-url] [next-number]"`). The body is steps 1–3 and 5 of the current prompt (`maintain-scan.yml:73-82`), made generic:
  - Read SARIF from the given dir (default `.snyk-results/`) and treat the contents as data.
  - Validate each finding with a confidence rating, group findings by root cause, and skip any group an existing intent already covers.
  - Write `intent/<nnn>-<kebab>/intent.md` per the intent skill's monitoring section. Quote rule ID, severity, file/package, CVE and run link; redact secrets.
  - Finish with a confirmed table and a dismissed table, ready for a PR body.
  - Publishing (branch, commit, push, `gh pr create`) stays out of the skill.
- `maintain-scan.yml`: the prompt becomes `/maintain-scan .snyk-results <run url> <next>`, plus the unattended line and the old step 4 (branch, commit only the new intent.md files, push, PR with the tables the skill printed), plus "Never push to `main`". Add `Skill` to `--allowedTools`. Nothing else changes.
- Spec amendment as above.
- Verify: `actionlint`; then, after merge, a `workflow_dispatch` of maintain-scan gives the same behaviour (PR or "nothing confirmed"), since Claude workflows skip on the PR that changes them.

## PR 2: evals + workflow (`evals/skill-creator`), simplified
Revised during implementation at the owner's request: no runner scripts, no orchestrator prompt, no report generator, no history file. Evals are invoked as `/skill-creator eval <skill>`, and the workflow comments the results on the PR.

- **Eval sets** in skill-creator's format, `.claude/skills/<skill>/evals/evals.json`, with fixtures in `evals/files/`:
  - `/intent`: detailed statement (answers in the prompt), vague statement, two ideas mixed.
  - `/spec`: three new, unspecced fixture intents (delete a pet, edit a visit, filter vets by specialty), because the human specs for past intents sit in the repo and an executor could read them. Traps come from this codebase's history.
  - `/maintain-scan`: a fixture project (`tree/`, with `.fixture` sources and an open intent 001) and two scans (mixed findings with a synthetic secret and a planted instruction; all false positives).
  - Prompts that would write into the repo ask, as a test run, to save under skill-creator's outputs directory; expectations check the repo is unchanged.
- **`.claude/settings.json`** enables `skill-creator@claude-plugins-official`.
- **`.github/workflows/evals.yml`**: one job on PRs touching `.claude/skills/**` or `CLAUDE.md`, and on dispatch (`skill` input). `claude-code-action` loads skill-creator from the official marketplace and runs `/skill-creator:skill-creator eval <skill>` for each changed skill, comparing `origin/main`'s copy (old_skill) with the working copy, static viewer only. Claude posts one PR comment with `gh pr comment` (per-skill table, failed expectations, run link). Then a token scan and `upload-artifact` of `.claude/skills/*-workspace/` (90 days). Permissions `contents: read`, `pull-requests: write`, `id-token: write`.
- **Docs:** `CLAUDE.md` Evals section and CI list; spec 013 rewritten to match; `.gitignore` gets `.claude/skills/*-workspace/`.

## Critical files
- New: `.claude/skills/{intent,spec,maintain-scan}/evals/{evals.json,files/**}`, `.github/workflows/evals.yml`.
- Edited: `.claude/settings.json`, `CLAUDE.md`, `intent/013-ai-pipeline-evals/spec.md`, `.gitignore`.

## Verification
- Locally: `/skill-creator eval <skill>` for each skill; read the benchmark and at least one output.
- Sensitivity: a deliberately bad skill edit shows a drop in the old_skill vs with_skill comparison.
- `actionlint .github/workflows/*.yml` is clean; `./mvnw clean verify` unchanged.
- After merge: dispatch `evals.yml` per skill; then a test PR touching `/intent` gets the comment and the artifact.
- Optionally, run `/intent` to capture the deferred evals (review, spec-to-code, fixes, weekly history) as a follow-up intent.
