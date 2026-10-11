# Intent: Automated dependency updates
Author: matthew-js-porter

## Problem
"I want to setup a github actions workflow for either renovatebot or dependabot to keep my dependencies up to date. I want it to remove any version overrides whenever spring boot upgrades. When PR builds fail, I want Claude to try to fix it."

Today nobody is reminded when a dependency falls behind, so updates happen only when someone checks by hand or a Snyk finding forces one. Security fixes also leave version overrides in `pom.xml` (currently `tomcat.version` and `jackson-bom.version`) that pin Boot-managed libraries. Nothing removes them once Spring Boot ships the same or a newer version, so they go stale, and in time they can hold a library back below what Boot would give us. When an upgrade breaks the build, someone has to work out the fix by hand.

## Proposed outcome
- **Update PRs:** a dependency bot (Renovate or Dependabot, chosen in the spec) opens pull requests on a regular schedule for:
  - Maven dependencies, plugins and the `spring-boot-starter-parent` version;
  - GitHub Actions versions in `.github/workflows/`;
  - the Maven wrapper and the Java version (`.sdkmanrc`, `pom.xml` and the CI JDK move together).
- **Override clean-up on Boot upgrades:** when a PR upgrades Spring Boot, the same PR removes each version override whose library the new Boot version manages at the same or a newer version. An override is kept when Boot is still behind it, and the PR says which overrides it removed and which it kept, and why.
- **Claude fixes failed bot PRs:** when the build fails on a dependency-bot PR, Claude tries to fix it and pushes the fix to that PR's branch, so the build re-runs. Retries are capped. If Claude can't fix it, it leaves a comment on the PR explaining what failed and what it tried.
- **Humans merge:** every update PR passes through the existing build, Snyk and Claude review checks, and nothing auto-merges.

Out of scope: auto-merging, fixing failed builds on PRs that weren't opened by the dependency bot, and changing the existing Snyk-to-intent flow.

## Affected users and systems
- The repo owner, who reviews and merges update PRs.
- `pom.xml` (parent, properties, overrides), `.mvn/wrapper`, `.sdkmanrc`, `.github/workflows/`.
- A new dependency-bot configuration, and possibly a GitHub App install (Renovate).
- New or extended Claude workflow(s) using `anthropics/claude-code-action`, alongside `build.yml`, `claude.yml` and `claude-code-review.yml`.

## Constraints
- Never push to `main`: updates and Claude fixes go to PR branches only.
- Actions stay pinned to version tags, never commit SHAs, and the bot must respect that.
- Java pins (`pom.xml`, `.sdkmanrc`, CI) stay in step, and a JDK version that SDKMAN can't install must not be proposed (see the Java 25 deviation in the 001 spec).
- Claude runs on the Pro subscription (`CLAUDE_CODE_OAUTH_TOKEN`), so fix attempts must be economical: capped turns, job timeouts, and a limit on attempts per PR. Grouping updates can help keep PR volume down.
- Claude's fix commits must authenticate as the Claude GitHub App so that `build.yml` re-runs on them (same as intent 002).
- Workflows pass `actionlint`, and `claude.yml` and `claude-code-review.yml` stay close to their upstream templates.

## Open questions
- Renovate or Dependabot? Decide in the spec against these outcomes: removing overrides on a Boot upgrade, keeping the Java pins in step, and the PR volume each produces. Owner: Matthew / spec.
- What schedule and grouping (e.g. weekly, with Boot and its overrides in one PR)? Owner: spec.
- How many fix attempts per PR before Claude gives up and comments? Owner: Matthew.
- Should Dependabot/Renovate's own security-update PRs overlap with, or replace, the weekly `/maintain-scan` intents for dependency CVEs? Owner: Matthew.
