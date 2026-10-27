# Plan: intent 018, code formatting and static checks

## Context
Nothing in the build enforces Java style or catches bug patterns or null-safety mistakes (intent 018). The spec (`intent/018-code-formatting-and-static-checks/spec.md`) adds spring-javaformat, Spring's Checkstyle rules, Error Prone and NullAway in JSpecify mode, brings the existing ~32 main and 22 test classes up to them, and documents the rules for agents. Done means `./mvnw clean verify` is green on Temurin 27 and in `build`, with no change in behaviour. This plan is saved as `intent/018-code-formatting-and-static-checks/plan.md`.

## Versions (latest on Maven Central, 2026-10-26)
Held as `pom.xml` `<properties>` so Renovate sees them:

| Property | Version | Note |
|---|---|---|
| `spring-javaformat.version` | 0.0.48 | formatter plugin and `spring-javaformat-checkstyle` |
| `checkstyle.version` | 9.3 | the engine `spring-javaformat-checkstyle` 0.0.48 depends on (spec 3.1) |
| `maven-checkstyle-plugin.version` | 3.6.0 | not managed by Boot 4.1.2 (checked in `spring-boot-dependencies`) |
| `error-prone.version` | 2.51.0 | released 2026-10-22 |
| `nullaway.version` | 0.14.3 | |
| jspecify | Boot-managed (1.0.1) | no version |

`maven-compiler-plugin` is Boot-managed (3.15.0); only its configuration is added. The parent's `<parameters>true</parameters>` stays.

