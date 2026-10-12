# Spec: Automated dependency updates
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Adopt **Renovate** (Mend Renovate GitHub App) to open weekly, grouped update PRs for Maven dependencies and plugins, the `spring-boot-starter-parent` version, GitHub Actions, the Maven wrapper and the Java pins. Add one new workflow, `dependency-updates.yml`, with two jobs that act only on Renovate PRs:

1. **Override clean-up (OpenRewrite, no Claude).** When a PR raises the Boot parent version, an OpenRewrite recipe removes every Spring Boot version override in `pom.xml`, whatever version the new Boot manages, and records the removals with old and new versions in the PR. `build` and `snyk` then re-run on the cleaned branch.
2. **Build-fix loop (Claude).** When `build` fails on a Renovate PR, Claude tries to fix it and pushes to the PR branch, up to a cap (default 2 attempts). If the cap is reached it comments what failed and what it tried.

Humans merge everything; nothing auto-merges and nothing is pushed to `main`. Out of scope: auto-merge, fixing failures on PRs not opened by Renovate, and any change to the Snyk-to-intent flow (`snyk.yml`, `maintain-scan.yml`).

## 2. Existing codebase context
- `pom.xml`: parent `spring-boot-starter-parent` 4.1.1; properties `java.version` 25, `webjars-bootstrap.version` 5.3.8 and two overrides, `tomcat.version` 11.0.26 and `jackson-bom.version` 3.1.7 (security pins, no comment explaining them).
- Java pins: `pom.xml` (`java.version`), `.sdkmanrc` (`java=25.0.2-open`), `actions/setup-java` `java-version: 25` in `build.yml` and `snyk.yml` (Temurin). Java 25 is a recorded deviation from the latest JDK because SDKMAN cannot install 27 (001 spec).
- Maven wrapper: `.mvn/wrapper/maven-wrapper.properties` (wrapper 3.3.4, `only-script`, Maven 3.10.0).
- Workflows: `build.yml` (`./mvnw -B clean verify`, runs on every PR, job name `build`), `snyk.yml` (Mon 03:00 UTC), `maintain-scan.yml` (Mon 06:00 UTC), `claude-code-review.yml` and `claude.yml` (installer templates, kept minimal), `design-spec.yml`, `evals.yml`. Third-party actions use version tags (`@v7`, `@v1`) per `CLAUDE.md`, with one exception: `snyk/actions/setup` is SHA-pinned (see C6).
- No `.github/dependabot.yml` and no Renovate config exist.
- Claude jobs authenticate with `CLAUDE_CODE_OAUTH_TOKEN` and, with no `github_token` input, push as the Claude GitHub App, so `build.yml` re-runs on those commits (002). `claude-code-action` rejects bot actors unless listed in `allowed_bots`, rejects `push` events (002 deviation, solved with a `workflow_dispatch` re-dispatch) and refuses to run when the workflow file differs from `main`.
- `claude-code-review.yml` already allows `allowed_bots: "claude,github-actions"`; Renovate PRs are same-repo PRs, so review and `/security-review` run on them already, but a Renovate-authored PR is not reviewed unless `renovate` is also allowed (see F9).

## 3. Decisions on the open questions

### 3.1 Renovate or Dependabot: Renovate
| Outcome | Dependabot | Renovate |
|---------|------------|----------|
| Maven deps, plugins, Boot parent | Yes | Yes |
| GitHub Actions | Yes | Yes |
| Maven wrapper | No native support for `maven-wrapper.properties` | Yes (`maven-wrapper` manager) |
| `.sdkmanrc` | No | Yes (`sdkman` manager) |
| Keep Java pins in step | Cannot group across ecosystems or files | Custom regex managers plus a shared `groupName` can bump `pom.xml`, `.sdkmanrc` and both `setup-java` pins in one PR |
| Hold back unwanted versions (non-LTS, unsupported by SDKMAN) | Limited `ignore` rules | `allowedVersions` and `packageRules` per dependency |
| Group Boot with related changes | Groups by ecosystem only | Arbitrary groups |
| Removing overrides on a Boot upgrade | Not possible | Not possible either (no manager knows which properties shadow BOM entries; the hosted app cannot run post-upgrade commands), so a workflow job does it with OpenRewrite (F4, 3.5) |
| PR volume | Fixed schedule per ecosystem | Schedule, grouping and `prConcurrentLimit` |
| Setup | Built in | Install the Mend Renovate GitHub App (free) |

