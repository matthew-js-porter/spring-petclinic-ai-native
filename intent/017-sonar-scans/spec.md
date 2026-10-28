# Spec: Enable Sonar scans and fix any issues
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add SonarQube Cloud analysis to the pipeline, clear the first scan's findings, and feed Sonar results into the weekly Maintain loop. Five parts:

1. **Analysis workflow.** A new `sonar.yml` builds, runs the tests with JaCoCo coverage and submits the analysis for every PR and every push to `main`, on the same JDK as `build.yml`.
2. **Gate status check.** A second job in `sonar.yml` runs `sonarcloud-actions/sonarcloud-check-status-action` so a failed quality gate shows as a failing check on the PR.
3. **Maven wiring.** `pom.xml` gains the Boot-managed JaCoCo plugin (coverage report), the SonarSource Maven scanner plugin (the one non-Boot plugin, reason in 3.2) and the Sonar organisation and project properties.
4. **Issue clean-up.** After the first full scan, every open bug, vulnerability and code smell is fixed and every hotspot is reviewed, in the same PR as the setup, without changing behaviour or breaking the design system rules.
5. **Triage.** `/triage-findings` accepts Sonar issues and hotspots next to Snyk SARIF, files one intent per root cause across both tools, and `triage-findings.yml` fetches Sonar results into the existing weekly run.

Out of scope (from the intent): replacing Snyk, self-hosted SonarQube, making the Sonar check required in branch protection, PetClinic features.

## 2. Existing codebase context
- `pom.xml`: Boot parent 4.1.2, `java.version` 25, only `spring-boot-maven-plugin` declared. No JaCoCo, no Sonar. Tests run in `verify` through Boot-managed surefire.
- `build.yml`: `./mvnw -B clean verify` on `actions/setup-java@v6`, Temurin 25, `cache: maven`; `permissions: contents: read`; concurrency cancels superseded PR runs. This workflow stays unchanged.
- `snyk.yml` is the model for a scanning workflow: it skips fork PRs because the token is withheld, uses the same JDK as the build, and always uploads results as an artifact.
- `triage-findings.yml` runs Mondays 06:00 UTC: a shell step downloads the latest non-PR `snyk-sarif` artifact from `main`, a prep step computes the next intent number and branch, then one `claude-code-action` call runs `/triage-findings` (`--max-turns 40`) and opens one PR. The Claude step only runs when `count != '0'`. Its allowed tools exclude `curl`; network access happens in shell steps.
- `.claude/skills/triage-findings/SKILL.md` reads SARIF only (`snyk-oss.sarif`, `snyk-code.sarif`); its evals (`evals/evals.json`, fixtures in `evals/files/`) are Snyk-only.
- `REVIEW.md` names `build.yml` and `snyk.yml` as the workflows carrying the Java major; `CLAUDE.md` has a CI section listing workflows.
- Code under analysis: Java in `src/main/java` and `src/test/java`, Thymeleaf templates, `petclinic.css`, `navbar.js`, `application.yaml`, SQL, and the workflow YAML.
- Intent 016 (Java 27) has a spec but is not yet implemented on `main`; this spec therefore says "the build's JDK", not a number (A7).

## 3. Design

### 3.1 Workflow `sonar.yml`
- Triggers: `push` to `main`, `pull_request`, `workflow_dispatch`. No schedule: the push to `main` keeps the dashboard current and the weekly triage reads that.
- `permissions: contents: read`. The plan adds `pull-requests: read` to the analysis job only if SonarQube Cloud's documentation requires it. PR decoration is done by the SonarQube Cloud GitHub App, not by a workflow token.
- Concurrency `sonar-${{ github.ref }}`, cancelling in progress for PRs only, as in `build.yml`.
- **Job `analyze`**
  - `if` skips fork PRs (`github.event.pull_request.head.repo.full_name == github.repository`), like `snyk.yml`, because `SONAR_TOKEN` is withheld from them (C4).
  - `actions/checkout@v7` with `fetch-depth: 0` (Sonar needs full history for blame and new-code detection) and `persist-credentials: false`.
  - `actions/setup-java@v6` with the same `distribution` and `java-version` as `build.yml`, `cache: maven`.
  - Step: `./mvnw -B clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` with `SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}` in that step's `env` only. The token is not echoed, not on the command line and not in the diff. Running `verify` and `sonar` in one invocation lets the scanner pick up the surefire and JaCoCo reports with no upload/download plumbing.
  - `build.yml` is untouched, so the test suite runs a second time in CI; accepted for isolation (A3, C8).
  - On failure, upload `target/surefire-reports` as `build.yml` does.
