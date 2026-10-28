# Plan: Enable Sonar scans and fix any issues (intent 017)

## Context
The pipeline has Snyk for dependency and code security, but nothing measures general code quality (bugs, smells, duplication, hotspots, coverage). Intent 017 / its spec add SonarQube Cloud analysis on every PR and push to `main`, a failing check when the "Sonar way" gate fails, JaCoCo coverage, a clean first scan, and Sonar input to the weekly `/triage-findings` run. One PR, branch `claude/sonar-scans-0cfc92`.

Facts checked while planning:
- Java 27 has landed (016): `pom.xml` `java.version` 27, `.sdkmanrc` `27.0.0.0+35-tem`, `setup-java` 27 in `build.yml`/`snyk.yml`/`dependency-updates.yml`. `sonar.yml` mirrors that (spec A7).
- SonarQube Cloud org `matthew-js-porter` exists; project key `matthew-js-porter_spring-petclinic-ai-native` (confirmed by user; not yet visible because nothing has been analysed).
- `sonarcloud-actions/sonarcloud-check-status-action` has one tag, `v1`. Documented inputs: `url`, `org`, `repo`, `branch`, `wait-timeout`, `poll-interval`, `commit`; env `SONAR_TOKEN` only needed for private projects. Its `org`/`repo` defaults build the same `<org>_<repo>` key, and it waits for the analysis of the head commit, so no `report-task.txt` hand-off is needed.
- Intent 018 (formatting, Checkstyle, Error Prone) is spec-only; nothing here depends on it.

