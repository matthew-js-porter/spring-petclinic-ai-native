# Spec: Add ArchUnit architecture rules
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add ArchUnit to the test scope and a small set of architecture rules that run in `./mvnw clean verify`, locally and in CI. Five parts:

1. **Dependency.** `com.tngtech.archunit:archunit-junit5` in test scope, version in a `pom.xml` property so Renovate keeps it current. Reason stated in 3.1.
2. **Hygiene rules** (main and test code), adapted from the Spring Framework and Spring Boot `ArchitectureRules`.
3. **Layering rules** (main code only) for the web / repository / domain split, the `owner` / `vet` feature boundary, injection style and stereotype placement and naming.
4. **Violation clean-up.** Any existing violation is fixed in the same PR, with no suppressions and no visible behaviour change.
5. **Docs.** `CLAUDE.md` describes the rules and how to run them; `REVIEW.md` flags a change that weakens a rule or adds a suppression.

Out of scope (from the intent): auto-configuration and `@ConfigurationProperties` rules, rules for non-Java files, repackaging beyond what a fix needs, PetClinic features, branch protection changes.

## 2. Existing codebase context
- Root package `org.springframework.samples.petclinic` holds `PetClinicApplication` and `WelcomeController`. Feature packages `owner` and `vet` are flat: each mixes entities (records with `@Table`), `*Repository` interfaces (extend `Repository<T, ID>`), `@Controller` classes, `*View` records, `*Form` records and helpers (`IsoDates`, `Owners`, `Vets`).
- Controllers are package-private and use constructor injection. A scan of `src/main` finds no field injection, no `toUpperCase()`/`toLowerCase()`, no `Collectors.toList()`, no `URLEncoder`/`URLDecoder`, and no import from `owner` into `vet` or the reverse. The first run may still find violations (for example package cycles through the root package, or test code); the PR fixes them.
- Tests use `@Autowired` fields in test classes (e.g. `VetRepositoryTests`). Layering rules do not cover test code, so these are fine; the field-injection rule is a main-code rule.
- `pom.xml`: Boot parent 4.1.2, `java.version` 27, no explicit plugins besides `spring-boot-maven-plugin`. Tests run in `verify` through Boot-managed Surefire (JUnit 5/6 via Boot).
- `CLAUDE.md` requires a stated reason for any non-Boot dependency; `REVIEW.md` has a "Boot-managed dependencies" rule that stays silent when the spec gives a reason.
- Intent 018 (spring-javaformat, Checkstyle, Error Prone/NullAway) is not yet on `main` (A1).

## 3. Design

### 3.1 Dependency
- Add `com.tngtech.archunit:archunit-junit5` with `<scope>test</scope>`; its version lives in `<archunit.version>` in `<properties>`, which Renovate's Maven manager updates in the `maven-minor-patch` group.
- **Stated reason (required by `CLAUDE.md` / `REVIEW.md`):** ArchUnit is the de-facto library for architecture tests on the JVM, is what the Spring Framework and Boot rules are written in, and Spring Boot does not manage it. No Boot-managed alternative exists.
- **Version:** the latest 1.x release whose ASM can read Java 27 class files. The plan must verify this by running the rules on the compiled classes; ArchUnit's class importer fails on class file versions its bundled ASM does not know. If no released version reads Java 27, this is a blocker (see C1) and the plan stops and reports rather than lowering `java.version` or skipping classes.
- `archunit-junit5` brings the JUnit 5 engine. Boot's JUnit BOM is already on the classpath; the plan checks for an engine/version clash and, if one appears, uses `archunit` (core) and calls it from plain Jupiter tests, which is functionally the same.

### 3.2 Test layout and execution (resolves the intent's second question)
Rules are ordinary JUnit tests run by Surefire in `verify`, so there is no new build step, no new plugin, and a violation fails the build and the existing `build.yml` check with no workflow change.

- Package `org.springframework.samples.petclinic.architecture` under `src/test/java`:
  - `ArchitectureRules` (package-private final class): shared constants and `ArchRule` factories, so the rule text is in one place.
  - `HygieneRulesTests`: imports main and test classes; one `@ArchTest` per hygiene rule.
  - `LayeringRulesTests`: imports main classes only (`ImportOption.DoNotIncludeTests`); one `@ArchTest` per layering rule.