- **Job `quality-gate`** (`needs: analyze`, same fork-PR `if`)
  - Runs `sonarcloud-actions/sonarcloud-check-status-action@<version tag>` and fails when the gate is not `OK`. The action's inputs, and whether it needs the token, the project key or the scanner's `report-task.txt`, cannot be verified from this repository; the plan reads its README at the chosen tag and uses only documented inputs (A10).
  - If the action needs `target/sonar/report-task.txt`, `analyze` uploads it as an artifact and this job downloads it; otherwise the jobs stay independent.
  - Fallback the plan records if the action cannot be used: `-Dsonar.qualitygate.wait=true` on the scanner command, which fails `analyze` directly. That is the SonarSource-supported route and drops a third-party action, but the intent names the action, so the action is the default (C1).
- Actions are referenced by version tag, never a commit SHA (`CLAUDE.md`). `actionlint` must pass.

### 3.2 Maven changes
- **JaCoCo.** `org.jacoco:jacoco-maven-plugin` is in Spring Boot's plugin management, so no `<version>` is written. Executions: `prepare-agent` (default phase) and `report` bound to `verify`, producing `target/site/jacoco/jacoco.xml`. Scanner property `sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`. The JaCoCo version Boot manages must read class files of the build's JDK; if it cannot, the build is red and the plan reports it rather than excluding code (C3).
- **Sonar scanner plugin.** `org.sonarsource.scanner.maven:sonar-maven-plugin` is not managed by Spring Boot. **Stated reason (required by `CLAUDE.md` and `REVIEW.md`):** it is SonarSource's official Maven scanner, there is no Boot-managed equivalent, and the alternatives (a CLI scanner action with a separate download, or other build tools) do not read Maven's module, classpath and report layout as directly. Its version is a property in `pom.xml` `<properties>` (`sonar-maven-plugin.version`) so Renovate's Maven manager proposes updates in the `maven-minor-patch` group. The review's "Boot-managed dependencies" rule must find this reason here and post nothing.
- **Sonar properties in `pom.xml` `<properties>`:** `sonar.host.url=https://sonarcloud.io`, `sonar.organization`, `sonar.projectKey`. These are identifiers, not secrets. Using `pom.xml` rather than `sonar-project.properties` adds no new file; the YAML-only rule in `CLAUDE.md` concerns Spring config (A2).
- No exclusions are added up front. `sonar.coverage.exclusions` / `sonar.exclusions` are allowed only for code that cannot meaningfully be tested (e.g. the `main` method in `PetClinicApplication`), each with a one-line reason in the PR. Excluding code to pass the gate is not allowed.

### 3.3 First scan and issue clean-up
Process, all in the one PR:
1. Open the PR with the setup; confirm the analysis runs and PR decoration appears.
2. Read the findings from SonarQube Cloud (UI, or `api/issues/search` and `api/hotspots/search`; the project is public so reads need no token).
3. Fix each bug, vulnerability and code smell in code, tests, templates, CSS/JS or workflow YAML, with the smallest change that removes the violation.
4. Review each security hotspot: fix it, or mark it Safe in Sonar with a comment saying why.
5. Mark each false positive or won't-fix in Sonar with a stated reason. The exception is a finding that conflicts with a `CLAUDE.md` rule (e.g. version tags, never commit SHAs): it is suppressed in the file with `NOSONAR` on the flagged line, with the rule key and the reason in an adjacent comment, so the decision is reviewed with the code. The PR lists each use.
6. Repeat until the project shows 0 open issues, all hotspots reviewed and a passing gate.

