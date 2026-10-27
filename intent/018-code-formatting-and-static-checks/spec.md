# Spec: Add code formatting and static checks
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Make the Maven build enforce one Java style and two static analyses, and bring the existing code up to them in the same PR. Five parts:

1. **Formatting.** `spring-javaformat-maven-plugin` runs `validate` in the build (failing on any unformatted file) and `apply` is the single reformat command, for `src/main` and `src/test`.
2. **Checkstyle.** `maven-checkstyle-plugin` runs Spring's ruleset from `spring-javaformat-checkstyle`; any violation fails the build.
3. **Error Prone.** Runs inside `javac` on every compile (main and test); error-level findings fail the build.
4. **JSpecify nullness.** NullAway, run as an Error Prone check in JSpecify mode, fails the build on nullness violations. Every package, main and test, is `@NullMarked`; anything that may be null is `@Nullable`.
5. **Docs and upkeep.** `CLAUDE.md` (and `REVIEW.md` where relevant) say how to format and what the checks expect; Renovate keeps the new tools current.

Out of scope (from the intent): formatting or linting non-Java files, new Snyk/Sonar rules, PetClinic features, branch protection changes. No user-visible behaviour changes.

## 2. Existing codebase context
- `pom.xml`: Boot parent 4.1.2, `java.version` 27, only `spring-boot-maven-plugin` declared; no compiler, formatter or Checkstyle configuration.
- Sources: about 29 main and 22 test classes under `org.springframework.samples.petclinic` and its `owner` and `vet` packages. No `package-info.java` exists. `@Nullable` is not used anywhere, yet `null` flows through the code: `OwnerController` and `PetController` pass `null` models and ids to `form(...)`, `new Owner(null, ...)` creates unsaved aggregates (documented in `OwnerRepository`), `ownerId == null` / `petId == null` distinguish create from edit, and `VisitController` ends an `Optional` chain with `.orElse(null)`.
- Boot 4 / Spring Framework 7 use JSpecify annotations (`org.jspecify.annotations.NullMarked`, `Nullable`) and Boot manages the `org.jspecify:jspecify` version, so the annotations need no new unmanaged library.
- `build.yml` runs `./mvnw -B clean verify` on Temurin 27. The Claude fix workflows (`claude.yml`, `dependency-updates.yml`) must pass the same build. `REVIEW.md` currently says "Do not comment on style, naming or formatting unless it causes a bug".
- `renovate.json5` enables the `maven` manager with a `maven-minor-patch` group, so versions held in `pom.xml` properties are updated.

## 3. Design

### 3.1 Dependencies and versions
All of these are non-Boot-managed unless noted. Exact versions are chosen by the plan as the latest releases that work on Java 27 (A4), held in `pom.xml` `<properties>` so Renovate sees them.

| Tool | Artifact | Reason |
|---|---|---|
| spring-javaformat | `io.spring.javaformat:spring-javaformat-maven-plugin` | Required by the intent; the formatter Spring projects use. Not managed by Boot's parent. |
| Spring Checkstyle rules | `io.spring.javaformat:spring-javaformat-checkstyle` (dependency of the Checkstyle plugin) | Spring's conventions "as shipped with spring-javaformat" (intent). Version must match the formatter. |
| Checkstyle runner | `org.apache.maven.plugins:maven-checkstyle-plugin` and `com.puppycrawl.tools:checkstyle` | Needed to run the ruleset. The plan checks whether Boot manages the plugin; if not it is pinned. The engine version is the one spring-javaformat's own POM builds against. |
| Error Prone | `com.google.errorprone:error_prone_core` in `maven-compiler-plugin` `annotationProcessorPaths` | Required by the intent. |
| NullAway | `com.uber.nullaway:nullaway` in `annotationProcessorPaths` | JSpecify-aware nullness checker for Error Prone (answers the open question; A1). |
| JSpecify | `org.jspecify:jspecify` | Boot-managed; declared without a version. |

`maven-compiler-plugin` is Boot-managed, so only its `<configuration>` is added.

