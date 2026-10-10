# Spec: GitHub Actions build and AI-native SDLC pipeline
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add GitHub Actions workflows that (a) build and test the app with Maven, (b) run Snyk security scans (dependencies and static code analysis), and (c) automate three stages of the AI-native SDLC playbook with `anthropics/claude-code-action`: Design (spec PR on intent merge), Review (advisory AI PR review) and Maintain (weekly security scan that files findings as intents). Agents only open PRs; humans merge. Out of scope: Test-stage evals, deploy/rollback, runtime monitoring, branch protection, code owners.

## 2. Existing codebase context
- Spring Boot 4.1.1, Maven wrapper (`./mvnw`), `java.version` 25 in `pom.xml`, `.sdkmanrc` pins `java=25.0.2-open` (Java.net OpenJDK build).
- `.claude/skills/` contains `intent` and `spec`; `.claude/settings.json` has only a `PostToolUse` hook on `ExitPlanMode`. There is no `.github/`, no `REVIEW.md`, no `CODEOWNERS`.
- `intent/<nnn>-<name>/` holds `intent.md`, `spec.md`, `plan.md` per change. Intent 001 is the only other one.
- `CLAUDE.md` governs agent behaviour (YAML-only config, minimal actuator, workflow rules). It is read by every Claude job, so it must stay concise.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | **Build.** A `build` workflow runs `./mvnw -B clean verify` on every push to `main` and every pull request, on Java 25, and fails if the build or any test fails. |
| F2 | **Design.** When a push to `main` adds or changes `intent/*/intent.md`, a workflow runs `/spec` non-interactively on that file and opens a PR from a new branch adding `spec.md` beside it. It does nothing if `spec.md` already exists in that folder. |
| F3 | **Review.** Every non-draft PR from a branch in this repo gets a Claude review following `REVIEW.md` (bugs, compliance with the folder's `spec.md`/`plan.md` where present) and a Claude Code `/security-review`, posted as inline PR comments, each tagged `[critical]`, `[major]` or `[minor]`. The review job never approves, requests changes, or fails the PR check because of findings. |
| F4 | **Fix loop.** An `@claude` mention by a user with write access on a PR comment triggers Claude to push a fix commit to that PR branch. |
| F5 | **Snyk.** A Snyk workflow runs dependency (Open Source) and code (SAST) scans on push to `main`, on PRs, and weekly, and fails on findings at or above `high` severity. Results are uploaded as SARIF to the Security tab when available (see concern C4). |
| F6 | **Maintain.** A weekly workflow collects the latest Snyk results, has Claude validate each (confidence rating, false-positive reasoning) and writes each confirmed finding as a new `intent/<nnn>-<name>/intent.md` in a PR. Dismissed findings are listed in the PR body with a recorded reason. |
| F7 | No workflow pushes directly to `main`. Claude-created commits arrive via branch and PR only. |
| F8 | Commits pushed by Claude to branches trigger the build workflow (so each agent PR is built). |

### Non-functional / constraints
- Java: CI uses `actions/setup-java` with `java-version: 25`, distribution `temurin`. `.sdkmanrc` is the Java.net build, so distribution differs (see C5). Maven cache enabled through `setup-java`.
- Secrets: `CLAUDE_CODE_OAUTH_TOKEN` and `SNYK_TOKEN` as repo Actions secrets; never in files. Passed only to steps that need them.
- Least privilege: each workflow declares top-level `permissions: {}` (or `contents: read`) and elevates per job.
- Third-party actions pinned to a full commit SHA (with a version comment), except GitHub-owned and `anthropics/claude-code-action`, which follows the docs' `@v1` tag and is the only floating reference; revisit pinning it too (see C6).
- Cost control for Claude jobs: `--max-turns`, `timeout-minutes`, and `concurrency` groups that cancel superseded PR reviews. The build workflow has no path filters, so it always runs on PRs and gives one consistent signal.
- Config remains YAML; workflow files are the only new files outside `intent/`, plus `REVIEW.md` at the repo root.

## 4. Design

### 4.1 Layout
```
.github/workflows/build.yml          # F1
.github/workflows/snyk.yml           # F5
.github/workflows/design-spec.yml   # F2
.github/workflows/claude-code-review.yml  # F3
.github/workflows/claude.yml         # F4
.github/workflows/maintain-scan.yml  # F6
REVIEW.md                            # review policy read by F3
```
One workflow per concern so each can be triggered, disabled and debugged independently.

### 4.2 Workflow details

**build.yml** — triggers `push: [main]`, `pull_request`. Steps: checkout, `setup-java` (temurin 25, `cache: maven`), `./mvnw -B clean verify`. Upload `target/surefire-reports` on failure. `permissions: contents: read`.

**snyk.yml** — same triggers/schedule. Uses `snyk/actions/maven` for `snyk test --severity-threshold=high --sarif-file-output=snyk-oss.sarif` and `snyk/actions/setup` + `snyk code test --sarif-file-output=snyk-code.sarif`. Always keep SARIFs as workflow artifacts (the Maintain job reads these). Upload them to the Security tab with `github/codeql-action/upload-sarif` (`if: always()`) only when code scanning is enabled (repo variable `SARIF_UPLOAD=true`). `SNYK_TOKEN` is not exposed to fork PRs (GitHub withholds it); on those the job is skipped via `if: github.event.pull_request.head.repo.full_name == github.repository || github.event_name != 'pull_request'`.

**design-spec.yml** — trigger `push: branches: [main], paths: ['intent/*/intent.md']`. Steps: checkout (full history for diff); compute changed intent files with `git diff --name-only ${{ github.event.before }} ${{ github.sha }}`; for each whose folder lacks `spec.md`, run `claude-code-action@v1` with `prompt: "/spec @<path>"`, `claude_args: --max-turns 30 --allowedTools "Bash(git:*),Bash(gh pr create:*),Read,Write,Edit"` and instructions to create branch `spec/<nnn>-<name>`, commit only `spec.md`, and open a PR. Authenticated with `claude_code_oauth_token`. Permissions: `contents: write`, `pull-requests: write`, `id-token: write`. Concurrency group per intent folder. The action authenticates as the Claude GitHub App (no `github_token` input) so F8 holds.

**claude-code-review.yml / claude.yml** (generated by `/install-github-app`, kept close to the template)
- `claude-code-review.yml`, job `claude-review`: trigger `pull_request: [opened, synchronize, ready_for_review, reopened]`; skip drafts and fork PRs. Runs the code-review plugin as in the Claude Code docs (`plugin_marketplaces`, `plugins: code-review@claude-code-plugins`, prompt `/code-review:code-review --comment <repo>/pull/<n>`, `claude_args: --allowedTools "mcp__github_inline_comment__create_inline_comment"`) with an added instruction to follow `REVIEW.md`. Permissions: `contents: read`, `pull-requests: read`, `issues: read`, `id-token: write`. `concurrency` cancels in-progress reviews of the same PR. `continue-on-error: true` so findings or Claude outages never block merging. `allowed_bots: claude` so fix commits pushed by `@claude` are reviewed too.
- `claude-code-review.yml`, job `claude-security-review`: same triggers, guards and permissions; runs `prompt: /security-review` with full history and `origin/HEAD` set to the base branch (the command diffs against it). Findings are posted as inline comments tagged `[critical]` (HIGH) or `[major]` (MEDIUM).
- `claude.yml`, job `claude`: the installer's triggers (`@claude` in issue/PR comments, review comments, review bodies, and new or assigned issues), interactive mode (no `prompt`). Permissions: `contents: write`, `pull-requests: write`, `issues: write`, `id-token: write`, `actions: read`. The action's own write-access check applies.

**maintain-scan.yml** — trigger `schedule` weekly (Mon 06:00 UTC, after the weekly Snyk run at Mon 03:00 UTC) plus `workflow_dispatch`. Steps: checkout; download the latest Snyk SARIF artifacts from the most recent `snyk` workflow run on `main` (`gh run download`); invoke `claude-code-action@v1` with a prompt instructing: validate each finding against the code, rate confidence (high/medium/low), skip with a recorded reason when a false positive, and for each confirmed finding create `intent/<next nnn>-<kebab>/intent.md` using the intent template (finding quoted, secrets redacted) on branch `maintain/security-<date>`; open one PR. Permissions: `contents: write`, `pull-requests: write`, `actions: read`, `id-token: write`. `--max-turns` capped; exits cleanly with no PR if there are no open findings. Because the schedule trigger has no human actor, the "scheduled-run attributed to last editor of the cron line" rule applies (see C7).

### 4.3 REVIEW.md
Short policy (~40 lines): two passes (bugs; spec/plan conformance, `CLAUDE.md` rules such as YAML-only config and actuator exposure beyond `health`); severity definitions; instruction to report only issues with a concrete failure scenario; findings are advisory.

### 4.4 Repository setup (manual, one-time)
1. Install the Claude GitHub App on the repo (or run `/install-github-app`).
2. Run `claude setup-token`; add the result as secret `CLAUDE_CODE_OAUTH_TOKEN`.
3. Create a Snyk API token; add as secret `SNYK_TOKEN`.
4. Optional: enable code scanning (Settings → Code security) if the SARIF upload to the Security tab is wanted (see C4). Not required for the pipeline to work.
5. Settings → Actions → General: allow actions from GitHub and the listed marketplace actions; set workflow permissions to read-only default.
Branch protection is explicitly out of scope; without it, the human-merge gate (F7) is a convention rather than an enforced control (see C1).

## 5. Acceptance and verification
| Check | How |
|-------|-----|
| F1 | Open a PR with a deliberately failing test; `build` fails. Revert; it passes. `main` push also triggers it. |
| F2 | Merge a throwaway `intent/999-test/intent.md` to `main`; a PR appears adding `intent/999-test/spec.md`. Re-run: no duplicate PR. |
| F3 | Open a PR containing an obvious bug; inline comments appear with severity tags; the check stays green. |
| F4 | Comment `@claude fix …` on a PR; a commit lands on the PR branch and `build` runs on it (F8). |
| F5 | Push a known-vulnerable test dependency on a branch; Snyk fails the job and the results are visible (workflow artifacts, and the Security tab if enabled). Remove it. |
| F6 | Trigger `workflow_dispatch`; with at least one open alert a PR adds an `intent.md` per confirmed finding; with none, no PR is opened. |
| F7 | Inspect workflow runs: no `git push origin main` anywhere; `grep -r "push origin main\|HEAD:main" .github` returns nothing. |
| Lint | `actionlint` passes on all workflows. |
| Secrets | `git grep -i "sk-ant\|SNYK_TOKEN="` shows only `${{ secrets.* }}` references. |

## 6. Policy conformance
I looked for brand-guideline, security-policy and UX-standard skills or documents. None exist in the repo or the available skills (only `intent`, `spec`, Spring tooling and general utilities), so no formal policy could be checked. I applied these defaults, which a policy owner should confirm:
- **Security:** least-privilege `permissions`, SHA-pinned third-party actions, secrets only via Actions secrets, no secrets exposed to fork PRs, agent output only via PRs, Snyk scanning, findings redacted before being written into committed intents.
- **Brand / UX:** not applicable (no UI). Review comments use a plain `[severity]` prefix convention.

## 7. Areas of concern

- **C1 — Human gate is not enforced.** The playbook says "the agent may act up to the production gate but cannot pass it". Branch protection is out of scope per the intent, so nothing technically stops a push to `main` by a person or by a mis-scoped workflow. Mitigations in this spec (no push-to-main steps, PR-only agent flow, F7 check) are convention only. Recommend a follow-up intent for branch protection.
- **C2 — Subscription token for CI.** `CLAUDE_CODE_OAUTH_TOKEN` is tied to the personal Pro subscription of whoever ran `claude setup-token`; the docs recommend an API key for shared or organisational use. CI usage consumes the same Pro allowance as interactive use and may be rate-limited, causing flaky review/spec/scan runs. Review is made non-blocking (`continue-on-error`) so this never gates merges. Whether a Pro subscription is permitted for automated CI use under Anthropic's terms should be confirmed by the owner.
- **C3 — Prompt injection.** PR review and `@claude` jobs process untrusted text (PR diffs, comments, scan results written into intents). Mitigations: same-repo PRs only, write-access check on mentions, narrow `--allowedTools`, no secrets other than the OAuth token in the job, and human review of every PR. Residual risk accepted; the maintain job (which writes files from scan content) is the most exposed, and its output is gated only by human PR review.
- **C4 — Snyk SARIF / code scanning availability.** Uploading SARIF to the Security tab requires GitHub code scanning, which is free for public repositories but needs GitHub Advanced Security on private ones. Repo visibility is not stated in the intent. SARIF upload is therefore optional, and the SARIF workflow artifact is the source of truth for the Maintain job. With CodeQL dropped, Snyk Code is the only static analysis. Snyk Code (SAST) may also not be enabled on the Snyk plan; if it is not, F5 reduces to dependency scanning only.
- **C5 — JDK 25 and distribution mismatch.** The Snyk CLI's Maven support on Java 25 must be verified at implementation time; if either does not support 25, the fallback is to run that scan on the newest JDK it supports, and to record the deviation in this spec. The Maven build itself stays on 25. CI uses Temurin 25 while `.sdkmanrc` uses the Java.net build; a minor-version difference could produce differing results.
- **C6 — Floating action reference.** `anthropics/claude-code-action@v1` is floating and runs with write access and the OAuth token, conflicting with the SHA-pinning policy applied elsewhere. The Claude docs only show `@v1`. Recommend pinning to a SHA once the first working version is chosen and updating via Dependabot.
- **C7 — Scheduled-run actor.** Scheduled workflows are attributed to the user who last changed the cron line; the action rejects bot actors. If a bot (or the Claude app) edits the schedule, the Maintain job fails until it is added to `allowed_bots`. Also, GitHub disables schedules in public repos after 60 days without activity.
- **C8 — Design and Maintain depend on `/spec` and the intent template working unattended.** The `/spec` skill was written for interactive use. If it asks questions, the non-interactive run will stall or guess. The prompt must instruct it not to ask and to list assumptions in the spec. Specs produced this way still require full human review before any plan, as the playbook requires.
- **C9 — Intent numbering races.** Maintain and Design may run while other branches add intents with the same next number. Collisions are caught at PR review, as the `intent` skill already allows.
- **C10 — Pro usage limits vs. volume.** Review on every PR synchronise event can burn the allowance quickly. The spec cancels superseded runs and caps turns, but if limits are hit the owner should consider review-on-demand (label or `ready_for_review` only).

## 8. Handoff notes for engineering
- Suggested order: `build.yml` → `snyk.yml` → `REVIEW.md` + `claude-code-review.yml`/`claude.yml` → `design-spec.yml` → `maintain-scan.yml`, each in its own PR, so every PR is checked by the pipeline that already exists.
- Complete the one-time setup (4.4) before merging the Claude and Snyk workflows; they fail visibly without their secrets.
- Estimated effort: about one day, mostly verifying Snyk on Java 25 and tuning the Claude prompts.
- Record the pinned action SHAs and Java/Temurin versions in the PR descriptions.
- Update `CLAUDE.md` with a short "CI" section (workflow list, how to run `actionlint`, no push to `main`) once the workflows exist.

## 9. Implementation deviations
Recorded while implementing `plan.md` (PRs #2-#7).
- **Snyk runs via the CLI, not `snyk/actions/maven` (C5 resolved).** `snyk.yml` uses `actions/setup-java` (Temurin 25) plus `snyk/actions/setup` (pinned v1.0.0 SHA) and calls `snyk test` and `snyk code test` directly, so Snyk resolves dependencies on the same JDK as the build. Both scans were verified on Temurin 25.0.4 in CI. Both scans always run and upload SARIF, and a final step fails the job if either found issues.
- **Snyk Code is enabled on the account (C4 resolved for SAST).** The first run reported 0 code issues. Security-tab upload remains opt-in via `SARIF_UPLOAD=true` (the repo is public, so code scanning is free).
- **Snyk fails on current `main`.** Snyk Open Source reports high/critical issues in `tomcat-embed-core`/`tomcat-embed-websocket` 11.0.24 and `jackson-core`/`jackson-databind` 3.1.5, pulled in by Boot 4.1.1. This is F5 working as specified. The `snyk` check stays red until the dependencies are upgraded, and the first `maintain-scan` run should file these as intents.
- **F3/F4 keep the `/install-github-app` workflows** (`claude-code-review.yml`, `claude.yml` from PR #1) instead of a custom `review.yml`, with minimal changes. `claude-code-review.yml` adds the draft/fork guard, timeout, concurrency, `continue-on-error`, `--max-turns`, `allowed_bots: claude`, and `REVIEW.md` via `--append-system-prompt` (the code-review plugin only reads `CLAUDE.md` itself). `claude.yml` gets write permissions and `--max-turns`; it keeps the installer's extra triggers (`@claude` in new or assigned issues and in review bodies), which go beyond F4.
- **Security review uses `/security-review`, not `REVIEW.md`.** The security pass was removed from `REVIEW.md`; a second job runs Claude Code's built-in `/security-review` on each PR. Repo-specific checks it does not target (workflow permissions, action pinning) are no longer an explicit review item.
- **Claude's fix commits are reviewed.** Reviews of `@claude` commits can lead to further `@claude` fixes, but only when a human mentions `@claude` again, so the loop is not automatic.
- **Claude jobs skip on PRs that add or change their workflow.** claude-code-action refuses to run unless the workflow file is identical to the one on the default branch. So F2-F4 and F6 can only be verified after each workflow is merged, and a PR that edits a Claude workflow gets no Claude review of itself.
- **`design-spec.yml` additions.** A `workflow_dispatch` trigger scans all intents for a missing `spec.md` (backfill and re-runs). Folders that already have a `spec/<nnn>-<name>` branch are also skipped, which prevents duplicate PRs while a spec PR is open (F2 re-run check).
- **`design-spec.yml` re-dispatches itself (F2).** `claude-code-action@v1` rejects `push` events ("Unsupported event type: push", first seen when intent 003 merged). On a push, `detect` finds the changed intents and a `dispatch` job (`actions: write`) runs `gh workflow run design-spec.yml -f folder=<dir>` for each; the `spec` job runs only on `workflow_dispatch`. A manual dispatch with an empty `folder` still scans every intent. `GITHUB_TOKEN` may trigger `workflow_dispatch`, so no extra token is needed. The dispatched run's actor is then `github-actions[bot]`, which the action rejects as a non-human actor, so the `spec` job sets `allowed_bots: github-actions`; only this workflow's own dispatch (or a user with write access) can start it.
- **`maintain-scan.yml`** groups findings with a shared root cause into one intent and skips problems already covered by an existing intent. It does not invoke Claude when the latest `main` scan has no results or no artifact.
- **Action pinning (C6).** The only third-party action besides claude-code-action is `snyk/actions/setup`, which is SHA-pinned. `anthropics/claude-code-action@v1` stays floating; pin it to a SHA once the post-merge checks pass.