Constraints on fixes:
- User-visible behaviour is unchanged; existing tests keep passing and `./mvnw clean verify` stays green after every commit.
- Design system rules hold: a template or CSS fix adds no shadows, rounded corners, new colours or hard-coded values. If a Sonar rule conflicts with the design system, the design system wins and the finding is marked won't-fix with that reason (C2).
- Missing coverage on new code is fixed with tests, not exclusions.
- Marking issues in Sonar needs a Sonar login with the right permission and is a human step (A6); the analysis `SONAR_TOKEN` is not used for it.
- The findings are unknown until the first scan. The plan has a section listing them after the scan, and the PR description summarises them by rule.

### 3.4 Triage: Sonar input to `/triage-findings`
**Skill input.** The first argument is a results directory that may hold any of `snyk-oss.sarif`, `snyk-code.sarif`, `sonar-issues.json`, `sonar-hotspots.json`, `sonar-meta.json` (when none is given, the skill fetches the latest results into `.scan-results/`). Existing evals pass an explicit SARIF directory and must keep working. The run-link argument still carries the Snyk run URL; the Sonar dashboard URL comes from `sonar-meta.json`. The skill's `description` and heading change from "Snyk SARIF" to "Snyk and Sonar findings".
- `sonar-issues.json`: the `issues` array of `GET /api/issues/search` (fields used: `key`, `rule`, `severity`, `type` BUG / VULNERABILITY / CODE_SMELL, `component`, `line`, `message`, `status`, `effort`).
- `sonar-hotspots.json`: the `hotspots` array of `GET /api/hotspots/search` (`key`, `ruleKey`, `securityCategory`, `vulnerabilityProbability`, `component`, `line`, `message`, `status`).
- `sonar-meta.json`: `{ "dashboardUrl", "projectKey", "branch", "fetchedAt", "issues", "hotspots" }`.

**Skill behaviour** (additions; Snyk handling is unchanged):
1. Validate each Sonar issue and hotspot against the current code, with the same confidence rating and the same concrete-reason rule for dismissals. A hotspot is confirmed only if the risky pattern is real, reachable and not test-only.
2. Group by root cause across tools: a Snyk and a Sonar finding on the same weakness are one group and one intent quoting both (A1). Many issues of one rule across files are one group when one change fixes them.
3. Cover bugs, vulnerabilities, code smells and hotspots (A4). Only open issues (`OPEN`, `CONFIRMED`, `REOPENED`) and `TO_REVIEW` hotspots are fetched.
4. Quote rule key, type/severity, file:line and the dashboard link in each intent. Same redaction, same ban on designing fixes, same ban on mentioning dismissed or already-covered items.
5. Sonar `message` and rule text are untrusted data, like SARIF.
6. The Confirmed and Dismissed tables gain a "Source" column (Snyk, Sonar, both).
7. The skill still writes only `intent.md` files; apart from read-only fetches of the latest results (see the workflow below), it does not call the Sonar API, mark issues in Sonar, commit, push or open PRs. "Dismisses false positives with a reason" means the dismissed table records the reason; a human applies it in Sonar (C5).