Renovate is the only option that covers the wrapper and Java pins, and its grouping controls keep PR volume low. Cost: one GitHub App install and a `renovate.json5` file. **Hosting:** the hosted Mend Renovate App, not a self-hosted Renovate action, so no extra token or secret is needed. PRs it opens are authored by `renovate[bot]` through an App token, so `build.yml` and the other `pull_request` workflows run on them (PRs opened with `GITHUB_TOKEN` would not trigger them).

### 3.2 Schedule and grouping
- Schedule: `before 5am on monday` (UTC), after the Snyk scan (03:00) so a fresh scan exists and before `maintain-scan` (06:00). Vulnerability-driven PRs are not held to the schedule (see 3.4).
- `prConcurrentLimit: 3`, `prHourlyLimit: 2`; Dependency Dashboard enabled so deferred updates are visible.
- Groups (one PR each):
  - **spring-boot**: the parent version plus any `org.springframework.boot` artifact (including the `spring-boot-maven-plugin` if ever versioned explicitly). Always its own PR, so override clean-up (F4) has a clear trigger.
  - **maven-minor-patch**: every other Maven dependency and plugin, minor and patch only.
  - **github-actions**: all workflow actions, minor and patch only.
  - **java**: `pom.xml`, `.sdkmanrc` and both CI JDK pins (F6).
  - **maven-wrapper**: wrapper and Maven distribution.
  - Major updates are never grouped: one PR per dependency.

### 3.3 Fix attempts per PR: 2
The intent leaves this to the owner. Default **2** attempts, configured in one place (`MAX_FIX_ATTEMPTS` in the workflow `env`), because each attempt spends Pro allowance and most failures are either trivial (an import or property rename) or need a human decision.

### 3.4 Security updates versus `maintain-scan`: Renovate security PRs supplement, maintain-scan is unchanged
`vulnerabilityAlerts` is enabled so Renovate opens a PR when a fix exists for a dependency Dependabot alerts flag (this requires Dependabot alerts to be enabled on the repo; it is a repo setting, not Dependabot PRs). `maintain-scan` stays as it is (out of scope) and still files intents for Snyk findings, including ones Renovate cannot fix. Overlap is accepted: a CVE may produce both an intent and a Renovate PR. Reviewers close the intent once the Renovate PR merges (see C7).

### 3.5 Override clean-up: OpenRewrite, unconditional removal
On a Boot upgrade every override is removed, even when the new Boot still manages an older version than the override. The owner chose this over keeping overrides Boot has not caught up with, accepting a possible downgrade that `snyk` and the reviewer catch (C4). The removal is done by OpenRewrite's built-in `org.openrewrite.maven.RemoveRedundantProperties`, so neither Claude nor a custom script is involved.

Options checked and rejected (tested on copies of `pom.xml` with parents 4.1.1 and 4.2.0-M2):
- Claude reading the effective POM: not deterministic, costs Pro allowance.
- `RemoveRedundantProperties` with `onlyIfValuesMatch: true`: removes an override only when Boot's version is exactly equal, so an override Boot has overtaken stays and pins an older version.
- A `ParentPomInsight` precondition: works, but needs a hand-picked "remove at Boot X" threshold per override that is unknown when the pin is added.
- Overrides as `<dependencyManagement>` entries with `RemoveRedundantDependencyVersions` (`onlyIfManagedVersionIs: GTE`): correct same-or-newer behaviour, but drops the Boot property convention; the owner wants to keep the properties.
- A custom recipe with version comparison: rejected to avoid maintaining recipe code.

