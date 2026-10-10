# Plan: GitHub Actions build and AI-native SDLC pipeline

Spec: `intent/002-github-actions-pipeline/spec.md` (intent in the same folder).

## Context
Nothing runs automatically on this repo today. The spec defines six workflows plus a `REVIEW.md` policy: Maven build, Snyk scanning, and the playbook's Design, Review and Maintain stages via `anthropics/claude-code-action`. Agents only open PRs; humans merge. The repo has no `.github/`, so this is purely additive. Existing context: Java 25 and Boot 4.1.1 in `pom.xml`, `./mvnw` wrapper, `/spec` skill in `.claude/skills/spec`, YAML-only config.

## Approach
Deliver as one PR per step, in this order, so each PR is built by the pipeline that already exists. Work on a branch per step, never directly on `main`.

### Step 0 — One-time manual setup (user, before steps 3-6)
1. Install the Claude GitHub App on the repo (or `/install-github-app`).
2. `claude setup-token` -> repo secret `CLAUDE_CODE_OAUTH_TOKEN`.
3. Snyk API token -> repo secret `SNYK_TOKEN`.
4. Settings -> Actions: default workflow permissions read-only; allow GitHub and marketplace actions.
5. Optional: enable code scanning and set repo variable `SARIF_UPLOAD=true` for Security-tab upload.

### Step 1 — `build.yml` (F1)
- Triggers: `push: [main]`, `pull_request`. Top-level `permissions: contents: read`.
- Steps: `actions/checkout`, `actions/setup-java` (temurin, `java-version: 25`, `cache: maven`), `./mvnw -B clean verify`, upload `target/surefire-reports` on failure.
- Verify: `actionlint`; PR with a deliberately failing test goes red, revert goes green.

### Step 2 — `snyk.yml` (F5)
- Triggers: push to `main`, PRs, weekly `cron: "0 3 * * 1"`. Skip when the PR is from a fork (secret unavailable).
- `snyk/actions/maven` for `snyk test --severity-threshold=high --sarif-file-output=snyk-oss.sarif`; `snyk/actions/setup` + `snyk code test --sarif-file-output=snyk-code.sarif`. Always upload SARIFs as workflow artifacts; upload to Security tab only when `vars.SARIF_UPLOAD == 'true'`.
- Check Snyk on Java 25 and that Snyk Code is enabled on the account; record any deviation in the spec (C4, C5).
- Verify: known-vulnerable test dependency on a branch fails the job; remove it.

### Step 3 — `REVIEW.md` + `claude-code-review.yml` / `claude.yml` (F3, F4)
- `REVIEW.md` (~40 lines): passes for bugs, spec/plan and `CLAUDE.md` conformance (incl. actuator exposure beyond health); `[critical]/[major]/[minor]` tags; only report issues with a concrete failure scenario; advisory only.
- `claude-code-review.yml` (generated, minimal edits): `pull_request` (opened, synchronize, ready_for_review, reopened), skip drafts and forks; `claude-code-action@v1` with the `code-review@claude-code-plugins` plugin, prompt following `REVIEW.md`, `--comment`, `claude_args` with `--allowedTools "mcp__github_inline_comment__create_inline_comment" --max-turns`, `timeout-minutes`, `concurrency` cancel-in-progress per PR, `continue-on-error: true`. Auth via `claude_code_oauth_token`; no `github_token` input so commits trigger CI. Second job `claude-security-review` runs `/security-review` (full history, `origin/HEAD` set to the base branch) and posts findings inline.
- `claude.yml` (generated, minimal edits): `@claude` mentions, interactive mode, write permissions.
- Verify: PR with an obvious bug gets tagged inline comments with a green check; `@claude` comment produces a commit on the PR branch and `build` runs on it.

### Step 4 — `design-spec.yml` (F2)
- Trigger: `push` to `main` on `paths: ['intent/*/intent.md']`. Compute changed intents from `github.event.before..sha`; skip folders that already have `spec.md`.
- `claude-code-action@v1` with prompt `/spec @<path>` plus instructions: do not ask questions, list assumptions in the spec, branch `spec/<nnn>-<name>`, commit only `spec.md`, open a PR. `actions/checkout` first so `.claude/skills/spec` exists. Per-folder `concurrency`, `--max-turns`, timeout.
- Verify: merge throwaway `intent/999-test/intent.md`; a spec PR appears; re-run creates no duplicate. Delete the throwaway afterwards.

### Step 5 — `maintain-scan.yml` (F6)
- Triggers: `cron: "0 6 * * 1"` and `workflow_dispatch`.
- Download latest Snyk SARIF artifacts from the last `snyk` run on `main` (`gh run download`); Claude validates each finding (confidence, false-positive reasoning), writes `intent/<next nnn>-<kebab>/intent.md` per confirmed finding (findings quoted, secrets redacted) on branch `maintain/security-<date>`, opens one PR; dismissed findings listed in the PR body. No PR when nothing is open.
- Verify: `workflow_dispatch` with and without an open finding.

### Step 6 — Wrap-up
- Add a short "CI" section to `CLAUDE.md` (workflow list, `actionlint`, never push to `main`).
- Pin third-party actions to SHAs with version comments; note the `claude-code-action@v1` floating reference (C6) and consider SHA pin later.
- Update `spec.md` for any deviations found (Java 25 tool support, Snyk plan features).

## Critical files
- New: `.github/workflows/{build,snyk,review,design-spec,maintain-scan}.yml`, `REVIEW.md`
- Edited: `CLAUDE.md` (CI section), `intent/002-github-actions-pipeline/spec.md` (deviations only)
- Reused: `.claude/skills/spec/SKILL.md` (Design job), `./mvnw` and `pom.xml` Java 25 pin (build)

## Verification (end to end)
- `actionlint .github/workflows/*.yml` is clean.
- Spec section 5 checks, F1-F7, each run as described in the steps above.
- `grep -rn "push origin main\|HEAD:main" .github` returns nothing; `git grep -i "sk-ant\|SNYK_TOKEN="` shows only `${{ secrets.* }}` references.
- Confirm every Claude-created change arrived as a PR and the `build` check ran on it.
