# Intent: Resolve high-severity vulnerabilities in Spring Framework and Spring Boot dependencies
Author: claude[bot]

## Problem
The weekly Snyk Open Source scan of `main` reports four high-severity vulnerabilities in dependencies that `pom.xml` brings in through Spring Boot 4.1.2 (`spring-boot-starter-parent`). The resolved versions are Spring Framework 7.0.10 and `spring-boot-actuator` 4.1.2, all on the compile classpath of the running application. Snyk run: https://github.com/matthew-js-porter/spring-petclinic-ai-native/actions/runs/45168151277

- `SNYK-JAVA-ORGSPRINGFRAMEWORK-22727152`, high: Improper Cleanup on Thrown Exception in `org.springframework:spring-core` (`pom.xml`).
- `SNYK-JAVA-ORGSPRINGFRAMEWORK-22727191`, high: Incorrect Behavior Order: Validate Before Canonicalize in `org.springframework:spring-beans` (`pom.xml`).
- `SNYK-JAVA-ORGSPRINGFRAMEWORK-22727208`, high: Allocation of Resources Without Limits or Throttling in `org.springframework:spring-web` (`pom.xml`).
- `SNYK-JAVA-ORGSPRINGFRAMEWORKBOOT-22729051`, high: Insecure Temporary File in `org.springframework.boot:spring-boot-actuator` (`pom.xml`).

The SARIF output carries no CVE identifiers or fixed versions; they must be looked up. Exploitability in this application has not been assessed: the web layer, bean binding and actuator are all in use.

## Proposed outcome
- A Snyk Open Source scan of `main` no longer reports these four findings.
- The fix keeps dependency versions managed by Spring Boot wherever a Boot release contains them, so the project does not carry version overrides without a stated reason.
- `./mvnw clean verify` and the application's existing behaviour are unchanged.

Out of scope: other dependencies and scan findings; application features.

## Affected users and systems
- Everyone running the application (web MVC, actuator, bean binding).
- `pom.xml` and the Renovate `spring-boot` update group.

## Constraints
- Favour Boot-managed versions; a non-Boot or overridden version needs a stated reason in the spec.
- Java 27 and the minimal actuator surface (`health` only) stay as they are.
- Changes go through a branch and PR.

## Open questions
- Which Spring Boot or Spring Framework release fixes each finding, and is it available yet? Owner: spec author.
- If no Boot release fixes them yet, is a temporary override acceptable? Owner: Matthew.