`RemoveRedundantProperties` without a `namePattern` removes every property Boot's parent chain also defines, which includes `java.version`. The recipe therefore excludes `java.version` (renaming it out of the way and back) instead of naming each override, so new overrides are covered without editing the recipe (5.6).

## 4. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | **Maven updates.** Renovate opens PRs for dependency, plugin and `spring-boot-starter-parent` version updates in `pom.xml`, grouped as in 3.2. |
| F2 | **Actions updates.** Renovate opens PRs for `uses:` references in `.github/workflows/`. It keeps version tags (e.g. `actions/checkout@v7`) and never rewrites a reference to a commit SHA (`pinDigests: false`, no `helpers:pinGitHubActionDigests`). Existing SHA pins are left as they are (C6). |
| F3 | **Wrapper.** Renovate opens PRs for `.mvn/wrapper/maven-wrapper.properties` (wrapper version and Maven distribution URL). |
| F4 | **Override clean-up.** On a Renovate PR that changes the `spring-boot-starter-parent` version, the workflow runs the `petclinic.RemoveSpringBootOverrides` OpenRewrite recipe (5.6), which removes every property in `pom.xml` that the new Boot parent chain also defines (today `tomcat.version` and `jackson-bom.version`), regardless of version, except `java.version`. It commits the removal to the PR branch and posts a PR comment with a table: property, override version, Boot-managed version. A PR where nothing was removed gets no commit and a one-line comment. `java.version` is excluded by the recipe, and properties Boot does not define (`webjars-bootstrap.version`) are never touched. |
| F5 | **Deterministic clean-up.** No model is involved. The recipe makes the change, and the table's Boot-managed version comes from `./mvnw -q help:evaluate -Dexpression=<property> -DforceStdout` run after the removal. After pushing, the job dispatches `build.yml` and `snyk.yml` on the PR branch, so the cleaned head is built and scanned (a push made with `GITHUB_TOKEN` does not trigger them by itself). |
| F6 | **Java pins in step.** One Renovate group bumps `java.version`, `.sdkmanrc` and the `java-version` in `build.yml` and `snyk.yml` together. Only versions SDKMAN can install are proposed, and only LTS majors (the 001 deviation), with patch updates of the current major allowed. A check in `build.yml` fails when the major in `pom.xml`, `.sdkmanrc` and the CI files differ (see 5.4). |
| F7 | **Build-fix loop.** When `build` completes with `failure` on a PR from a `renovate/*` branch authored by `renovate[bot]`, Claude is run with the failing run's logs and attempts a fix, pushing commits to that branch as the Claude GitHub App so `build` re-runs. The run is skipped when the attempt cap is reached. |
| F8 | **Cap and give-up.** The workflow counts previous attempts from a hidden marker (`<!-- dep-fix-attempt -->`) in PR comments it posts at the start of each attempt. After `MAX_FIX_ATTEMPTS` failed builds it does not start another run; it posts one comment saying what failed (failing step, error excerpt), what each attempt changed, and that a human is needed. If Claude concludes mid-attempt that it cannot fix the failure (e.g. a major API removal), it posts the same comment immediately. |
| F9 | **Existing checks run.** Every Renovate PR runs `build`, `snyk` and both Claude review jobs. `renovate` is added to `allowed_bots` in `claude-code-review.yml` (a one-line change to a template file). |
| F10 | **No auto-merge.** `automerge` is false everywhere; Renovate is not given merge permission and the new workflow never merges. |
| F11 | **No push to `main`.** Renovate, the clean-up job and the fix job write only to `renovate/*` branches. `grep -rn "push origin main\|HEAD:main" .github` stays empty. |
| F12 | **Scope guard.** The fix job acts only on PRs authored by `renovate[bot]` with a `renovate/*` head branch in this repository. All other PRs, including forks, are ignored. |