## Commit 1: tooling (build may be red)
- **`pom.xml`**
  - Add the `org.jspecify:jspecify` dependency.
  - `spring-javaformat-maven-plugin`: `validate` goal in the `validate` phase.
  - `maven-checkstyle-plugin`: `check` in `validate`, after the formatter. Dependencies `com.puppycrawl.tools:checkstyle:${checkstyle.version}` and `io.spring.javaformat:spring-javaformat-checkstyle:${spring-javaformat.version}`. `configLocation` `io/spring/javaformat/checkstyle/checkstyle.xml`, `includeTestSourceDirectory` true, `failOnViolation`/`violationSeverity=warning` so every violation fails the build, and `suppressionsLocation` `src/checkstyle/checkstyle-suppressions.xml`, created only if a suppression is needed.
  - `maven-compiler-plugin` configuration: `annotationProcessorPaths` with `error_prone_core` and `nullaway`. `compilerArgs`: `-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, and `-Xplugin:ErrorProne -Xep:NullAway:ERROR -XepOpt:NullAway:OnlyNullMarked=true -XepOpt:NullAway:JSpecifyMode=true`. This applies to `compile` and `testCompile`. No `-Werror`. Each pom block gets a short comment giving the reason for the non-Boot tool.
- **`.mvn/jvm.config`** (new): the `--add-exports jdk.compiler/com.sun.tools.javac.{api,file,main,model,parser,processing,tree,util}=ALL-UNNAMED` and `--add-opens jdk.compiler/com.sun.tools.javac.{code,comp}=ALL-UNNAMED` flags Error Prone documents.
- **Spike (C1):** `sdk env`, then `./mvnw clean verify`. Confirm that each of the four tools loads and reports on Java 27, even if it reports findings. If a tool fails to run on 27 at all (for example a javac internals error, as opposed to findings), stop and tell the user. Do not lower `java.version` and do not drop the tool.
- **`renovate.json5`**: a `packageRules` entry, `groupName: "spring-javaformat"`, matching `io.spring.javaformat:**` and `com.puppycrawl.tools:checkstyle`, placed after `maven-minor-patch` so it takes precedence. Error Prone and NullAway stay in `maven-minor-patch`.
- **`.github/workflows/claude.yml`** (user's choice): add `Bash(./mvnw:*)` to the existing `--allowed-tools` string, and change nothing else. Run `actionlint .github/workflows/*.yml`. Note in the PR that the Claude workflows skip on this PR because it changes them.
- **Docs**
  - `CLAUDE.md`, Commands: `./mvnw spring-javaformat:apply  # reformat; verify fails on unformatted code`. Add a "Code style and static checks" section: Spring Java Format (tabs); the Spring Checkstyle rules (`this.` on fields, `final` classes with private constructors, javadoc rules, and so on); Error Prone ERROR fails the build; NullAway with JSpecify, so every new package needs a `@NullMarked` `package-info.java`, uses `org.jspecify.annotations.Nullable` in type-use position only, and never suppresses to get green; any suppression is narrow and states its reason. Run `spring-javaformat:apply` before committing. In the CI section, say `build` enforces these.
  - `REVIEW.md` conformance: one bullet saying that a new `@SuppressWarnings` for an Error Prone or NullAway check, an `-Xep:…:OFF`, or a Checkstyle suppression without a stated reason is `[minor]`. Keep the "no style comments" rule.

## Commit 2: mechanical format
Only the output of `./mvnw spring-javaformat:apply`. The code already looks Spring-formatted (tabs, `this.`, parenthesised lambda params), so this should be small. If it touches more than a handful of files, add `.git-blame-ignore-revs` with that commit's SHA in a follow-up commit (A7). Otherwise skip it.

## Commit 3: Checkstyle and Error Prone fixes
Fix each reported violation in code. Likely examples: hidden-field and final-class rules, javadoc style, and test method naming. Suppress only when the code genuinely can't change: one `<suppress files=… checks=…>` per entry, each with an XML comment (A11 if a header check fires). If the Checkstyle volume on tests is too big to review, stop and ask the user. Do not add a broad suppression (C4).

## Commit 4: JSpecify / NullAway
- `@NullMarked` `package-info.java` in `org.springframework.samples.petclinic`, `.owner` and `.vet` under `src/main/java`. Then check whether test compilation treats the test classes as null-marked: put a deliberate `return null` in a test helper and confirm it fails. If it doesn't fail, add `@NullMarked` to each test class (A6).
- `@Nullable` on what really may be null. That covers the `@Id Integer id` components of `Owner`, `Pet`, `Visit`, `PetType`, `Vet`, `Specialty` and similar records. It covers the `ownerId`/`petId` parameters of `OwnerController.form(…)` and `PetController.form(…)`/`checkContext(…)`, the optional `lastName` request param in `OwnerController`, and anything else NullAway shows to be nullable.
- Then fix the dereferences that follow from nullable ids while keeping behaviour. Use `Objects.equals(pet.id(), petId)` in place of `pet.id().equals(petId)` (as in `VisitController.findPet`, `form`). Persisted aggregates passed to non-null parameters (for example `visits.insert(pet.id(), …)`) get the narrowest fix: a null check that is already a real contract, or one commented assertion per spot where Spring Data guarantees a saved id (spec 3.5). List each one in the PR.
- `VisitController.form`: `.orElse(null)` for `typeName`. If a non-null rewrite keeps the rendered output identical, use it. Otherwise declare `@Nullable String typeName`.
- No `@SuppressWarnings("NullAway")`. Any NullAway option beyond the three is added only for a concrete case, with a pom comment.

## Commit 5: suppressions (only if any remain), each with a reason.

## Verification
1. `./mvnw clean verify` is green. Time it against a baseline run taken before commit 1 to check spec A8, a slowdown of no more than about 30%.
2. Prove the failure modes by hand, then revert each one. Record them in the PR description:
   - Unformatted line → `verify` fails; `spring-javaformat:apply` fixes it.
   - Remove a `this.` → Checkstyle fails in `validate`.
   - An ERROR-level pattern (`DeadException`: `new IllegalStateException();` without `throw`) fails in main and in test.
   - `return null` from a non-`@Nullable` method, and an unchecked deref of a `@Nullable` value, each fail in main and in test.
3. `grep -r 'SuppressWarnings("NullAway")' src` is empty.
4. `spring-tools:validate` and `actionlint .github/workflows/*.yml` both pass.
5. `git diff main --stat` touches no templates, CSS, JS, YAML (other than `claude.yml`) or SQL.
6. Branch, push, and open a PR. The description lists every suppression, the hand-proof results and the build-time figure. CI `build` is green.

## Critical files
`pom.xml`, `.mvn/jvm.config` (new), `renovate.json5`, `.github/workflows/claude.yml`, `CLAUDE.md`, `REVIEW.md`, `src/main/java/**/package-info.java` (new), the records and controllers in `src/main/java/org/springframework/samples/petclinic/{owner,vet}/`, and possibly `src/checkstyle/checkstyle-suppressions.xml`. The spec gets a deviation note for the `claude.yml` change, which spec C5 had avoided.
