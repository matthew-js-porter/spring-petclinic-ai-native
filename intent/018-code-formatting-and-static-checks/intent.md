# Intent: Add code formatting and static checks
Author: matthew-js-porter

## Problem
"Add code formatting spring-javaformat. Error prone with JSpecify checks." Nothing in the build keeps the Java code consistently formatted or checks it against Spring's coding conventions, so style drifts between human and agent changes and reviews spend time on it. Common bug patterns and null-safety mistakes are only caught if a reviewer, Snyk or Sonar happens to notice them, and the code does not state which values may be null.

## Proposed outcome
- The Java code (main and test) is formatted with spring-javaformat, and the build fails when any file is not formatted; contributors and agents have a single command to reformat.
- Spring's Checkstyle conventions, as shipped with spring-javaformat, are enforced, and a violation fails the build.
- Error Prone runs on every compile, and an error-level finding fails the build.
- Null-safety is checked with JSpecify: all packages, main and test, are null-marked, anything that may be null is annotated as such, and a nullness violation fails the build.
- The existing code passes all of the above in the same PR: `./mvnw clean verify` is green locally and in CI, with no change to user-visible behaviour.
- `CLAUDE.md` (and `REVIEW.md` where relevant) say how to format and what the checks expect, so agents produce code that passes first time.

Out of scope: formatting or linting non-Java files (templates, CSS, JS, YAML); new Snyk/Sonar rules; PetClinic features; branch protection changes.

## Affected users and systems
- Developers and agents writing Java in this repo, including Claude fix workflows (`claude.yml`, `dependency-updates.yml`), whose pushes must pass the new checks.
- The Maven build and `build.yml` in CI.
- All Java sources under `src/main` and `src/test`.
- `CLAUDE.md`, `REVIEW.md`, and possibly the `/spec` skill if it should mention the conventions.

## Constraints
- Java 27 (pinned in `pom.xml` and `.sdkmanrc`); all tools must work on it.
- Favour versions Spring Boot manages; spring-javaformat, Checkstyle, Error Prone and any null checker are non-Boot, so each needs a stated reason and version in the spec, and Renovate should keep them updated.
- Suppressions or exclusions must be narrow and carry a reason; no blanket disabling to get green.
- No pushes to `main`; everything lands through a branch and PR.
- Build time should stay reasonable for the CI and local loop.

## Open questions
- Which tool enforces JSpecify nullness on Error Prone (NullAway is the usual choice)? Owner: spec.
- Should Error Prone warnings (not just errors) fail the build, or only selected ones? Assumed errors only. Owner: Matthew / spec.
- Should formatting run in a separate commit from the behavioural fixes, so the diff is reviewable? Assumed yes. Owner: spec.