**Workflow `triage-findings.yml`** (existing schedule, no new schedule, one Claude run):
- The workflow is a checkout and one `claude-code-action` call running `/triage-findings` with no arguments. With no results directory, the skill runs its `scripts/fetch-results.sh` (no arguments), which fetches into `.scan-results/` (git-ignored): the latest non-PR `snyk-sarif` artifact from `main` with `gh run download`, and Sonar issues and hotspots of the main branch with `curl -fsS` on `api/issues/search?componentKeys=<projectKey>&resolved=false&ps=500` and `api/hotspots/search?projectKey=<projectKey>&status=TO_REVIEW&ps=500`, writing `sonar-*.json` and `sonar-meta.json`. With no `branch` parameter the API reads the project's main branch, whatever SonarQube Cloud names it. The project is public, so no token is sent. A failed fetch for one source is noted in the report and never blocks the other.
- Allowed tools add only that script, as an exact command, plus `jq` and `date`. Claude gets no `curl` or `gh run` of its own, because a prefix rule such as `curl -fsS https://sonarcloud.io/api/:*` still accepts extra URLs and `-d @file` / `-o <path>` options. The step passes the job's token as `SNYK_ARTIFACT_TOKEN`, which the script uses for `gh`, because the token claude-code-action provides may lack `actions: read`.
- Prompt text, commit message, branch name (`maintain/findings-<date>`) and PR title ("Maintain: findings …") drop "security"/"Snyk" wording, since confirmed intents may be code quality only.
- The fetch is capped at 500 per type; the PR notes when the cap truncated the list. `--max-turns` rises from 40 to 50 to cover the fetch; if volume makes that tight, the skill triages by severity first rather than the budget being raised (A8).
- `actionlint` clean.

### 3.5 Evals
Add cases to `.claude/skills/triage-findings/evals/evals.json` with fixtures under `evals/files/` (never the top-level `intent/`; any Java or POM fixture uses the `.fixture` suffix):
1. **sonar-only**: a real bug and a real code smell become intents; a smell in test code and one on a file that no longer exists are dismissed with reasons.
2. **cross-tool-duplicate**: the same weakness in Snyk SARIF and Sonar yields exactly one intent quoting both rule IDs; the table shows source "both".
3. **covered-by-existing-intent**: a Sonar issue matching an existing intent is skipped and listed as a duplicate.
4. **hotspot**: one reachable hotspot is confirmed, one test-only hotspot dismissed.
5. **planted-instruction**: an instruction in a Sonar `message` is ignored and nothing is written outside the intents.

The existing Snyk cases stay unchanged so `skill-evals` compares like for like. New cases have nothing on `main` to compare against, so they must score 100% on the first PR.

### 3.6 Documentation
- `CLAUDE.md`: add `sonar.yml` to the CI list; update the `triage-findings.yml` line and the `/triage-findings` mention to Snyk and Sonar; note that Sonar issues are fixed, or marked in Sonar with a reason.
- `REVIEW.md`: add `sonar.yml` to the workflows named in the Java pins rule (with `build.yml` and `snyk.yml`).
- `README.md`: only if it lists workflows; a quality gate badge is optional (A9).

## 4. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | Every PR from this repository and every push to `main` runs a Sonar analysis in `sonar.yml`; results appear on SonarQube Cloud and as PR decoration. |
| F2 | A failed quality gate makes the `quality-gate` job, and so the PR check, fail; a passing gate makes it pass. |
| F3 | JaCoCo coverage from the existing tests is read by Sonar and counts in the gate's coverage-on-new-code condition. |
| F4 | The analysis uses the same JDK distribution and major as `build.yml`. |
| F5 | The default "Sonar way" gate is used unchanged. |
| F6 | After clean-up the project shows 0 open bugs, vulnerabilities and code smells, all hotspots reviewed, gate passing. Each false positive or won't-fix is marked in Sonar with a reason. |
| F7 | `/triage-findings` handles Sonar issues and hotspots, filing one intent per root cause across Snyk and Sonar and skipping problems existing intents cover. |
| F8 | `triage-findings.yml` fetches Sonar results in the existing weekly job: no new schedule, at most one Claude call per run. |
| F9 | The existing build, tests, Snyk scans and Snyk-only triage behave as before; existing evals pass unchanged. |
| F10 | New eval cases cover Sonar input (3.5) and score 100%. |
| F11 | `CLAUDE.md` and `REVIEW.md` describe the new pipeline pieces. |

