# Intent: Enable Sonar scans and fix any issues
Author: matthew-js-porter

## Problem
"Enable sonar scans and fix any issues." The pipeline checks dependencies and code security with Snyk, but nothing measures general code quality: bugs, code smells, duplication, security hotspots or test coverage. Quality problems can merge unnoticed, and there is no record of how much of the code the tests exercise.

## Proposed outcome
- Every pull request and every push to `main` is analysed by SonarQube Cloud, and the results (issues, hotspots, coverage, duplication) are visible on the SonarQube Cloud project and linked from the PR.
- A pull request whose Sonar quality gate fails shows a failing check, using the `sonarcloud-actions/sonarcloud-check-status-action` action (https://github.com/sonarcloud-actions/sonarcloud-check-status-action) to report the gate status.
- Test coverage from the existing test suite is reported to Sonar and counts toward the quality gate on new code.
- After the first full scan of `main`, every open issue (bugs, vulnerabilities, code smells) is fixed and every security hotspot is reviewed. Anything judged a false positive or won't-fix is marked as such in Sonar with a stated reason, so the project shows zero open issues and a passing quality gate.
- Sonar findings feed the Maintain loop: the weekly `/triage-findings` run reads the latest Sonar results on `main` alongside Snyk's, validates them against the code, dismisses false positives with a reason, skips problems an existing intent already covers, and opens a PR with an `intent.md` per confirmed root cause, just as it does for Snyk.
- The existing build, tests and Snyk scans keep working unchanged, and `/triage-findings` still handles Snyk findings as before.

Out of scope: replacing Snyk; self-hosted SonarQube; branch protection changes (making the Sonar check required); PetClinic features.

## Affected users and systems
- Developers and agents opening PRs, who will see a Sonar check and PR decoration.
- GitHub Actions workflows in `.github/workflows/` and repository secrets.
- The Maven build (analysis and coverage reporting).
- Application code, templates, CSS/JS and tests, wherever the first scan finds issues.
- A SonarQube Cloud organisation and project for this public repo.
- The `/triage-findings` skill, its evals, and the weekly `triage-findings.yml` workflow.
- `CLAUDE.md` and `REVIEW.md`, which describe the CI pipeline.

## Constraints
- SonarQube Cloud (free for this public repo), authenticated with a `SONAR_TOKEN` repository secret; no credentials in the diff.
- Quality gate status reported via `sonarcloud-actions/sonarcloud-check-status-action`, referenced by version tag, not commit SHA.
- Java 25 (pinned in `pom.xml` and `.sdkmanrc`); analysis must run on the same JDK as the build.
- Favour tooling whose versions Spring Boot manages; any non-Boot plugin or action needs a stated reason in the spec.
- Fixes must not change user-visible behaviour or break the design system rules; every fix keeps `./mvnw clean verify` green.
- No pushes to `main`; everything lands through a branch and PR. Lint workflow changes with `actionlint`.
- `/triage-findings` changes need eval cases covering Sonar input, and the `skill-evals` check must not score lower than `main`. It still does not fix code, commit, push or open PRs itself.
- The weekly Claude run must stay economical on the Pro plan: Sonar findings are added to the existing weekly job, not a new schedule.

## Open questions
- When the same problem shows up in both Snyk and Sonar, should triage file one intent for it? One is assumed. Owner: Matthew / spec.
- Should triage include Sonar code smells and hotspots, or only bugs and vulnerabilities? Assumed to cover all of them, matching the fix scope above. Owner: Matthew.

Decisions recorded: the SonarQube Cloud organisation and project exist, `SONAR_TOKEN` is set, and analysis runs from CI (automatic analysis off); the default "Sonar way" quality gate is used; the issue fixes land in the same PR as the scan setup.