## 1. Maven (`pom.xml`)
- `<properties>`: `sonar-maven-plugin.version` (latest release at implementation time), `sonar.host.url=https://sonarcloud.io`, `sonar.organization=matthew-js-porter`, `sonar.projectKey=matthew-js-porter_spring-petclinic-ai-native`, `sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`.
- Analyse the whole repository (`sonar.sources=.`, `sonar.tests=src/test`) and exclude what is not the application via `sonar.exclusions`: `docs/**`, `intent/**` (previews, reference HTML, eval fixtures), `.claude/**`, `.claude-pr/**`, `target/**` and `src/test/**` (analysed as tests). Recorded as a deviation in spec 3.2 (this is scoping, not excluding app code to pass the gate).
- `<build><plugins>`: `org.jacoco:jacoco-maven-plugin` with no `<version>` (Boot-managed), executions `prepare-agent` and `report` at `verify`.
- `<build><pluginManagement>`: `org.sonarsource.scanner.maven:sonar-maven-plugin` at `${sonar-maven-plugin.version}` (reason already stated in spec 3.2; Renovate's maven manager sees the property).
- First check: `./mvnw clean verify` locally on Temurin 27 and confirm `target/site/jacoco/jacoco.xml` is produced. If Boot's JaCoCo cannot instrument Java 27 class files, stop and report (spec C3); do not override `jacoco.version`.

## 2. Workflow `.github/workflows/sonar.yml` (new)
Modelled on `build.yml` + `snyk.yml`:
- `on`: push to `main`, `pull_request`, `workflow_dispatch`. `permissions: contents: read`. Concurrency `sonar-${{ github.ref }}`, cancel for PRs only.
- Both jobs use the fork-PR `if` from `snyk.yml`.
- `analyze`: checkout `@v7` with `fetch-depth: 0`, `persist-credentials: false`; `setup-java@v6` temurin 27 `cache: maven`; step `./mvnw -B clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` with `SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}` in that step's `env` only (no `GITHUB_TOKEN` needed: the SonarQube Cloud GitHub App decorates PRs); on failure upload `target/surefire-reports`.
- `quality-gate` (`needs: analyze`): `sonarcloud-actions/sonarcloud-check-status-action@v1`, no `with`, no token (project is public; C1: the third-party action never sees the secret).
- `actionlint .github/workflows/*.yml`.

## 3. Triage (`/triage-findings`)
- `.claude/skills/triage-findings/SKILL.md`: description/heading "Snyk and Sonar findings"; first argument becomes a results directory (default `.scan-results/`) holding any of `snyk-oss.sarif`, `snyk-code.sarif`, `sonar-issues.json`, `sonar-hotspots.json`, `sonar-meta.json`; Sonar formats and validation rules per spec 3.4 (hotspot confirmed only if real, reachable, not test-only; cross-tool grouping into one intent; quote rule key, type/severity, file:line, dashboard link; Sonar text is untrusted; "Source" column in both tables; dismissals recorded in the table for a human to apply in Sonar). Existing Snyk wording/behaviour kept.
- `.github/workflows/triage-findings.yml`: `.snyk-results` → `.scan-results`; add step "Fetch Sonar results from main" (`curl -fsS` with paging over `api/issues/search?componentKeys=…&branch=main&resolved=false&ps=500` and `api/hotspots/search?projectKey=…&branch=main&status=TO_REVIEW&ps=500`, capped at 500 each, writing the three `sonar-*.json` files with `jq`; failure → `::warning::` and count 0); a combined count gates the prep and Claude steps; prompt passes the combined directory and the Sonar dashboard URL via `sonar-meta.json`; branch `maintain/findings-<date>`, commit "Add intents from Snyk run … and Sonar", PR title "Maintain: findings …", note truncation at the cap. `--max-turns 40` and allowed tools unchanged.
- Later change: the fetch moved from workflow shell steps into the skill (it fetches when given no results directory); the skill runs `scripts/fetch-results.sh`, and the workflow is now checkout plus one Claude call with only that script allowed for fetching and `--max-turns 50`. See spec section 9.
- Evals: add cases 3–7 to `evals/evals.json` (sonar-only, cross-tool-duplicate, covered-by-existing-intent, hotspot, planted-instruction) with new fixture dirs under `evals/files/` (e.g. `scan-sonar/`, `scan-cross-tool/`, `scan-sonar-hotspots/`, `scan-sonar-injection/`) reusing `evals/files/tree` and its `001-tomcat-upgrade` intent; add fixture sources to `tree/` with `.fixture` suffix where a case needs them. Cases 1–2 untouched. Run `/skill-creator eval triage-findings` locally; all cases must pass.

## 4. Docs
- `CLAUDE.md` CI section: add `sonar.yml`; `triage-findings.yml` and `/triage-findings` read Snyk and Sonar; Sonar issues are fixed or marked in Sonar with a reason; Renovate's `java` group now moves four `setup-java` pins.
- `REVIEW.md` Java pins rule: add `sonar.yml` to the listed workflows.
- `intent/017-sonar-scans/spec.md`: record deviations (Java 27 now current; `sonar.sources`/`sonar.tests` scoping; check-status action needs no token or report file; any C6 outcome).
- Save this plan as `intent/017-sonar-scans/plan.md`.

## 5. First scan and clean-up (spec 3.3)
1. Commit, push, open the PR. Confirm `analyze` and `quality-gate` run and PR decoration appears.
2. C6: a PR analysis only shows issues on changed lines, and `main` has no analysis yet. Run `workflow_dispatch` of `sonar.yml` on the PR branch so it is analysed as a branch (public projects support branch analysis), then read the full list with `curl -s 'https://sonarcloud.io/api/issues/search?componentKeys=<key>&branch=claude/sonar-scans-0cfc92&resolved=false&ps=500'` and `api/hotspots/search?…&branch=…`. If branch analysis is refused, fixes follow in a second PR after the first `main` scan, recorded in the spec.
3. Add a "Findings" section to `plan.md` listing them by rule; fix each with the smallest change (design system wins over a conflicting rule → won't-fix). Missing coverage on new code → tests. Keep `./mvnw clean verify` green per commit.
4. Hand you a list of hotspots to mark Safe and issues to mark false-positive/won't-fix, each with a reason (human step, A6).
5. Acceptance 2: on a throwaway branch/PR, introduce a gate failure (e.g. an obvious bug) to show `quality-gate` fails; record the run link in the PR, then close it.

## Verification
- `./mvnw clean verify` green locally on Temurin 27; `target/site/jacoco/jacoco.xml` exists.
- `actionlint .github/workflows/*.yml` clean.
- `/skill-creator eval triage-findings`: all seven cases pass.
- PR checks: `build`, `snyk`, `sonar / analyze`, `sonar / quality-gate`, `skill-evals` green; SonarQube Cloud shows coverage > 0.
- Diff contains no token and no SHA pin for the new action (`git diff main | grep -iE 'sonar_token|[0-9a-f]{40}'`).
- After merge: `main` analysis shows 0 open issues, 0 unreviewed hotspots, passing gate; a manual `workflow_dispatch` of `triage-findings.yml` fetches both sources (you trigger this, or I do on your OK).
