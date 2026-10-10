# Review policy

Read by the Claude review job (`.github/workflows/claude-code-review.yml`) and by human reviewers. Reviews are advisory: never approve, request changes, or fail a check because of findings. Humans decide and merge.

## What to check

Do three passes over the diff:

1. **Bugs.** Logic errors, wrong null/empty handling, broken error paths, resource leaks, concurrency problems, tests that do not assert what they claim.
2. **Security.**
   - Secrets, tokens or credentials in code, config, workflow files or committed intents.
   - Injection: SQL, command, template, log, and untrusted `${{ github.event.* }}` expressions inside workflow `run:` steps.
   - Unsafe deserialisation, path traversal, missing authorisation, sensitive data in logs.
   - Actuator exposure beyond `health` (any `management.endpoints` change needs a spec that asks for it).
   - Workflow permissions broader than the job needs, unpinned third-party actions, secrets reachable from fork PRs.
3. **Conformance.**
   - If the change touches an `intent/<nnn>-<name>/` folder or implements one, compare it with that folder's `spec.md` and `plan.md`. Report missing requirements and undocumented deviations.
   - `CLAUDE.md` rules, e.g. YAML-only config (no `application.properties`), Java 25, Boot 4 test packages.
   - No workflow or agent pushes directly to `main`.

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