### 3.2 Formatting
- `spring-javaformat-maven-plugin` with `validate` bound to the `validate` phase. `./mvnw spring-javaformat:apply` is the one reformat command.
- Covers `src/main/java` and `src/test/java` (the plugin's defaults).
- The failure output (or `CLAUDE.md`) points at `./mvnw spring-javaformat:apply`.

### 3.3 Checkstyle
- `maven-checkstyle-plugin` `check` goal bound to `validate`, after the formatter, so style failures surface before compilation and need no compiled classes (A5). Every violation fails the build; test sources are included.
- Configuration is the Spring ruleset shipped in `spring-javaformat-checkstyle`, via `configLocation`; no project-local copy of the rules.
- Suppressions go in `src/checkstyle/checkstyle-suppressions.xml`, one narrow `<suppress files=... checks=...>` per entry with an XML comment giving the reason. No whole-package or whole-check suppressions. The PR description lists every one.
- The code is changed to comply wherever possible; a rule is suppressed only when the code cannot reasonably change.

### 3.4 Error Prone
- Configured on `maven-compiler-plugin` with the Error Prone `compilerArgs` (`-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, `-Xplugin:ErrorProne ...`) and `error_prone_core` in `annotationProcessorPaths`; the same for test compilation.
- Error Prone needs access to javac internals on JDK 16+. The required `--add-exports`/`--add-opens` flags go in a new `.mvn/jvm.config`, so they apply to local, IDE-launched and CI `./mvnw` runs (A10). The plan verifies this on Java 27 (C1).
- Default severities: `ERROR` findings fail the build, `WARNING` findings print only, no `-Werror` (A2). A check may be promoted with `-Xep:<Check>:ERROR`. Any `-Xep:<Check>:OFF` needs a reason in a `pom.xml` comment.
- Code-level suppressions use `@SuppressWarnings("<Check>")` on the smallest element, with a comment giving the reason.

### 3.5 JSpecify nullness
- NullAway compiler arguments: `-Xep:NullAway:ERROR`, `-XepOpt:NullAway:OnlyNullMarked=true`, `-XepOpt:NullAway:JSpecifyMode=true`. Further options (custom injection annotations, excluded field annotations) are added only for a concrete framework case, each justified in a `pom.xml` comment.
- `@NullMarked` `package-info.java` in `org.springframework.samples.petclinic`, `...owner` and `...vet` under `src/main/java`. Test classes live in the same packages; if NullAway does not treat them as null-marked through the main package annotation, each test class gets `@NullMarked` instead (A6). All test code ends up null-marked.
- Anything that may be null is annotated `org.jspecify.annotations.Nullable` in type-use position (for example `@Nullable Integer id`). Expected places: generated-id fields and constructor parameters on entities, records, views and forms (`Owner`, `Pet`, `Visit`, and so on), optional `@PathVariable`/`@RequestParam` parameters such as `ownerId`, `petId` and `lastName`, and the `form(model, null)` helper parameters. `Optional.orElse(null)` in `VisitController` is rewritten if a simple non-null alternative exists, otherwise its result is annotated.
- No `@SuppressWarnings("NullAway")`, and no `Objects.requireNonNull` added only to quiet the checker. One narrowly scoped assertion with a comment is allowed where a real runtime contract exists (for example Spring Data returning a saved entity with its id set).
- Annotations are compile-time only. Any code change NullAway forces must preserve behaviour and be covered by existing tests.

### 3.6 Build ordering and time
`validate`: formatter, then Checkstyle. `compile` / `test-compile`: javac with Error Prone and NullAway. Tests as before. Target: a clean `verify` no more than about 30 % slower than before (A8). If it is slower, the PR reports the figure instead of dropping a check.

### 3.7 CI
- `build.yml` already runs `./mvnw -B clean verify`, so it needs no change; `.mvn/jvm.config` applies in CI automatically.
- The Claude fix workflows run the same Maven build, so an unformatted fix fails `build`. `CLAUDE.md` tells agents to run `./mvnw spring-javaformat:apply` before committing. The plan checks the allowed tools in `claude.yml` and `dependency-updates.yml` and changes them minimally, and only if `./mvnw` is not already permitted (C5).

### 3.8 Renovate
`renovate.json5` already covers `maven`. The plan adds a `packageRules` entry grouping `io.spring.javaformat:*` (and the Checkstyle engine version) so they move in one PR, and keeps Error Prone and NullAway versions in `<properties>` so Renovate can see them; they fall into `maven-minor-patch`.

### 3.9 Documentation
- `CLAUDE.md`, Commands: add `./mvnw spring-javaformat:apply` and note that `verify` fails on unformatted code. Add a "Code style and static checks" section: Spring Java Format (tabs), Spring Checkstyle rules, Error Prone, and the JSpecify rules (new packages must be `@NullMarked`; use `org.jspecify.annotations.Nullable`, not other `@Nullable` flavours; no suppression without a stated reason; never suppress to get green). Update the CI section to say `build` enforces these.
- `REVIEW.md`: keep "Do not comment on style" (the tools enforce it). Add one conformance bullet: a new Error Prone/NullAway `@SuppressWarnings`, an `-Xep:...:OFF` flag or a Checkstyle suppression with no stated reason is a `[minor]` finding.
- `/spec` skill: no change; the conventions live in `CLAUDE.md`, which the skill already reads.

## 4. Delivery in commits
To keep the diff reviewable (A3), the PR has separate commits in this order:
1. Tooling: `pom.xml`, `.mvn/jvm.config`, docs, Renovate rule. The build may be red here; the message says so.
2. Mechanical format: only the output of `./mvnw spring-javaformat:apply`.
3. Checkstyle and Error Prone fixes that are not formatting.
4. JSpecify: `package-info.java` files, `@Nullable` annotations and the minimal code changes NullAway demands.
5. Suppressions, if any, with reasons.

The final PR state is green, and every commit after the first leaves behaviour unchanged. Listing commit 2 in `.git-blame-ignore-revs` is optional (A7).

## 5. Acceptance criteria
1. `./mvnw clean verify` passes locally on the pinned JDK and in `build`, with all existing tests unchanged in behaviour.
2. An unformatted line in any Java file under `src/main` or `src/test` makes `./mvnw verify` fail, and `./mvnw spring-javaformat:apply` fixes it.
3. A Spring Checkstyle violation (for example a missing `this.` on a field access) fails `verify`.
4. An error-level Error Prone bug pattern fails compilation, in main and in test sources.
5. Returning `null` from a non-`@Nullable` method, or dereferencing a `@Nullable` value unchecked, fails compilation, in main and in test sources.
6. All main packages are `@NullMarked`; all test code is null-marked. No `@SuppressWarnings("NullAway")` exists.
7. Every suppression or disabled check is narrow and carries a reason; the PR description lists them.
8. Each non-Boot tool has its version in `pom.xml` and its reason in section 3.1; Renovate groups spring-javaformat and Checkstyle updates.
9. No change to templates, CSS, JS, YAML, SQL or user-visible behaviour; no `application.properties`.
10. `CLAUDE.md` and `REVIEW.md` are updated as in 3.9, and the Claude fix workflows can pass the checks.
11. CI build time rises by at most about 30 % (A8), or the PR reports the actual figure.

## 6. Test strategy
- The existing test suite is the behavioural safety net.
- Criteria 2-5 are each proven once by hand (introduce the defect, see the failure, revert) and recorded in the PR description. No permanent failing sources are committed, since they would break the build.
- Run `spring-tools:validate` on the result. Run `actionlint` only if workflow files change.

## 7. Areas of concern
- **C1: Java 27 support.** Error Prone, NullAway, Checkstyle and spring-javaformat all depend on javac or parser internals and lag new JDKs. This spec cannot verify from the repository that releases supporting Java 27 exist. If one does not, the plan stops and reports it; it does not lower `java.version` (which `CLAUDE.md` pins) or silently drop the tool. Commit 1 doubles as a spike confirming all four tools run.
- **C2: Spring style vs existing code.** The code was written without these rules, so the reformat and Checkstyle fixes may be large. Separate commits are the mitigation.
- **C3: `REVIEW.md` vs this intent.** The review's "no style comments" rule stays valid because tools now enforce style; the new suppression bullet is a narrow exception.
- **C4: "No blanket disabling" vs a strict ruleset.** Spring's Checkstyle rules are written for Spring's own code. If many checks fire on tests, a broad test suppression is forbidden; the cost is more code changes. If volume makes the PR unreviewable, escalate to a human rather than suppress.
- **C5: Fix workflows cannot be validated in-PR.** Per `CLAUDE.md`, Claude workflows skip on a PR that changes them, so edits to `claude.yml` or `dependency-updates.yml` are first exercised after merge. The design avoids editing them unless necessary.
- **C6: Conflicts with open branches.** The mechanical reformat touches nearly every Java file, so open branches with Java changes will conflict. Merge when few are open; Renovate rebases on conflict.
- **C7: NullAway and reflective injection.** Spring Data JDBC and form binding set values reflectively, which NullAway can flag as uninitialised fields. Records with constructor binding avoid this; otherwise `@Nullable` on the field is preferred over exclusion annotations.

## 8. Assumptions
- **A1.** NullAway is the JSpecify checker (the intent's suggestion), run on Error Prone with `JSpecifyMode` and `OnlyNullMarked`. Its JSpecify support is partial (for example generics); accepted.
- **A2.** Only error-level findings fail the build; warnings print only (the intent's assumption).
- **A3.** Formatting and behavioural fixes are separate commits in one PR (the intent's assumption "yes"), not separate PRs, because the intent requires the build green in the same PR.
- **A4.** No versions are fixed in this spec because they cannot be verified from here; the plan picks the latest that work on Java 27.
- **A5.** Checkstyle runs in the `validate` phase after the formatter, not `verify`, so style fails fast. It moves to `verify` only if the ruleset needs compiled classes.
- **A6.** One `@NullMarked` `package-info.java` per main package covers the same-named test packages; per-class `@NullMarked` on tests is the fallback.
- **A7.** `.git-blame-ignore-revs` is optional and decided in the plan.
- **A8.** "Reasonable build time" means a clean `verify` no more than about 30 % slower.
- **A9.** Only Java is formatted; `pom.xml` indentation is not enforced.
- **A10.** A new `.mvn/jvm.config` is an acceptable home for Error Prone's `--add-exports`/`--add-opens` flags.
- **A11.** If the Spring ruleset enables a licence-header check and the files carry no header, that check is disabled by one stated suppression; adding headers to every file is out of scope.
