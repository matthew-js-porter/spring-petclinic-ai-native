# Spec: Spring PetClinic starter project
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Add a minimal Spring Boot application at the repo root that builds, starts, and exposes only `/actuator/health`. It is the baseline all later PetClinic features land in. No domain features, persistence, UI, security, or custom endpoints.

## 2. Existing codebase context
The repo has no application code. It contains only `.claude/` (settings, `intent` and `spec` skills), `intent/`, and a `.gitignore` that lists only `.idea`. So there is nothing to integrate with or regress; the work is purely additive at the repo root.

## 3. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | The project builds with `./mvnw clean verify` without errors. |
| F2 | The application starts without errors (`./mvnw spring-boot:run` or `java -jar`). |
| F3 | `GET /actuator/health` returns HTTP 200 with `status` equal to `UP`. Boot 4.1 also adds a `groups` array (`liveness`, `readiness`) to the body; that is accepted. |
| F4 | No other actuator endpoint is exposed over HTTP. |
| F5 | No custom controllers, entities, views, or static content. |

### Non-functional / constraints (from intent)
- Maven build, with the Maven wrapper.
- Dependencies limited to `spring-boot-starter-webmvc` (web MVC; the artifact name may be `spring-boot-starter-web` depending on Boot version) and `spring-boot-starter-actuator`, plus the default `spring-boot-starter-test` that start.spring.io adds.
- Spring Boot: the latest stable release, resolved to a concrete version number at generation time and pinned in `pom.xml` (never a `LATEST`/range specifier).
- Java: Java 25 (LTS), pinned as `java.version` 25. **Deviation:** the intent asks for the latest Java the Boot release supports (Boot 4.1.1 supports 27), and this spec originally asked for the latest non-LTS. JDK 27 could not be installed through SDKMAN (its download broker returns 404 for every 27 build), so 25 was chosen. Revisit once JDK 27 is installable.
- JDK is managed with SDKMAN: `.sdkmanrc` at the repo root pins `java=25.0.2-open` (Java.net build).
- Configuration is YAML (`application.yaml`), not `.properties`.
- Artifact `spring-petclinic` (Maven `<name>` is `PetClinic`, which gives the `PetClinicApplication` class), generated at the repo root.
- Generated with the `spring-tools:create-spring-boot-project` skill (start.spring.io).

## 4. Design

### 4.1 Layout
```
/pom.xml, mvnw, mvnw.cmd, .mvn/, .sdkmanrc
/src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java
/src/main/resources/application.yaml
/src/test/java/org/springframework/samples/petclinic/PetClinicApplicationTests.java
```
Group ID `org.springframework.samples.petclinic`, artifact `spring-petclinic`, base package `org.springframework.samples.petclinic`. Set the package name explicitly when generating, since start.spring.io would otherwise derive it from the group and artifact (`...petclinic.spring_petclinic`).

### 4.2 Configuration
Take the Spring Boot defaults. `application.yaml` contains only what the generator emits (`spring.application.name: PetClinic`). Do not add exposure or health settings; Boot's default exposes only `health` over HTTP.

### 4.3 Generation steps
1. Generate the project at the repo root with the `spring-tools:create-spring-boot-project` skill, requesting Spring Web and Actuator, with the latest Boot release resolved to a concrete version and Java 25 (see constraints). The generated `.gitignore` may overwrite the existing one.
2. Convert the generated `application.properties` to `application.yaml` (delete the `.properties` file). Do not overwrite `.claude/` or `intent/`.
3. Verify per section 5.

### 4.4 Out of scope
Domain features, persistence/datasource, UI/templates, Spring Security, custom endpoints, other actuator endpoints, Docker/CI.

## 5. Acceptance and verification
- `./mvnw clean verify` succeeds.
- A test (`@SpringBootTest` with `MockMvc` or a random-port web environment) asserts `/actuator/health` returns 200 and `status` is `UP`. `contextLoads` alone is not sufficient.
- A test asserts a non-health actuator path, e.g. `/actuator/info` and `/actuator/env`, returns 404.
- Manual smoke: start the app and `curl -i localhost:8080/actuator/health` gives `200` and a body with `"status":"UP"`. Run via the `run` skill.
- Run the `spring-tools:validate` skill and confirm no diagnostics.

## 6. Policy conformance
I looked for brand-guideline, security-policy, and UX-standard skills or documents to apply. None exist in this repo or in the available skills (only `intent`, `spec`, and Spring tooling). So no formal policy could be checked. In place of that, I applied these general-purpose defaults, which a policy owner should confirm:
- **Security:** minimal attack surface (health only), no secrets, no authentication added because the intent puts security out of scope, dependencies limited to official Spring Boot starters.
- **Brand / UX:** not applicable. The app has no UI, and the only user-facing output is the JSON health response in Boot's standard format.

## 7. Handoff notes for engineering
Small, single-PR change. Estimated effort: under an hour. Record the Boot and Java versions used in the PR description.