- Separate classes so a failure names its category and each can be run alone: `./mvnw test -Dtest=LayeringRulesTests`.
- `@AnalyzeClasses(packages = "org.springframework.samples.petclinic")` with `@ArchTest` fields/methods. Classes are imported once per test class (ArchUnit caches within a class). The architecture package itself is excluded from the imported set, so rules about tests do not trip on the rule code.
- Every rule uses `.as("...")` and `.because("...")` so the failure message states the rule and what to do instead (e.g. "Use String.toUpperCase(Locale), not toUpperCase()"). ArchUnit adds the offending class and member to each violation.
- Each rule has `allowEmptyShould(false)` (the ArchUnit default), so a rule that matches nothing, for instance after a package rename, fails instead of silently passing. `archunit.properties` is not added.
- Build-time budget: importing about 40 main and 25 test classes is sub-second to a few seconds; no `@SpringBootTest` is used.

### 3.3 Hygiene rules (main + test)
Adapted from Spring Framework / Boot `ArchitectureRules`, kept only where they apply to an application:

| # | Rule | Message hint |
|---|------|--------------|
| H1 | No package cycles (`slices().matching("org.springframework.samples.petclinic.(*)..").should().beFreeOfCycles()`, plus root vs. feature packages) | break the cycle; move the shared type to the package that owns it |
| H2 | `String.toUpperCase()` / `toLowerCase()` without args not called | pass `Locale.ROOT` or the user's locale |
| H3 | `Collectors.toList()` not called | use `Stream.toList()` |
| H4 | `URLEncoder.encode(String)` / `URLDecoder.decode(String)` without a `Charset` not called | pass `StandardCharsets.UTF_8` |
| H5 | `@Bean` methods returning `BeanPostProcessor` or `BeanFactoryPostProcessor` are `static` | add `static` |
| H6 | `@Bean` methods do not return private types | return a public/package type or an interface |
| H7 | No `System.out` / `System.err` / `printStackTrace` use | use SLF4J (assumed addition, A5) |

Excluded as not applicable: anything about `@AutoConfiguration`, `@ConfigurationProperties`, `@ConditionalOn*`, reactive testing, `@Deprecated(since)` conventions of framework code, and Boot's `Objects.requireNonNull` → `Assert` rule (A3).
H5 and H6 currently have nothing to match (no such `@Bean` methods); they use `allowEmptyShould(true)` with a comment that they guard future configuration classes (the only permitted use of that flag, A4).

### 3.4 Layering rules (main only)
The `owner` and `vet` packages are flat, so layers are identified by role (annotation, supertype, or name suffix) not by package:

- **Web** = classes annotated `@Controller`/`@RestController`/`@ControllerAdvice`.
- **Repository** = interfaces assignable to `org.springframework.data.repository.Repository`.
- **Domain** = classes annotated `@Table`.
- **Presentation types** = classes named `*View`, `*Form`, `Owners`, `Vets` (no stereotype to key on; matched by suffix, see A6).

Rules:
- L1 Layered architecture (`layeredArchitecture().consideringOnlyDependenciesInLayers()`): Web may access Repository, Domain, Presentation; Repository may access Domain only; Domain may access nothing in Web or Repository (and may not access Presentation); Presentation may access Domain (view mappers) but not Web or Repository.
- L2 Domain and repositories do not depend on web types (explicit restatement of the intent bullet; gives a sharper message than L1).
- L3 Features (`owner`, `vet`) do not depend on each other: `slices().matching("..petclinic.(*)..")` with `should().notDependOnEachOther()`, restricted to `owner` and `vet`. The root package may depend on features; features may not depend on the root package's types except via Spring annotations, and `PetClinicApplication` is not depended on by anyone.
- L4 No field injection: no field is annotated `@Autowired`, `@Inject` or `@Resource`; a class with a single constructor takes its collaborators through it. Test code is not covered (A7).
- L5 Stereotype placement:
  - `@Controller` classes reside in the root package or a feature package and are named `*Controller`; `*Controller` classes carry a Spring web stereotype.
  - Repository interfaces are named `*Repository`, and `*Repository` types are interfaces extending `Repository`.
  - `@Table` classes are named by the entity (no `View`/`Form`/`Controller`/`Repository` suffix) and are not annotated with web stereotypes.
  - `*Form` / `*View` types carry no Spring stereotype and no `@Table`.