### Non-functional / constraints
- Cost: the clean-up job uses no Claude allowance. Fix runs: `--max-turns 25` per run, `timeout-minutes: 20`, a `concurrency` group per PR (`dependency-updates-<pr>`, no cancel for in-flight pushes), at most one clean-up run per Boot PR head SHA, and the PR volume limits in 3.2.
- Permissions: `permissions: {}` at workflow level; jobs elevate to `contents: write`, `pull-requests: write`, `actions: read`, `id-token: write` (and `actions: write` only on the dispatching jobs, `cleanup` and `gate`). No secrets besides `CLAUDE_CODE_OAUTH_TOKEN`.
- Tool allowlist for Claude: `Bash(git:*)`, `Bash(./mvnw:*)`, `Bash(gh pr comment:*)`, `Bash(gh run view:*)`, `Read`, `Write`, `Edit`, `Glob`, `Grep`. No `gh pr merge`, no `git push` to non-PR refs by prompt rule.
- Config: Renovate config is `renovate.json5` at the repo root (JSON5 allows comments). `CLAUDE.md`'s "YAML only" rule is about Spring config (`application.yaml`) and does not apply (see C8).
- Action references use tags, never commit SHAs. The workflow passes `actionlint`.
- `claude.yml` is not touched. `claude-code-review.yml` changes only by adding `renovate` to `allowed_bots`. `build.yml` gains a `workflow_dispatch` trigger (`snyk.yml` already has one).
- The rewrite-maven-plugin version is pinned in the workflow `env` (`REWRITE_PLUGIN_VERSION`, 6.46.1 when tested) and kept current by a Renovate regex manager; the plugin is not added to `pom.xml`.
- No change to application code, `application.yaml` or the actuator surface.

## 5. Design

### 5.1 Layout
```
renovate.json5                          # F1, F2, F3, F6, F10 (new)
rewrite.yml                             # F4: override clean-up recipe (new)
.github/workflows/dependency-updates.yml  # F4, F5, F7, F8, F12 (new)
.github/workflows/claude-code-review.yml  # F9: add renovate to allowed_bots (one line)
.github/workflows/build.yml             # F6: Java pin consistency step; F5: workflow_dispatch trigger
CLAUDE.md                               # CI section: document the new workflow, Renovate and the override rule
```

### 5.2 renovate.json5
- `extends: ["config:recommended"]` minus the default digest pinning for actions; `timezone: "UTC"`; `schedule` as in 3.2; `dependencyDashboard: true`; `labels: ["dependencies"]`; `automerge: false`; `pinDigests: false`; `vulnerabilityAlerts: { enabled: true, labels: ["security"] }`.
- `enabledManagers`: `maven`, `maven-wrapper`, `github-actions`, `sdkman`, `custom.regex`.
- `packageRules`:
  - group `spring-boot` (`matchDatasources: maven`, `matchPackageNames: ["org.springframework.boot:*"]`), always its own PR, major updates included and not grouped with others.
  - group `maven-minor-patch` (`matchUpdateTypes: [minor, patch]`), excluding the Boot rule.
  - group `github-actions` (`matchManagers: github-actions`, minor and patch); majors separate. `snyk/actions/setup` is excluded from digest rewriting.
  - group `java` for the four Java pins; `allowedVersions` restricts to LTS majors (currently 25, then 29, etc.) and to versions present in SDKMAN's list.
  - `ignoreDeps` / `matchPackageNames` rule to **skip `tomcat.version` and `jackson-bom.version`** (overrides are owned by the security flow and F4, not by Renovate bumping them in isolation). Renovate only sees properties referenced from a `<version>` of a declared dependency, so these two are not seen today; the rule makes that explicit if an override ever becomes visible. `webjars-bootstrap.version` is referenced by a dependency and is updated normally.
- `customManagers` (regex) for `java-version:` in `build.yml` and `snyk.yml` and `<java.version>` in `pom.xml`, using the `java-version` datasource, so they share a dependency name and group. A further regex manager updates `REWRITE_PLUGIN_VERSION` in `dependency-updates.yml` (`maven` datasource, `org.openrewrite.maven:rewrite-maven-plugin`), grouped with `maven-minor-patch`. `.sdkmanrc` uses the built-in `sdkman` manager with the `-open` flavour kept.

