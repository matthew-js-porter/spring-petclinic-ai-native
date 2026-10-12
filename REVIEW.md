# Review policy

Read by the Claude review job (`.github/workflows/claude-code-review.yml`) and by human reviewers. Reviews are advisory: never approve, request changes, or fail a check because of findings. Humans decide and merge.

Security is not covered here: a separate job in the same workflow runs Claude Code's built-in `/security-review`.

## What to check

Do two passes over the diff:

1. **Bugs.** Logic errors, wrong null/empty handling, broken error paths, resource leaks, concurrency problems, tests that do not assert what they claim.
2. **Conformance.**
   - If the change touches an `intent/<nnn>-<name>/` folder or implements one, compare it with that folder's `spec.md` and `plan.md`. Report missing requirements and undocumented deviations.
   - `CLAUDE.md` rules, e.g. YAML-only config (no `application.properties`), Java 25, Boot 4 test packages, no actuator exposure beyond `health` without a spec that asks for it.
   - No workflow or agent pushes directly to `main`.
   - **Java pins and `.sdkmanrc`.** Renovate moves the Java major in `pom.xml` (`java.version`) and the `setup-java` `java-version` in `build.yml` and `snyk.yml`, but never `.sdkmanrc`: SDKMAN identifiers differ per platform and old builds are withdrawn, so no tool can pick one that installs. If the diff changes any of those Java majors and `.sdkmanrc` still names a different major, that is a `[major]` finding on the changed Java line (the `<java.version>` line if it changed), since `.sdkmanrc` is not in the diff: local `sdk env install` stays on the old JDK and the build's pin check fails. Ask a human to set it to an identifier `sdk list java` offers for the new major; do not suggest one yourself.

## Severity

Prefix every comment with one tag:

- `[critical]`: exploitable security issue, data loss, secret leak, or the build/app is broken on merge.
- `[major]`: incorrect behaviour in a realistic scenario, a missed spec requirement, or a policy breach.
- `[minor]`: a small correctness or robustness issue that is still worth fixing.

## Rules

- Only report an issue if you can state a concrete failure scenario: the inputs or state, and what goes wrong.
- Do not comment on style, naming or formatting unless it causes a bug.
- One comment per issue, on the most relevant line. Suggest a fix when it is short.
- No praise or summary comments. If nothing qualifies, post nothing.