- L6 Web classes are not `public` unless needed: controllers stay package-private (as today) so other packages cannot depend on them. Rule: `@Controller` classes should not be `public`.
- L7 Only web and the application class use `jakarta.servlet` / `org.springframework.web` types; domain and repositories do not (covered by L1 for project types, this covers framework web types).

### 3.5 Existing violations
- Run the rules, record every violation in the PR description, and fix each at source with the smallest change (e.g. add `Locale.ROOT`, `Stream.toList()`, move a shared type, reduce visibility).
- Fixes must not change rendered output, URLs, validation messages or the design-system rules in `docs/design-system/`.
- No `@ArchIgnore`, `ArchRule.ignoreViolations`, `archunit_ignore_patterns.txt`, `FreezingArchRule`, or `.that(not(...))` filters added merely to pass. If a class genuinely cannot satisfy a rule, the exclusion is expressed in the rule definition itself, names the exact class or method, and has a one-line reason on the line above it (C2).
- If a layering rule as written would force a repackage (e.g. splitting `owner` into `web`/`domain` sub-packages), the rule is rewritten to the role-based form above instead; repackaging is out of scope.

### 3.6 Verification that the rules are wired in
- On a scratch branch (not merged, not pushed to `main`), the PR author adds one deliberate violation per category (e.g. a `"x".toUpperCase()` call; a controller field annotated `@Autowired`; an `owner` class importing a `vet` class; a repository importing a controller) and confirms `./mvnw clean verify` fails with a message naming the class and the fix. Results (command, failing test, message) are pasted in the PR description.
- Optionally, and preferably, the rules themselves are tested: `ArchitectureRulesSelfTests` runs each rule against small fixture classes in `src/test/java/.../architecture/fixtures` (one passing and one failing type per rule) using `ClassFileImporter.importClasses`, and asserts `evaluate(...).hasViolation()`. This makes the "rules are wired in" check permanent and runs in `verify`. Fixtures are kept out of the production-class import set by the `architecture` package exclusion in 3.2 (A8).

### 3.7 Documentation
- `CLAUDE.md`: new "Architecture rules" subsection under Project/Commands: where the tests live, the command `./mvnw test -Dtest='HygieneRulesTests,LayeringRulesTests'`, a one-line summary of each rule family, the "fix, don't suppress" policy, and the layering vocabulary (controller = web, `*Repository` = repository, `@Table` = domain, `*View`/`*Form` = presentation). Also add ArchUnit to the CI section note that `build.yml` already runs them.
- `REVIEW.md`: a conformance bullet: a diff that deletes or weakens a rule in `src/test/.../architecture`, adds `@ArchIgnore`, an ignore-patterns file, `FreezingArchRule`, `allowEmptyShould(true)` or an exclusion without a reason is a `[major]` finding. Also state in this bullet that ArchUnit's `<version>` in `pom.xml` is justified by this spec (so the Boot-managed-dependencies rule stays quiet).
- The `/spec` skill is not changed (A9).

## 4. Requirements and acceptance criteria
1. `archunit` is declared test-scope with the version in a property and the reason in this spec. *Check:* `pom.xml` diff; `./mvnw dependency:tree` shows it in test scope only.
2. `./mvnw clean verify` runs `HygieneRulesTests` and `LayeringRulesTests` and passes on the PR's code, locally and in `build.yml`. *Check:* Surefire report lists both classes with the expected number of tests.
3. Every rule in 3.3 and 3.4 exists as a named test with `.as`/`.because`. *Check:* review of the test classes.
4. A violation of any rule fails the build with the class name and corrective hint. *Check:* scratch-branch evidence (3.6) and self tests.
5. No rule is suppressed, frozen or silently empty. *Check:* grep for `ArchIgnore`, `Freezing`, `ignore`, `allowEmptyShould`; the only `allowEmptyShould(true)` uses are H5/H6 with comments.
6. Behaviour is unchanged. *Check:* all pre-existing tests pass unmodified (other than a violation fix needing a test touch, which is noted in the PR); templates and CSS are untouched.
7. `CLAUDE.md` and `REVIEW.md` updated as in 3.7.
8. Added build time under about 10 seconds locally (A10).
9. Rule code complies with whatever formatting and static checks are on `main` when the PR merges (intent 018).