### 5.3 dependency-updates.yml
Triggers and structure follow `design-spec.yml`'s pattern because `claude-code-action` does not support `workflow_run`:

- **`cleanup` job** (no Claude) — `on: pull_request: [opened, reopened, synchronize]`, paths `pom.xml`. Runs only when `github.event.pull_request.user.login == 'renovate[bot]'`, the head repo is this repo, `github.actor == 'renovate[bot]'` and the diff changes the parent `<version>`. Its own push uses `GITHUB_TOKEN`, which does not trigger workflows, so it never re-triggers itself. Renovate rebasing the PR force-pushes as Renovate and removes the clean-up commit, which this trigger handles by re-running the clean-up.
  Steps: checkout the PR head; `actions/setup-java` (Temurin 25); record the override values; `./mvnw -B org.openrewrite.maven:rewrite-maven-plugin:${REWRITE_PLUGIN_VERSION}:run -Drewrite.activeRecipes=petclinic.RemoveSpringBootOverrides`; if `pom.xml` changed, read each removed property's Boot value with `help:evaluate`, commit "Remove Spring Boot version overrides for Boot X" as `github-actions[bot]`, push to the PR branch, run `gh workflow run build.yml --ref <branch>` and `gh workflow run snyk.yml --ref <branch>`, and post the F4 table; otherwise post the one-line comment. Permissions: `contents: write`, `pull-requests: write`, `actions: write`.
- **`gate` job** — `on: workflow_run: workflows: [build], types: [completed]`. Condition: `conclusion == 'failure'`, `event` is `pull_request` or `workflow_dispatch` (the latter is the build the clean-up dispatches), `head_branch` starts with `renovate/`, head repo is this repo. It resolves the PR number, checks the PR author is `renovate[bot]`, counts attempt markers, and either posts the F8 give-up comment or runs `gh workflow run dependency-updates.yml -f pr=<n> -f run_id=<failed run id>` (`actions: write`). Nothing in `gate` runs PR code: `workflow_run` executes the default-branch workflow with secrets and must not check out the PR head.
- **`fix` job** — `on: workflow_dispatch` with inputs `pr` and `run_id`. Re-checks author and branch, posts the marker comment, checks out the PR branch, runs `claude-code-action@v1` with `allowed_bots: github-actions,renovate` and a prompt containing: PR number, failing run id (`gh run view --log-failed`), the rule to make the smallest change that makes `./mvnw -B clean verify` pass, not to weaken or delete tests, not to add suppressions or downgrade the update to hide the failure, to commit and push to the PR branch only, and to post a comment if it cannot fix it. The dispatched run's actor is `github-actions[bot]`, hence the `allowed_bots` entry (same as `design-spec.yml`).
- Both Claude jobs authenticate with `claude_code_oauth_token` and no `github_token`, so commits are authored by the Claude GitHub App and `build` re-runs on the new head (F7). Each new failure re-enters `gate`, which enforces the cap.

### 5.4 Java pin consistency step
A short shell step in `build.yml` (no new action) extracts the major from `java.version` in `pom.xml`, from `.sdkmanrc` and from `build.yml`/`snyk.yml`, and fails with the differing values if any disagree. This guards the case where Renovate grouping is not honoured (a rebase splits the group) and where a human edits one pin. It needs no Claude call.

### 5.5 Repository setup (manual, one-time)
1. Install the Mend Renovate GitHub App on this repository (Renovate opens an onboarding PR; use the `renovate.json5` from this change instead of the generated one).
2. Enable Dependabot alerts (Settings → Code security) so `vulnerabilityAlerts` has data. Do not enable Dependabot version or security updates.
3. Settings → Actions → General: allow workflows to be triggered by `renovate[bot]` PRs (they are not fork PRs, so approval is not required); no new secrets.
4. Optional: make `build` a required check (branch protection is out of scope for this intent; see C2).

