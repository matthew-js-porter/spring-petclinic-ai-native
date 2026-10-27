# Intent: Add ArchUnit architecture rules
Author: matthew-js-porter

## Problem
"Add ArchUnit with standard arch rules for spring boot apps." Nothing in the build checks how the code is structured. Layering (controllers, repositories, entities, forms, views), the split between the `owner` and `vet` features, and Spring coding conventions are kept only by habit and review. Agent-written changes can drift from them, for example with a package cycle, a controller that skips a layer, field injection, or `toUpperCase()` with no `Locale`, and nothing fails until a reviewer notices, if one does. The Spring Framework and Spring Boot repos run architecture checks for this reason, and their rules are a proven place to start.

## Proposed outcome
- Architecture rules run as part of `./mvnw clean verify`, locally and in CI, and a violation fails the build with a message that names the offending class and says what to do instead.
- **Hygiene rules**, taken from the Spring Framework and Spring Boot `ArchitectureRules` where they fit an application rather than a framework. Examples: no package cycles; `String.toUpperCase`/`toLowerCase` only with a `Locale`; `Stream.toList()` over `Collectors.toList()`; `URLEncoder`/`URLDecoder` with a `Charset`; `BeanPostProcessor`/`BeanFactoryPostProcessor` `@Bean` methods are static; `@Bean` methods don't return private types. Rules about auto-configuration, `@ConfigurationProperties` and reactive testing are left out unless the app gains those things. These rules check main and test code.
- **Layering rules** for the app's own architecture, in main code. Examples: the web layer may use repositories and the domain, but the domain and repositories don't depend on the web layer; features (`owner`, `vet`) don't depend on each other's internals; no field injection; Spring stereotypes (controllers, repositories) sit where the conventions say and follow their naming.
- The existing code passes every rule in the same PR: existing violations are fixed rather than frozen or suppressed, and user-visible behaviour does not change.
- `CLAUDE.md` describes the rules and how to run them, so agents produce code that passes first time. `REVIEW.md` flags any change that weakens a rule or adds a suppression.
- A deliberate violation added to the code on a scratch branch fails the build, which shows the rules are actually wired in.

Out of scope: the auto-configuration and `@ConfigurationProperties` rules (the app has neither); rules for non-Java files; reorganising packages beyond what fixing a violation needs; new PetClinic features; branch protection changes.

## Affected users and systems
- Developers and agents writing Java here, including the Claude fix workflows (`claude.yml`, `dependency-updates.yml`), whose pushes must pass the rules.
- The Maven build and `build.yml` in CI.
- All Java sources under `src/main` and `src/test` (`owner`, `vet`, the root package).
- `CLAUDE.md`, `REVIEW.md`, and possibly the `/spec` skill if specs should mention the rules.

## Constraints
- Java 27 (pinned in `pom.xml` and `.sdkmanrc`) and Spring Boot 4.1. ArchUnit must read Java 27 class files.
- ArchUnit is not managed by Spring Boot, so the spec must give a reason and a version for it, and Renovate should keep it updated.
- Any exclusion or suppression must be narrow and carry a reason. Blanket ignores just to get the build green are not allowed.
- Must fit alongside the checks from intent 018 (spring-javaformat, Checkstyle, Error Prone/NullAway): rule code is formatted and null-checked like any other code.
- Build time should stay reasonable for local runs and CI.
- No pushes to `main`; everything lands through a branch and PR.

## Open questions
- Exactly which layering and naming rules make up the set, and how they read the `owner`/`vet` packages (the web, view and form types share a package with the entities)? Owner: spec.
- Should the rules run as ordinary JUnit tests or in a separate build step, and should hygiene and layering rules live in separate test classes? Owner: spec.
- Spring Boot's `Objects.requireNonNull` → `Assert` rule: adopt it, or leave null checks to NullAway from intent 018? Owner: Matthew / spec.
- Should intent 018 merge first, so the rules are written against formatted, null-marked code? Assumed yes. Owner: Matthew.
