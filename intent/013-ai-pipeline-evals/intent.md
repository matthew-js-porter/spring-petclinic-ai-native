# Intent: AI Pipeline Evals
Author: matthew-js-porter

## Problem
Most of this project's SDLC runs through AI steps: `/intent` and `/spec` draft the documents, `design-spec.yml` opens spec PRs, `claude-code-review.yml` reviews every PR against `REVIEW.md`, `@claude` pushes fixes, `maintain-scan.yml` turns Snyk findings into intents, and agents implement specs. Nobody can say how well any of these steps work, or notice when they get worse. A change to `REVIEW.md`, `CLAUDE.md`, a skill, a workflow prompt, or the underlying model can quietly make reviews miss bugs or specs miss requirements, and the first sign is a defect a human happened to catch. Intent 002 deferred the playbook's Test-stage eval suite to "a separate future intent"; this is it.

The repo's own history shows what the steps must catch. Real mistakes were found and fixed by hand: saving the `Owner` aggregate renumbers pet ids; an update without the owner check would let one owner's URL change another's pet; seed inserts without an identity reset break the first insert; binding a typed form field shows Spring's generic error message (fixed in spec 010); a session id leaked into redirect URLs (009); house rules such as no `application.properties` and no intent references in comments.

## Proposed outcome
Each AI step in the pipeline has a small, repeatable eval that produces a score, so a change to a prompt, skill, rule file or model shows whether that step got better or worse.

Observable results:
- **Code review:** given PRs that reintroduce known defects from this repo's history, the reviewer flags each one; given clean, already-merged PRs, it raises few or no false findings. Recall and false-positive rate are reported.
- **Spec generation:** given past intents (004 onwards), the generated spec covers every observable outcome in the intent, keeps out-of-scope items out, has the required sections and no Status field, follows the design system, and spots the known traps for that intent. Scored against the human-reviewed specs.
- **Spec to code:** given a merged spec at the commit before its implementation, an agent's implementation builds and passes the tests from the real merged PR, which it never saw. The pass rate is reported.
- **Maintain scan:** given a fixture scan with confirmed findings, false positives and a duplicate of an open intent, it files intents only for confirmed, new findings, quotes evidence and redacts secrets.
- **@claude fixes:** given a review comment or a failing build, the fix makes the build pass, changes only relevant files and never touches `main`.
- **`/intent` skill:** given a loose problem statement, it asks at most one round of questions, produces about a page, names no classes or files, and records unknowns as open questions.
- Evals run automatically on PRs that change `REVIEW.md`, `CLAUDE.md`, `.claude/skills/`, or the Claude workflow prompts, and weekly to catch model changes. They can also be run by hand.
- Results are posted where the PR author sees them, with each step's score next to its previous baseline, and are kept so trends are visible over time. Evals are advisory: a low score never blocks a merge.
- The case sets are versioned in the repo, and adding a case for a newly found defect is a documented, small change.

Out of scope:
- Blocking merges on eval scores (a later decision once baselines are stable).
- Evaluating the PetClinic app itself; its behaviour stays covered by the existing tests.
- Deploy, rollback and runtime monitoring (still deferred from 002).
- Choosing or comparing other models or providers.

## Affected users and systems
- The maintainer and reviewers who rely on AI reviews and specs to approve work.
- The Claude workflows (`claude-code-review.yml`, `claude.yml`, `design-spec.yml`, `maintain-scan.yml`) and the `/intent` and `/spec` skills, as the things being measured.
- `REVIEW.md`, `CLAUDE.md` and the design system, whose rules the graders check.
- GitHub Actions: new workflow runs and their output on PRs.
- Git history and merged PRs (#43, #45 and earlier), as the source of test cases.

## Constraints
- Keep runs small: a small fixed case set per step, using the same subscription OAuth token as the other Claude workflows. The spec sets a concrete per-run limit and the trigger rules that keep weekly and per-PR cost within it.
- Evals never push to `main`, never merge anything, and never comment on or change real open PRs or issues; they work on fixtures or throwaway branches.
- No secrets, tokens or personal data in case files or results.
- Third-party actions pinned to a commit SHA; workflows pass `actionlint`. Claude workflows on a PR that changes them skip until merged, so the spec must say how eval workflow changes themselves are checked.
- Results must be reproducible enough to compare runs: the same cases, the model recorded with each result, and graders that are plain checks where possible and a written rubric where judgement is needed.

## Open questions
- Exact score thresholds that count as a regression worth flagging, and when evals might become blocking. Owner: Matthew, after a few weeks of baselines.
- Where historical results live (repo file, workflow artifacts, or elsewhere). Owner: spec.

Resolved in review: all six AI steps are in scope; evals are advisory in CI (on PRs that change prompts, rules or skills, plus weekly, and by hand); runs are kept small on the existing subscription token.
