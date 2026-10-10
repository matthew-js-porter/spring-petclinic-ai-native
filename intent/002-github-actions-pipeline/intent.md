# Intent: GitHub Actions build and AI-native SDLC pipeline
Author: matthew-js-porter

## Problem
"Setup a build pipeline for the app using github actions. Should have one for building the app using maven but also the steps in the AI SDLC Playbook." Today nothing runs automatically on this repo: a broken build or test can be merged unnoticed, and the playbook's automated stages (Design, Review, Maintain) exist only as written guidance, so they depend on someone remembering to do them by hand.

## Proposed outcome
Workflows under GitHub Actions that deliver the following observable results:

- **Build:** every push to `main` and every pull request runs the Maven build and all tests (the equivalent of `./mvnw clean verify`) on the repo's pinned Java version, and fails visibly if either fails.
- **Design (spec on intent merge):** when an `intent.md` merges to `main`, a non-interactive Claude job applies the `/spec` skill and opens a pull request containing the matching `spec.md` beside the intent. A human still reviews and merges it.
- **Review (AI PR review):** every pull request gets an AI review against a `REVIEW.md` policy (bugs, security, compliance with the spec and plan), with findings tagged by severity. Findings are advisory; they neither approve nor block. An `@claude` mention on a review comment triggers a fix pushed to the PR.
- **Maintain (weekly security scan):** a weekly scheduled job runs Snyk scans of the repo (dependencies and static code analysis) (also run on pull requests), plus a Claude pass that validates the findings and writes them as new `intent.md` files for triage, via a PR.
- **Security scanning:** Snyk (dependency and static code analysis) is configured as part of the pipeline, with results visible in the workflow run. CodeQL is not used.
- Agent output always arrives as a pull request, never a direct push to `main`.

Out of scope: the playbook's Test-stage eval suite, any deploy or rollback pipeline (no deploy target exists yet), runtime monitoring/baseline bands, and PetClinic features.

## Affected users and systems
- Developers and agents opening PRs against this repo.
- The product owner / reviewers who gate intents, specs and PRs.
- New `.github/workflows/` definitions, a `REVIEW.md` policy, and GitHub repo settings (secrets).
- Existing `intent/` and `.claude/skills/` conventions, which the Design and Maintain jobs read and write.

## Constraints
- Java 25, as pinned in `pom.xml` and `.sdkmanrc`; the CI JDK must match.
- Claude authenticates in Actions via a `CLAUDE_CODE_OAUTH_TOKEN` repository secret (generated with `claude setup-token`), so runs use the Pro subscription rather than API billing; Snyk via a `SNYK_TOKEN` secret; no credentials in the diff.
- The user is on the Claude Pro plan, so Claude-driven jobs count against subscription usage limits and must be economical (weekly scan, PR review, intent-merge spec only; cap turns and set job timeouts).
- Uses the official `anthropics/claude-code-action` and the Claude GitHub App installed on the repo. Scheduled runs and PR review run in automation mode with a `prompt`. Skills in `.claude/skills/` need a checkout step.
- Commits pushed by the action must authenticate as the Claude GitHub App (not the default `GITHUB_TOKEN`) so that the build workflow triggers on them.
- Humans stay above the gate: agent jobs can open PRs but cannot merge them; a human reviews and merges.
- Claude jobs run non-interactively and must not commit directly to `main`.
- Keep the existing layout (`intent/<nnn>-<name>/`, YAML-only config).

## Open questions
- Does the Pro subscription's usage allowance comfortably cover PR review plus the weekly scan? Revisit after the first few runs. Owner: Matthew.

Decisions recorded: branch protection and code owners are out of scope; the Design job uses the existing `/spec` skill as-is; evals and deploy are a separate future intent; a Snyk account exists; Claude auth uses the subscription OAuth token.