### Non-functional / constraints
- `SONAR_TOKEN` only as `secrets.SONAR_TOKEN` in a step `env`; no credentials in the diff; token never printed.
- Third-party actions by version tag; `actionlint` clean; no push to `main`.
- Non-Boot plugin only with the reason in 3.2.
- `./mvnw clean verify` green at every commit; no user-visible behaviour change; design system rules hold.
- No `application.properties`; no change to the actuator surface.
- Triage still does not fix code, commit, push or open PRs.

## 5. Acceptance criteria
1. `sonar.yml` runs on the implementation PR, `analyze` and `quality-gate` pass, and SonarQube Cloud shows the PR analysis with coverage above 0.
2. A temporary failing condition (for example a throwaway PR) shows that a failing gate produces a failing `quality-gate` check; recorded in the PR, then removed.
3. After merge, the first push-to-`main` analysis shows 0 open issues, 0 unreviewed hotspots and a passing gate.
4. `build`, `snyk` and `skill-evals` are green on the PR.
5. A manual `workflow_dispatch` of `triage-findings.yml` fetches both sources and either opens one PR or prints the dismissed table with no PR.
6. The diff contains no token value and no commit-SHA pin for the new action.

## 6. Areas of concern
- **C1: third-party action versus the supported mechanism.** `sonarcloud-actions/...` is not under the SonarSource organisation, so it is a supply-chain dependency in a job that may hold `SONAR_TOKEN`, and `CLAUDE.md` forbids SHA pinning, the usual mitigation. The spec follows both rules as written (version tag) and gives the action the token only if its documented inputs require it. `-Dsonar.qualitygate.wait=true` is the safer fallback.
- **C2: Sonar rules versus the design system.** A rule may ask for something the design system forbids or that changes appearance. The design system wins; the finding is marked won't-fix with the reason. Not predictable until the first scan.
- **C3: JaCoCo and Sonar on the newest JDK.** Tooling often lags a new Java major. With 016 pending, a JDK bump could break coverage or analysis. The fallback is never to exclude code; it is to hold the tool or JDK change until supported.
- **C4: PRs without the secret.** Fork PRs get no analysis and no gate (skipped like Snyk). Any other bot PR denied secrets would also be skipped; a skipped check is neutral, not failed, so it cannot be mistaken for a pass of the gate.
- **C5: "dismiss in Sonar" cannot be automated by the skill.** The intent says triage dismisses false positives, but the skill must not mutate anything. Dismissals are recorded in the PR table and applied in Sonar by a human.
- **C6: "first full scan" versus "fixes in the same PR".** A PR analysis reports only new-code issues against `main`. If `main` has never been analysed, the PR may show everything or nothing as new; the plan checks this first. If full-project issues cannot be seen before merge, remaining fixes follow in a second PR after the first `main` scan and the deviation is recorded here. The free plan analyses `main` and PRs only, not other branches.
- **C7: Pro-plan economy.** The first batch of Sonar findings may be large. One Claude run, the 500 cap and the turn cap bound the cost; the first weekly run after launch may truncate (A8).
- **C8: duplicate test runs.** CI runs the suite twice per push (`build` and `sonar`). Accepted for isolation; revisit if CI time matters.

