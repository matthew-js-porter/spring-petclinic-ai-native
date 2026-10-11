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

## Skill evals

The pipeline's skills (`/intent`, `/spec`, `/maintain-scan` in [`.claude/skills/`](.claude/skills)) each have eval cases in `evals/evals.json`, run with Anthropic's [skill-creator](https://github.com/anthropics/claude-plugins-official/tree/main/plugins/skill-creator) plugin. CI runs them on PRs that change a skill, comments the results, and fails the check when the PR's version scores lower than `main`'s on any eval.

To run them locally:

1. Start Claude Code in the repo root. skill-creator is enabled in `.claude/settings.json`; accept the install prompt the first time (or run `/plugin install skill-creator@claude-plugins-official`).
2. Evaluate a skill:

   ```text
   /skill-creator eval <skill>
   ```

   Replace `spec` with `intent` or `maintain-scan`. To measure an edit, ask for a comparison with the version on `main`, for example `/skill-creator eval spec, comparing against the skill on origin/main`.
3. skill-creator runs each eval in a fresh subagent, grades every expectation, and opens a review page with the outputs and a benchmark (pass rate, time, tokens). Results are written to `.claude/skills/<skill>-workspace/`, which is gitignored.

When a skill misses something a human later catches, add a case to that skill's `evals/evals.json`.
