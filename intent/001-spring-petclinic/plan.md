# Plan: Spring PetClinic starter project (intent 001)

## Context
The repo has no application code (only `.claude/`, `intent/`, `.gitignore`). Per `intent/001-spring-petclinic/spec.md`, add a minimal Spring Boot app at the repo root that builds, starts, and exposes only `/actuator/health`. It is the baseline for later PetClinic features.

## Steps
1. **Resolve versions**: use spring-tools MCP (`getLatestBootVersionsFromMavenRepo` / `getReleases`) to get the latest stable Boot release, and its supported Java versions. Boot 4.1.1 supports Java 27, but JDK 27 is not installable via SDKMAN, so Java 25 (LTS) is pinned instead (deviation from spec; see spec constraints). Both are pinned as concrete values.
2. **Generate** with the `spring-tools:create-spring-boot-project` skill (start.spring.io) into the repo root:
   - groupId `org.springframework.samples.petclinic`, artifactId/name `spring-petclinic`, packageName `org.springframework.samples.petclinic` (set explicitly)
   - Maven, Java, YAML-config afterwards; dependencies: `web`, `actuator` (plus default test starter)
   - Extract without touching `.claude/` or `intent/`; the generated `.gitignore` may replace the existing one (keep the `.idea` entry).
3. **Convert config**: replace `src/main/resources/application.properties` with `application.yaml` containing only the generator's `spring.application.name: PetClinic`. Delete the `.properties` file. No exposure/health settings.
4. **Verify class names**: main class `PetClinicApplication` and test `PetClinicApplicationTests` in the base package (rename generated ones if the generator's names differ).
5. **Tests** (in `PetClinicApplicationTests` or a sibling test class, `@SpringBootTest` + `@AutoConfigureMockMvc`; the annotation package differs in Boot 4, so check the generated version):
   - `GET /actuator/health` -> 200, `$.status` = `UP`
   - `GET /actuator/info` and `/actuator/env` -> 404
   - Keep `contextLoads`.

6. **Add `.sdkmanrc`** at the repo root with `java=25.0.2-open`.

## Verification
- `./mvnw clean verify` passes.
- Run via the `run` skill: start the app, `curl -i localhost:8080/actuator/health` gives 200 and `{"status":"UP"}`; `/actuator/env` gives 404. Stop the app afterwards.
- Run `spring-tools:validate` and confirm no diagnostics.
- Confirm no `.properties` file, no custom controllers/entities/static content.

## Handoff
Record the Boot and Java versions used in the PR description. Do not commit unless asked.