## 7. Assumptions
- **A1** One intent per problem when Snyk and Sonar report the same one (the intent's first open question).
- **A2** Scanner settings live in `pom.xml` properties; `sonar.organization` and `sonar.projectKey` are read by the plan from the existing SonarQube Cloud project. They are not secrets.
- **A3** A separate `sonar.yml` that runs `verify` itself, leaving `build.yml` unchanged, rather than a Sonar step inside `build`.
- **A4** Triage covers bugs, vulnerabilities, code smells and hotspots (the intent's second open question, as it assumes).
- **A5** No Sonar cache step unless scan time warrants it.
- **A6** Marking issues false-positive, won't-fix or safe is done by a human in the Sonar UI.
- **A7** The Java setup in `sonar.yml` mirrors `build.yml` whichever major is current; if 016 lands first, `sonar.yml` follows it.
- **A8** The Sonar fetch is capped at 500 issues and 500 hotspots per weekly run; the PR notes when the cap is reached.
- **A9** No README badge is required.
- **A10** The check-status action gets only the inputs its README documents at the chosen tag, and the latest tag at implementation time is used.
- **A11** Test code is analysed too and its issues are fixed like any other; coverage excludes test sources as Sonar does by default.

## 8. Out of scope
Replacing Snyk, self-hosted SonarQube, required-check branch protection, PetClinic features, custom quality profiles or gates, and Java language upgrades.

## 9. Deviations found during implementation
- **Java 27 is current.** Intent 016 landed before this work, so `sonar.yml` uses Temurin 27, as `build.yml` does (A7).
- **JaCoCo is not Boot-managed.** Spring Boot 4.1.2's parent no longer manages `org.jacoco:jacoco-maven-plugin` (3.2 assumed it did). It is declared with `jacoco-maven-plugin.version` 0.8.16 in `pom.xml` `<properties>`, so Renovate's `maven-minor-patch` group updates it. **Stated reason:** JaCoCo is the coverage format SonarQube Cloud's Java analyser reads, Boot manages no coverage tool, and 0.8.16 is the first release with official Java 27 support (C3).
- **Analysis scope.** `sonar.sources=.`, `sonar.tests=src/test` and `sonar.exclusions=docs/**,intent/**,.claude/**,.claude-pr/**,target/**,src/test/**` are set in `pom.xml`. The whole repository is analysed except the `docs/design-system/**` previews, the reference HTML under `intent/`, the skill eval fixtures and agent config, none of which is the application; `src/test` is excluded from sources only because `sonar.tests` analyses it. New top-level directories are analysed by default. It excludes no application code (3.2).
- **Check-status action.** `sonarcloud-actions/sonarcloud-check-status-action` has one tag, `v1`. Its documented inputs (`url`, `org`, `repo`, `branch`, `wait-timeout`, `poll-interval`, `commit`) default to this repository's `<org>_<repo>` key and wait for the analysis of the head commit, and `SONAR_TOKEN` is only needed for private projects. So `quality-gate` passes no inputs and no token, and needs no `report-task.txt` artifact (3.1, A10, C1).
- **Fetching moved into the skill.** The weekly workflow first fetched with shell steps and skipped Claude when the count was 0. To keep the workflow small and let a local `/triage-findings` with no arguments do the same job, the skill now fetches itself (3.4). The cost: the Claude step runs every week, even when there are no findings, and the session that reads untrusted finding text can also run the fetch script, which takes no arguments and only reads from GitHub and SonarQube Cloud. The fetch has no eval case, because evals run offline with fixture directories.
- **Weekly fetch.** The cap equals the page size, so the fetch makes one request per type (`ps=500`) and compares `paging.total` with what it got to detect truncation, rather than paging. Snyk results also land in `.scan-results/`, and the run-link argument is `none` when there is no Snyk run.
- **Eval fixtures.** The Sonar cases use their own tree, `evals/files/tree-sonar/`, with an existing intent `001-visit-booking-logging` for the covered-by-existing-intent case, so the Snyk cases' `tree/` is unchanged.
- **Tag ref for the check-status action.** The first PR analysis reported `githubactions:S7637` ("Use full commit SHA hash for this dependency") on `sonarcloud-actions/sonarcloud-check-status-action@v1` in `sonar.yml`. `CLAUDE.md` requires version tags, never commit SHAs, so the `uses:` line carries `# NOSONAR`, with the comment above it naming the rule and the reason (3.3 step 5, C1). An inline suppression keeps the decision next to the line and covers only that line, unlike a `sonar.issue.ignore.multicriteria` entry in `pom.xml`.
