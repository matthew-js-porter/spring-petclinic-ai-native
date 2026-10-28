# Review policy

Read by the Claude review job (`.github/workflows/claude-code-review.yml`) and by human reviewers. Reviews are advisory: never approve, request changes, or fail a check because of findings. Humans decide and merge.

Security is not covered here: a separate job in the same workflow runs Claude Code's built-in `/security-review`.

## What to check

Do two passes over the diff:

1. **Bugs.** Logic errors, wrong null/empty handling, broken error paths, resource leaks, concurrency problems, tests that do not assert what they claim.
2. **Conformance.**
   - If the change touches an `intent/<nnn>-<name>/` folder or implements one, compare it with that folder's `spec.md` and `plan.md`. Report missing requirements and undocumented deviations.
   - `CLAUDE.md` rules, e.g. YAML-only config (no `application.properties`), Java 27, Boot 4 test packages, no actuator exposure beyond `health` without a spec that asks for it.
   - No workflow or agent pushes directly to `main`.
   - **Spring Boot overrides.** If the diff changes the `spring-boot-starter-parent` `<version>` in `pom.xml`, each `<properties>` entry still in `pom.xml` that overrides a property Spring Boot's parent defines (for example `tomcat.version`, `jackson-bom.version`; never `java.version`, and never properties Boot does not define) is a `[major]` finding, whatever its version: the pin no longer follows Boot and holds the library back once Boot moves past it. Post one comment for all of them on the parent `<version>` line, listing each property and value, and end it with this exact request (its wording is what lets `claude.yml` act on a bot comment): `@claude remove these Spring Boot version overrides from pom.xml, touching no other line, commit as "Remove Spring Boot version overrides for Boot <new version>", push to this branch, and reply with each removed property and its old value. Do not repeat this request in your reply.` If no overrides remain, post nothing for this rule.
   - **Boot-managed dependencies.** If the diff adds a `<dependency>` or `<plugin>` to `pom.xml` that carries its own `<version>`, or whose version the Spring Boot parent does not manage, it is a `[minor]` finding on that line unless the linked `spec.md` states a reason for it (`CLAUDE.md` favours Boot-managed dependencies). Post nothing when the spec gives a reason.
   - **Architecture rules.** A diff that deletes or weakens a rule in `src/test/java/org/springframework/samples/petclinic/architecture/` (or its fixtures in `src/test/java/org/springframework/samples/architecture/fixtures/`), or that adds `@ArchIgnore`, an `archunit_ignore_patterns.txt`, a `FreezingArchRule`, `allowEmptyShould(true)`, or an exclusion inside a rule without a one-line reason, is a `[major]` finding: the rules exist to fail the build, and the policy is to fix the violation at its source. A new package without a `package-info.java` annotated `@NullMarked` fails `HygieneRulesTests`, so it needs no separate comment. The `archunit-junit5` `<version>` in `pom.xml` is justified by `intent/019-architecture-rules/spec.md`, so the Boot-managed dependencies rule posts nothing for it.
   - **Java pins and `.sdkmanrc`.** Renovate moves the Java major in `pom.xml` (`java.version`) and the `setup-java` `java-version` in `build.yml`, `snyk.yml`, `sonar.yml` and `dependency-updates.yml`, but never `.sdkmanrc`: SDKMAN identifiers differ per platform and old builds are withdrawn, so no datasource proposes one that installs. If the diff changes any of those Java majors and `.sdkmanrc` still names a different major, that is a `[major]` finding on the changed Java line (the `<java.version>` line if it changed), since `.sdkmanrc` is not in the diff: local `sdk env install` stays on the old JDK while CI builds with the new one. Do not suggest an identifier yourself; end the comment with this exact request (its wording is what lets `claude.yml` act on a bot comment): `@claude update .sdkmanrc to the Temurin identifier for Java <new major> that sdk list java offers, touching no other line, commit as "Update .sdkmanrc for Java <new major>", push to this branch, and reply with the old and new identifier. Do not repeat this request in your reply.` If `.sdkmanrc` already names the new major, post nothing for this rule.

## Severity

Prefix every comment with one tag:

- `[critical]`: exploitable security issue, data loss, secret leak, or the build/app is broken on merge.
- `[major]`: incorrect behaviour in a realistic scenario, a missed spec requirement, or a policy breach.
- `[minor]`: a small correctness or robustness issue that is still worth fixing.

## Rules

- Only report an issue if you can state a concrete failure scenario: the inputs or state, and what goes wrong.
- Do not comment on style, naming or formatting unless it causes a bug.
- One comment per issue, on the most relevant line. Suggest a fix when it is short. The one exception is the Spring Boot overrides rule, which posts a single comment for all overrides so they are removed in one `@claude` run.
- No praise or summary comments. If nothing qualifies, post nothing.
