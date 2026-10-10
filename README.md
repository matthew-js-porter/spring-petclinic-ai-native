# Spring PetClinic (AI-native)

A Spring PetClinic sample app built using the [Anthropic AI-native SDLC playbook](https://claude.com/resources/articles/the-ai-native-sdlc-playbook).

Each change starts as a written artifact and moves through the same flow before any code lands:

1. **Intent**: the problem and desired outcome (`intent.md`)
2. **Spec**: requirements, design, and acceptance criteria (`spec.md`)
3. **Plan**: the implementation steps (`plan.md`)
4. **Implementation and verification**

These artifacts live in [`intent/`](intent), one folder per change, so the reasoning behind the code stays in the repo.

## Stack

- Spring Boot
- Java 25
- Maven 

## Getting started

Prerequisites: JDK 25. With [SDKMAN](https://sdkman.io), run `sdk env install` in the repo root to use the version pinned in `.sdkmanrc`.

```bash
./mvnw clean verify          # build and test
./mvnw spring-boot:run       # start the app on :8080
```