## 5. Areas of concern
- **C1 Java 27 support.** ArchUnit reads class files through a bundled ASM; support for a new class file version usually lags the JDK. This spec cannot confirm that a released ArchUnit reads Java 27 class files. If none does, the choices are: wait, use a snapshot or milestone (not Boot-managed and unreleased, which conflicts with the "reasonable, stable dependency" intent), or lower the test compile level (conflicts with the Java 27 pin). This spec recommends waiting and reporting rather than choosing either, and treats it as a go/no-go check at the start of the plan.
- **C2 Role-based layers vs. strict layering.** The intent wants layering "in main code" but the packages are flat by feature, with entities, forms, views and controllers side by side. Rules keyed on annotations and suffixes are less robust than package rules: a mis-named class escapes a layer. L5 (naming) compensates by making the suffix conventions themselves enforceable. The alternative, splitting packages, is out of scope. Accept the weaker form; revisit if packages are later split.
- **C3 "Fix, not suppress" vs. rules that may not fit.** If an unavoidable violation exists (for example the Boot `main` class, or a record that must be public), the policy of no blanket ignores and the policy of a green PR can clash. The spec resolves it with a narrow, named, commented exclusion inside the rule, and requires the PR to list each.
- **C4 Intent 018 ordering.** If intent 018 lands first, test code must be formatted (spring-javaformat), pass Checkstyle and be null-marked (`@NullMarked` in `package-info.java` for the new `architecture` package; fixtures that deliberately break rules must still compile under Error Prone/NullAway). If 019 lands first, 018 must format the new files. Either order works; the PR rebases onto whichever is on `main`.
- **C5 Test-scope dependency vs. the `@AnalyzeClasses` scan root.** Rules that import test classes (hygiene) depend on the compiled test output, so they only run in `test`/`verify`, never in `compile`. Developers get the failure at test time, not at compile time. Accepted.
- **C6 Rule drift in ArchUnit versions.** Renovate bumps can change which classes are matched (new API in the library or a stricter default). The self tests (3.6) catch a rule that silently stops working.

## 6. Assumptions
- **A1** Intent 018 merges first, as the intent assumes. If it has not, the PR targets `main` as is and later conflicts are resolved by whichever lands second.
- **A2** Rules run as ordinary JUnit tests in Surefire (`verify`), not as a separate plugin or Maven profile; hygiene and layering are separate test classes.
- **A3** The Spring Boot `Objects.requireNonNull` → `Assert` rule is not adopted. Null checking belongs to NullAway from intent 018, and an `Assert`-based rule would conflict with NullAway's recognition of `Objects.requireNonNull`.
- **A4** `allowEmptyShould(true)` is permitted only for H5 and H6, which have no matching code today, with an explanatory comment.
- **A5** H7 (no `System.out`/`printStackTrace`) is added beyond the intent's examples; it comes from the same family as Spring's rules and does not conflict with anything. It can be dropped without affecting the rest.
- **A6** `*View`, `*Form`, `Owners` and `Vets` are treated as the presentation layer by name because they carry no stereotype annotation. The list lives in one place in `ArchitectureRules`.
- **A7** Field injection is banned in main code only; `@Autowired` fields in test classes (e.g. `VetRepositoryTests`) remain, because Spring test conventions use them.
- **A8** The rule self tests (3.6) are included, in addition to the manual scratch-branch check the intent asks for, so the evidence is repeatable and survives ArchUnit upgrades.
- **A9** The `/spec` skill is not changed; `CLAUDE.md` is enough for agents.
- **A10** "Reasonable build time" means roughly 10 seconds added locally; no parallel execution or caching work is needed.
- **A11** ArchUnit's exact version is chosen in the plan (latest release that reads Java 27 class files); it is not named here because it cannot be verified from this repository.
- **A12** No CI workflow changes: `build.yml` already runs `verify`, and Renovate already covers `pom.xml` properties.

## 7. Out of scope
Auto-configuration and `@ConfigurationProperties` rules, non-Java file rules, repackaging, new features, branch-protection changes, replacing Checkstyle or Error Prone checks with ArchUnit equivalents.
