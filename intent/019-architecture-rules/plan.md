# Plan: intent 019 — ArchUnit architecture rules

## Context
Nothing in the build checks code structure; layering, the `owner`/`vet` split and Spring conventions rely on review. `intent/019-architecture-rules/spec.md` asks for ArchUnit hygiene + layering rules running as plain JUnit tests in `./mvnw clean verify`, existing violations fixed (not suppressed), and `CLAUDE.md`/`REVIEW.md` updated. Intent 018's checks are not in `pom.xml` yet (A1), so new code just follows the existing style (tabs, Spring-style headers as in the current tests).

Findings from the scan:
- Latest ArchUnit on Central is **1.5.1**. Local JDK default is 25; build needs `sdk env` (Temurin 27).
- No cross-feature imports, no field injection, no `toUpperCase()`/`Collectors.toList()`/`System.out` in `src/main`. Controllers are package-private. Forms reference controllers only in Javadoc `{@link}` (no bytecode dependency). Domain records don't reference views/forms.
- `PetClinicApplication` has one `@Bean Clock clock()` → H6 has a match; only **H5** is empty and needs `allowEmptyShould(true)` (spec says H5+H6; record this deviation in spec).
- `IsoDates` belongs to no layer; `consideringOnlyDependenciesInLayers()` ignores it.

## Steps

### 1. Go/no-go: ArchUnit reads Java 27 (spec C1)
- `sdk env`; add `archunit-junit5` `${archunit.version}` = `1.5.1`, test scope.
- Write a first test `ImportedClassesTests` (kept permanently): imports `org.springframework.samples.petclinic` and asserts known classes (`OwnerController`, `Vet`, `OwnerRepositoryTests`) are present and the main-class count is > 30. Reason: ArchUnit's importer logs and **skips** classes its shaded ASM can't read, so rules would silently pass on nothing.
- Run `./mvnw test -Dtest=ImportedClassesTests`; also check the log for "Couldn't import" warnings. If 1.5.1 cannot read Java 27 classes: stop, revert, report (no snapshot, no lowered release level).
- Check engine clash with Boot's JUnit BOM (Jupiter 6 / Platform 6): if the ArchUnit engine isn't discovered or errors, switch to `archunit` core and plain `@Test` methods calling `rule.check(classes)` (spec 3.1 fallback).

### 2. Rule code — `src/test/java/org/springframework/samples/petclinic/architecture/`
- `ArchitectureRules` (package-private final): layer predicates (web = `@Controller`/`@RestController`/`@ControllerAdvice`; repository = assignable to `org.springframework.data.repository.Repository`; domain = `@Table`; presentation = simple name ends `View`/`Form` or is `Owners`/`Vets`, list in one constant), an `ImportOption` excluding `..petclinic.architecture..`, and one `ArchRule` factory per rule, each with `.as(...)` and `.because(...)`.
- `HygieneRulesTests` — `@AnalyzeClasses(packages = "org.springframework.samples.petclinic", importOptions = ExcludeArchitecturePackage.class)`; `@ArchTest` H1–H8:
  - H1 `slices().matching("org.springframework.samples.petclinic.(*)..").should().beFreeOfCycles()` plus a root↔feature cycle check (root-package classes in their own slice via `SliceAssignment`).
  - H2–H4, H7 `noClasses().should().callMethod(...)` for `String.toUpperCase()`/`toLowerCase()`, `Collectors.toList()`, `URLEncoder.encode(String)`/`URLDecoder.decode(String)`, `printStackTrace()`, and `accessField(System.class,"out"/"err")`.
  - H5 static BPP/BFPP `@Bean` methods, `allowEmptyShould(true)` with a one-line comment (guards future config classes).
  - H6 `@Bean` methods' raw return type is not private.
  - **H8 (added at review) every package is `@NullMarked`:** custom `ArchCondition` over the imported `JavaPackage`s that contain classes: `tryGetPackageInfo()` is present and annotated `org.jspecify.annotations.NullMarked` (RUNTIME retention, so ArchUnit sees it). Message: "add package-info.java with @NullMarked". JSpecify comes transitively from `spring-core` (Spring Framework 7), so no new dependency; confirm with `dependency:tree -Dincludes=org.jspecify` and declare nothing unless it is missing from test/compile scope.
  - Fix for H8: add `package-info.java` with `@NullMarked` to `petclinic`, `petclinic.owner`, `petclinic.vet` in `src/main` (test classes in those packages share it; no duplicate `package-info` in `src/test`). Add one to the test-only `architecture` package too. The self-test fixture for H8 is an unmarked sub-package `architecture.fixtures.unmarked` (excluded from the production import, so it does not trip the real rule). No NullAway here, so the annotation changes no behaviour; intent 018 later enforces it.
