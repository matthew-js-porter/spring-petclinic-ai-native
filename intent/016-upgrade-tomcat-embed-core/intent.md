# Intent: Remove the critical Tomcat vulnerabilities from the shipped dependencies
Author: claude[bot]

## Problem
Snyk Open Source reports two critical vulnerabilities in `org.apache.tomcat.embed:tomcat-embed-core@11.0.26`, the embedded server that runs the application. Source: [Snyk run 40292672290](https://github.com/matthew-js-porter/spring-petclinic-ai-native/actions/runs/40292672290) on `main` (scan of 2026-10-26).

- `SNYK-JAVA-ORGAPACHETOMCATEMBED-21707475`, critical (CVSS 9.3), CVE-2026-101296, CWE-304: "Missing Critical Step in Authentication vulnerability in org.apache.tomcat.embed:tomcat-embed-core". Affects the `JNDIRealm` authentication process when configured with a non-GSSAPI SASL mechanism; an attacker can authenticate without the correct password.
- `SNYK-JAVA-ORGAPACHETOMCATEMBED-21707477`, critical (CVSS 9.9), CVE-2026-101356, CWE-284: "Access Control Bypass vulnerability in org.apache.tomcat.embed:tomcat-embed-core". Affects HTTP/2 asynchronous servlet processing; requests and responses can be mixed up between users through incorrect object recycling.

Both are fixed in Tomcat 11.0.27 or higher. Snyk's suggested path is `org.springframework.boot:spring-boot-starter-webmvc@4.1.2` (currently 4.1.1). The version is introduced through `spring-boot-starter-webmvc` > `spring-boot-starter-tomcat` > `spring-boot-starter-tomcat-runtime`, and `pom.xml` pins it explicitly with `tomcat.version` set to `11.0.26`.

Validation against the code: the dependency is on the runtime classpath. The application sets no realm and does not enable HTTP/2, so exploitability in the current configuration looks low (confidence that it is exploitable here: low to medium). The package is still a critical finding that fails the `snyk.yml` gate (high and above) and would become reachable if either feature were enabled later.

## Proposed outcome
- A Snyk Open Source scan of `main` no longer reports `SNYK-JAVA-ORGAPACHETOMCATEMBED-21707475` or `SNYK-JAVA-ORGAPACHETOMCATEMBED-21707477`.
- The resolved `tomcat-embed-core` version is 11.0.27 or higher.
- The build and all tests still pass and the application starts and serves its pages as before.

Out of scope: enabling or changing realms or HTTP/2, and any other dependency not named in the findings.

## Affected users and systems
Everyone using the running application (the embedded Tomcat serves all requests), the maintainers, and the `snyk.yml` workflow. Touches the dependency versions in `pom.xml`.

## Constraints
- Favour versions managed by Spring Boot; a non-Boot version is allowed only with a stated reason (see `CLAUDE.md`).
- Java 25 and the existing build must keep working.
- Changes go through a branch and PR, never directly to `main`.

## Open questions
- Is the explicit `tomcat.version` pin still needed, or can the version come from Spring Boot? Related to the override clean-up in intent 014. Owner: maintainers.
- Does Spring Boot 4.1.2 (Snyk's suggested fix) ship Tomcat 11.0.27 or later, and is it available? Owner: maintainers.
