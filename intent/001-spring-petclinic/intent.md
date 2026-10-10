# Intent: Spring PetClinic starter project
Author: matthew-js-porter
Status: accepted

## Problem
There is no application in this repo yet. We are building Spring PetClinic AI-natively, and every later feature needs a running, buildable Spring Boot project to land in. Without a minimal, known-good baseline, later specs and plans have nothing to build on or verify against.

## Proposed outcome
A new Spring Boot project exists in the repo, created with the `/spring-tools:create-spring-boot-project` skill, using the web MVC starter and the actuator starter. "All the app should do for now is expose the actuator /health endpoint."

Observable results:
- The project builds and starts without errors.
- A request to the actuator health endpoint (`/actuator/health`) returns HTTP 200 with status `UP`.

Out of scope: any PetClinic domain features (owners, pets, vets, visits), persistence, UI, security, and custom endpoints. No other actuator endpoints need to be exposed.

## Affected users and systems
- Developers and agents working on this repo (new baseline to build on).
- The repo root gains a new Spring Boot project; no existing code is affected (the repo currently contains no application code).

## Constraints
- Starters: web MVC and actuator only (as requested).
- Build tool: Maven.
- Versions: latest Spring Boot and latest Java supported by it.
- Name: `spring-petclinic`.
- Location: the project lives at the repo root.
- The health endpoint is the standard `/actuator/health`.
- Generated via the `spring-tools:create-spring-boot-project` skill (start.spring.io).
- Keep the exposed surface minimal: health only.

## Open questions
- Group ID and base package were not specified beyond the name `spring-petclinic`. Suggested default: `org.springframework.samples.petclinic`. Owner: Matthew.