- `LayeringRulesTests` — same plus `ImportOption.DoNotIncludeTests`; `@ArchTest` L1–L7 as in spec 3.4 (L1 `layeredArchitecture().consideringOnlyDependenciesInLayers()` with role-based `layer(...).definedBy(predicate)`; L2 domain/repository not depending on web; L3 `owner`↔`vet` `notDependOnEachOther()` and nothing depends on `PetClinicApplication`; L4 no `@Autowired`/`@Inject`/`@Resource` fields (check `jakarta.inject`/`jakarta.annotation` on classpath; use name-based `annotatedWith(String)` so absent types don't break compile); L5 naming/stereotype rules; L6 `@Controller` not public; L7 domain/repository don't depend on `jakarta.servlet..`/`org.springframework.web..`).
- `ArchitectureRulesSelfTests` (spec 3.6/A8) + `architecture/fixtures/` package: per rule a minimal violating fixture; `ArchitectureRules.<rule>().evaluate(new ClassFileImporter().importClasses(...)).hasViolation()` is asserted true, and a compliant fixture asserts false. Fixtures excluded from the production import by the architecture-package `ImportOption`.

### 3. Run and fix violations (spec 3.5)
- `./mvnw test -Dtest='HygieneRulesTests,LayeringRulesTests,ArchitectureRulesSelfTests'`. Expected violations are few (possible: test code `toLowerCase()`, root/feature slice details, public repository/record visibility only matters for L6 which targets controllers). Fix at source with smallest change; if a rule would force a repackage, rewrite it role-based. Any unavoidable exclusion lives inside the rule, names the class, one-line reason above. Record every violation + fix for the PR body.
- No `@ArchIgnore`, freezing, ignore-pattern files.

### 4. Scratch-branch evidence (spec 3.6)
- Temporary local commits (never pushed): `"x".toUpperCase()` in a controller, an `@Autowired` field, an `owner` class importing `vet.Vet`, a repository referencing a controller. Run `./mvnw clean verify` each, capture failing test + message, then drop the commits (`git reset --hard` to the pre-scratch SHA only on that scratch branch).

### 5. Docs
- `CLAUDE.md`: "Architecture rules" subsection (location, `./mvnw test -Dtest='HygieneRulesTests,LayeringRulesTests'`, one line per family, layering vocabulary, fix-don't-suppress); note in CI section that `build.yml` runs them.
- `REVIEW.md`: `[major]` conformance bullet for weakening/deleting rules or adding `@ArchIgnore`, ignore files, `FreezingArchRule`, `allowEmptyShould(true)` or unexplained exclusions; note ArchUnit's `<version>` is justified by spec 019.
- `intent/019-architecture-rules/spec.md`: add H8 to the 3.3 table and the three `package-info.java` files to 3.5; note deviations (H6 not empty; chosen version 1.5.1; any engine fallback). Save this plan as `intent/019-architecture-rules/plan.md`.

### 6. Verify and ship
- `./mvnw clean verify` on Temurin 27; Surefire reports list the three architecture classes; time the added tests (< ~10 s).
- `./mvnw dependency:tree -Dincludes=com.tngtech.archunit` → test scope only.
- `grep -rn 'ArchIgnore\|Freezing\|allowEmptyShould' src/test` → only H5.
- `spring-tools:validate`. Commit on `claude/architecture-rules-809d98`, open PR to `main` with violation list and scratch evidence.

## Critical files
- `pom.xml`
- `src/test/java/org/springframework/samples/petclinic/architecture/{ArchitectureRules,HygieneRulesTests,LayeringRulesTests,ArchitectureRulesSelfTests,ImportedClassesTests}.java`, `architecture/fixtures/*.java`
- `src/main/java/org/springframework/samples/petclinic/{,owner/,vet/}package-info.java`
- `CLAUDE.md`, `REVIEW.md`, `intent/019-architecture-rules/{spec.md,plan.md}`