### 5.6 rewrite.yml
A declarative recipe at the repo root, where rewrite-maven-plugin finds it by default:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: petclinic.RemoveSpringBootOverrides
displayName: Remove Spring Boot version overrides
description: Removes properties in pom.xml that override a value Spring Boot's parent defines, except java.version.
recipeList:
  - org.openrewrite.maven.RenamePropertyKey:
      oldKey: java.version
      newKey: petclinic.keep.java.version
  - org.openrewrite.maven.RemoveRedundantProperties
  - org.openrewrite.maven.RenamePropertyKey:
      oldKey: petclinic.keep.java.version
      newKey: java.version
```

`RemoveRedundantProperties` has no exclude option, so `java.version` is renamed to a key the parent does not define, the redundant properties are removed, and the key is renamed back; its value and position are unchanged (tested with parents 4.1.1 and 4.2.0-M2). The recipe resolves inherited properties through the whole parent chain (`tomcat.version` comes from `spring-boot-dependencies`), so any new Boot property override is removed on the next Boot upgrade without editing this file. The exclusion list is the only thing to maintain: a property that deliberately sets a Boot-defined value and must survive upgrades needs its own rename pair here (C13).

## 6. Acceptance and verification
| Check | How |
|-------|-----|
| F1 | After install, the Dependency Dashboard lists pending updates; a lowered Maven dependency version on a throwaway branch yields a grouped Renovate PR on the next scheduled (or manually triggered) run. |
| F2 | The first actions PR keeps `@vN` tags; `git diff` shows no 40-character SHA introduced. |
| F3 | Lowering the Maven distribution version yields a wrapper PR. |
| F4 / F5 | Locally: with the parent changed to another Boot release, `./mvnw org.openrewrite.maven:rewrite-maven-plugin:<ver>:run -Drewrite.activeRecipes=petclinic.RemoveSpringBootOverrides` removes `tomcat.version` and `jackson-bom.version` and leaves `java.version` and `webjars-bootstrap.version`. On a Renovate Boot PR: one clean-up commit, a table with old and Boot-managed versions, and `build` and `snyk` runs on the new head. A failing dispatched `build` reaches `gate`. Re-run after a Renovate rebase: the clean-up reappears. |
| F6 | Lower all four Java pins; one PR raises them together. Edit only `.sdkmanrc`: `build` fails on the consistency step. A non-installable, non-LTS version is never proposed. |
| F7 / F8 | Open a Renovate PR (or a branch named `renovate/test` authored by the bot in a sandbox repo) that breaks a test; a fix commit appears from the Claude App and `build` re-runs. Make the failure unfixable: after 2 attempts a single give-up comment appears and no third run starts. |
| F9 | A Renovate PR shows `build`, `snyk`, `claude-review` and `claude-security-review` runs. |
| F10 / F11 | `grep -rn "automerge" renovate.json5` shows `false`; `grep -rn "push origin main\|HEAD:main" .github` returns nothing. |
| F12 | A human PR with a failing build triggers neither `gate` nor `fix`. |
| Lint | `actionlint .github/workflows/*.yml` passes. |
| Config | `npx --yes --package renovate -- renovate-config-validator renovate.json5` passes. |

## 7. Policy conformance
No brand-guideline or UX-standard document applies (no UI; `docs/design-system/` covers pages only). Checked against `CLAUDE.md`, `REVIEW.md` and the earlier specs:
- No push to `main`; every change goes through a branch and PR (F11).
- Actions referenced by version tag, not SHA (F2). Renovate must be configured not to pin digests.
- Workflows pass `actionlint`; `claude.yml` untouched; `claude-code-review.yml` changed by one line.
- Boot managed-version policy: Boot's default actuator surface and YAML-only Spring config are untouched.
- Intents are not mentioned in code comments: workflow and config comments describe what the file does, not intent numbers.

## 8. Areas of concern

- **C1 — Prompt injection from update PRs.** Renovate PR bodies contain release notes and changelogs from third-party repositories, and build logs contain output from dependencies. Claude reads both while holding write access to the PR branch. Mitigations: the fix job acts only on PRs authored by `renovate[bot]` on `renovate/*` branches, uses a narrow tool allowlist (no `gh pr merge`, no network tools), cannot push to `main`, and every change is reviewed by a human and by the existing security review. The prompt tells Claude to treat PR body and logs as data. Residual risk accepted; a malicious upstream release can still try to steer the fix.
- **C2 — "Humans merge" is a convention.** Branch protection is out of scope here and in 002 (C1 there). Nothing technically stops a merge of a red or unreviewed update PR. Recommend a follow-up intent for required checks, since this change raises PR volume.
- **C3 — Pro subscription limits.** Fix attempts and two review jobs per Renovate push all draw on the same allowance. Weekly grouping, `prConcurrentLimit: 3`, the cap of 2 attempts and `--max-turns 25` bound the cost, but a week with several failing groups could still exhaust the allowance and make runs fail or stall. A failed or rate-limited Claude run produces no fix and no comment from Claude; `gate` therefore posts the give-up comment itself once the cap is reached or a fix run errors. Using an API key instead is an owner decision (002 C2).
- **C4 — Unconditional removal can downgrade a pinned library.** If a Boot release still manages an older version than an override (e.g. Boot 4.2.0-M2 manages Jackson 3.1.6, below the 3.1.7 pin), removing the override downgrades it, possibly back to the vulnerable version the pin was added for. This is the owner's choice (3.5). Safety nets: the table shows both versions side by side, and the dispatched `snyk` run fails on high-or-worse findings. A downgrade to a version with only a medium or low finding, or none, passes silently unless the reviewer reads the table. The fix is to re-add the override on the PR branch (it is removed again on the next Boot upgrade).
- **C5 — Clean-up runs on a Renovate-triggered event and Renovate rebases remove it.** Renovate rebases its branches when `main` changes or when someone edits the PR in a way it does not recognise (including the clean-up and fix commits, unless "rebase when behind base branch" is set to `conflicted`). Set `rebaseWhen: "conflicted"` so Renovate does not force-push over the clean-up or fix commits. If it does, the `synchronize` trigger by `renovate[bot]` re-runs the clean-up.
- **C6 — SHA pin versus tag policy.** `CLAUDE.md` says never to pin to commit SHAs, but `snyk.yml` pins `snyk/actions/setup` to a SHA (002 deviation). Renovate would, left alone, update the SHA while leaving the comment `# v1.0.0`; if told to ignore digests it leaves it alone permanently. This spec leaves the reference alone (excluded in `renovate.json5`); the owner should decide whether to move it to `@v1` tag so the policy is consistent. That change is out of scope.
- **C7 — Overlap with `maintain-scan`.** A dependency CVE can generate both a Renovate security PR and a `maintain/security-*` intent PR. Merging the Renovate PR clears the Snyk finding on the next scan, but the intent already filed stays open. Not solved here (the Snyk flow is out of scope); reviewers must close the stale intent.
- **C8 — Config format.** `CLAUDE.md` says "Config is YAML only; do not add `application.properties`". Renovate's native formats are JSON and JSON5, not YAML. I read the rule as covering Spring application config and chose `renovate.json5`. If it is meant repo-wide, the alternative is to reject Renovate in favour of Dependabot (`dependabot.yml` is YAML), which loses the wrapper, `.sdkmanrc` and Java-grouping requirements. The owner should confirm.
- **C9 — Java grouping depends on regex managers and on SDKMAN data.** Renovate's `sdkman` manager and the `java-version` datasource may not agree on the latest patch (Temurin in CI versus the `-open` build in `.sdkmanrc`), so the group may propose a patch version not available for one of them. The consistency check (5.4) compares majors only, and the build uses `setup-java` with a major, so a patch mismatch is harmless. If Renovate cannot be made to keep all four in one PR, the fallback is a Renovate PR for `.sdkmanrc` and `java.version` only and a Claude fix commit for the CI pins; the spec does not mandate that.
- **C10 — First-run Boot upgrade and dependency churn.** Boot 4.1.x moves quickly. A Boot PR may require code changes beyond a trivial fix (e.g. package moves like those noted in `CLAUDE.md` for test annotations). Two attempts will often be insufficient; the give-up comment is the expected outcome for these, and the owner picks up the PR.
- **C11 — Claude workflows skip on PRs that change them.** `claude-code-action` refuses to run unless the workflow file matches `main`, so `dependency-updates.yml` can only be verified after it is merged, and the PR adding it gets no Claude review of itself (same as 002). The `workflow_run` trigger also only fires for workflow files on the default branch.
- **C12 — Actor restrictions.** The action rejects non-human actors unless listed. The `fix` job sees `github-actions[bot]` (dispatch) and PR-triggered review sees `renovate[bot]`. Each is allow-listed only in the job that needs it. If the Claude App cannot be used to push on a `workflow_dispatch` run, commits would be authored by `github-actions[bot]` and would not re-trigger `build`. This must be verified in the first end-to-end test; the fallback is a PR comment `@claude` trigger, which already pushes as the App (`claude.yml`).
- **C13 — Every Boot-defined property is removed except `java.version`.** The recipe treats any property that Boot's parent chain also defines as an override. That covers new security pins automatically, but it also removes a property set on purpose for another reason (e.g. a future `maven.compiler.*` or `project.build.sourceEncoding` setting). Such a property needs a rename pair in `rewrite.yml`, like `java.version`. Reviewers see any such removal in the table.
- **C14 — Dispatched checks.** The `build` and `snyk` runs the clean-up dispatches are attached to the branch head, but they are `workflow_dispatch` runs, not `pull_request` runs. They show on the PR's head commit; if `build` later becomes a required check (C2), confirm that a dispatched run satisfies it, or switch the push to a GitHub App token (a new secret) so `pull_request` runs fire.

## 9. Assumptions
- Renovate (hosted Mend App) is chosen over Dependabot (3.1).
- Schedule is Monday before 05:00 UTC, grouping as in 3.2, `prConcurrentLimit` 3.
- Fix attempts are capped at 2 per PR, configurable via `MAX_FIX_ATTEMPTS`.
- `maintain-scan` and Snyk flows are unchanged; overlap with Renovate security PRs is accepted (3.4). Dependabot alerts are enabled solely as Renovate's vulnerability data source.
- Only LTS Java majors, plus patch updates of 25, are proposed; this extends the 001 deviation.
- The existing overrides `tomcat.version` and `jackson-bom.version` are not touched by Renovate and are cleaned up only by F4 on a Boot upgrade.
- On a Boot upgrade, every property Boot's parent also defines is removed regardless of version, except `java.version` (owner decision, 3.5); the clean-up uses OpenRewrite, not Claude.
- Only the PR author `renovate[bot]` on `renovate/*` branches in this repository counts as "opened by the dependency bot".
- The Java consistency check is added to `build.yml` rather than a new workflow (small addition to a non-template file); `claude-code-review.yml` gets the one-line `allowed_bots` change.
- `renovate.json5` is acceptable under the YAML-only config rule (C8).
- `snyk/actions/setup` stays SHA-pinned and ignored by Renovate (C6).

## 10. Handoff notes for engineering
- Suggested order: (1) `renovate.json5` and the Renovate App install, observe the first PRs; (2) Java consistency step in `build.yml`; (3) `dependency-updates.yml` plus the `allowed_bots` line; (4) `CLAUDE.md` CI section. Each in its own PR.
- Validate the Claude identity question (C12) and that dispatched `build`/`snyk` runs show on the PR (C14) before writing the workflow; both are quick experiments on a throwaway branch.
- After merging `dependency-updates.yml`, run the F4 and F7/F8 checks from section 6 once; they cannot run earlier (C11).
- Estimated effort: about one day, mostly tuning the Renovate regex managers and the fix prompt.
