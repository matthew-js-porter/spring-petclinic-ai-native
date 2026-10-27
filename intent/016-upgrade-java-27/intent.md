# Intent: Upgrade to Java 27
Author: matthew-js-porter

## Problem
PetClinic builds and runs on Java 25. Intent 001 asked for the latest Java the Spring Boot release supports, which is 27, but its spec fell back to 25 because SDKMAN could not install a JDK 27 at the time. That deviation was meant to be temporary ("Revisit once JDK 27 is installable"). JDK 27 has since shipped and an up-to-date SDKMAN offers GA builds from most vendors (an outdated SDKMAN install hides them), yet the project is still on 25.

Local development (Java.net `-open` via SDKMAN) and CI (Temurin via setup-java) also use different JDK vendors, and neither is the one Spring Boot itself builds on: BellSoft Liberica, used in Boot's own `.sdkmanrc` and CI and in the Paketo buildpacks behind `spring-boot:build-image`.

The dependency pipeline keeps it there. Renovate allows only LTS Java majors (25 and 29), so it will never propose 27, or 28 later. The project wants to stay on the latest JDK always, not just move between LTS releases.

## Proposed outcome
- `./mvnw clean verify` passes on Java 27, locally and in CI, and the app runs on 27 (`./mvnw spring-boot:run`, `/actuator/health` returns UP).
- Every place that pins Java (the Maven `java.version`, `.sdkmanrc`, the CI and Snyk workflows) names 27, and they agree with each other.
- Local development and CI both use Temurin 27 (same vendor in both places); `sdk env install` installs it on a fresh machine.
- Renovate proposes every new Java major as it ships (28, 29, …), LTS or not, still grouped and never auto-merged.
- The Java 25 deviation recorded in the 001 and 014 specs is resolved, and the docs (`CLAUDE.md`, specs) no longer describe 25 as the target.

Out of scope: adopting Java 27 language or API features in application code, and changing the Spring Boot version.

## Affected users and systems
- Developers: local JDK via SDKMAN (`.sdkmanrc`).
- Build: `pom.xml` Java version, the Maven wrapper build.
- CI: `build.yml`, `snyk.yml` (setup-java, staying on Temurin), the `dependency-updates.yml` fix loop and the review rule that flags Java major PRs.
- Renovate config (`renovate.json5`) and its `java` group.
- Docs: `CLAUDE.md`, `intent/001-spring-petclinic/spec.md`, `intent/014-automated-dependency-updates/spec.md`.

## Constraints
- Temurin everywhere (SDKMAN `-tem`, setup-java `distribution: temurin`), so local and CI run the same vendor's JDK. *Amended by the owner during implementation: originally Liberica everywhere, to match Spring Boot. Renovate's github-actions manager reads setup-java's `java-version` only for `temurin` and `adopt`, and its `java-version` datasource lists Adoptium (Temurin) releases, so Liberica needed a fragile regex manager and could get a major proposed before BellSoft shipped it. Matching Boot's vendor was judged not worth that.*
- Must be a GA build, not early access.
- Must stay within what the current Boot 4.1 line supports.
- Non-LTS majors lose updates when the next one ships (27 at about the time 28 arrives, ~March 2027), so staying current is a commitment, not a one-off.
- Renovate still does not manage `.sdkmanrc`; a human sets it on each Java major PR.
- All changes go through a branch and PR; no push to `main`.

## Open questions
- Do Snyk, Maven plugins and other build tooling work on 27 without changes? (Owner: spec/plan)
- SDKMAN's build (27.0.0) trails setup-java's (27.0.1). Is a patch-level gap between local and CI acceptable, or should CI pin to what SDKMAN offers? (Owner: matthew-js-porter)
