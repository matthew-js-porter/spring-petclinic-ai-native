# Spec: Upgrade to Java 27
Source intent: [intent.md](./intent.md)
Author: matthew-js-porter

## 1. Summary
Move the project from Java 25 to Java 27 and from two different JDK vendors (Java.net locally, Temurin in CI) to Liberica everywhere, and let Renovate propose every future Java major, LTS or not. The work has four parts:

1. **Java pins.** `pom.xml` `java.version`, `.sdkmanrc`, and `setup-java` in `build.yml`, `snyk.yml` and `dependency-updates.yml` all name 27; CI and local use Liberica.
2. **Renovate.** The `java` group drops its LTS-only `allowedVersions`, so 28, 29 and later each arrive as a grouped PR (never auto-merged).
3. **Review rule and `.sdkmanrc` automation.** `REVIEW.md` stops saying "Java 25", and its Java pins rule changes from "ask a human to set `.sdkmanrc`" to the Spring Boot overrides pattern: the reviewer posts a `@claude` request and `claude.yml` acts on it, so a Java major PR gets its `.sdkmanrc` updated on the PR branch (3.6).
4. **Docs.** `CLAUDE.md`, `README.md`, `REVIEW.md` and the 001 and 014 specs stop describing 25 as the target and record that the deviation is resolved.

Out of scope (from the intent): Java 27 language or API features in application code, and changing the Spring Boot version.

## 2. Existing codebase context
- `pom.xml`: `<java.version>25</java.version>` (line 30); parent `spring-boot-starter-parent` 4.1.2; Maven wrapper `3.10.0`.
- `.sdkmanrc`: `java=25.0.2-open`. Per the 014 spec this identifier is already withdrawn from SDKMAN, so `sdk env install` fails today.
- `.github/workflows/build.yml`, `snyk.yml` and `dependency-updates.yml` (fix job) use `actions/setup-java@v6` with `distribution: temurin`, `java-version: 25`. The intent lists `build.yml` and `snyk.yml`, but `dependency-updates.yml` also installs the JDK for the fix loop and must change too (see A1).
- `renovate.json5`: the `java` package rule matches `java` and `java-jdk`, with `allowedVersions: "/^(25|29)(\\.|$)/"` and a comment "LTS only". One regex custom manager reads `<java.version>` in `pom.xml` with the `java-version` datasource and `extractVersionTemplate` major-only. `.sdkmanrc` is deliberately unmanaged.
- `REVIEW.md` line 14 names "Java 25" as a `CLAUDE.md` rule example; line 18 is the Java pins / `.sdkmanrc` rule, which is version-agnostic and its requirement that a human picks the SDKMAN identifier.
- Docs naming 25 or Temurin: `CLAUDE.md` (Commands, the Java bullet, CI `build.yml` line), `README.md` (lines 17 and 22), `intent/001-spring-petclinic/spec.md` (lines 26, 27, 47), `intent/014-automated-dependency-updates/spec.md` (lines 15, 80, 116, 126, 156, 183, 196).
- Other specs (003–011) mention Java 25 or Temurin as context. They are historical records and are not edited (A4).
- The Maven build uses only Boot-managed plugins and the wrapper; no `maven-compiler-plugin` `release` override or toolchain config exists, so `java.version` alone sets the compile target.

## 3. Decisions on the open questions

### 3.1 Do Snyk, Maven plugins and build tooling work on 27?
Not knowable from the repository; it is verified by the build itself and is the first acceptance gate (F1, A1). Reasoning for expectation: Spring Boot 4.1 supports Java 27 (intent 001 spec), and the Boot-managed plugins follow Boot. Snyk runs through the Snyk CLI on the runner's JDK (`snyk.yml` already uses `setup-java` instead of the `snyk/actions/maven` image), so it uses whatever JDK and Maven the runner has. Fallback if a tool fails on 27: record it under section 8 and keep the pin at 27 only if a Boot-supported fix exists; otherwise stop and return to the owner (no silent downgrade).

### 3.2 Patch gap between SDKMAN (27.0.0) and setup-java (27.0.1): accept it, pin the major only
CI pins `java-version: 27` (major, which setup-java resolves to the latest Liberica 27.x GA), and `.sdkmanrc` pins the SDKMAN identifier for whatever Liberica 27 build SDKMAN offers. A patch-level gap between local and CI is accepted. Rationale: the Renovate pins are major-only by design (014 spec 5.2), the same major gives the same language and API level, and pinning CI to an exact patch would need a manual bump for every Liberica release (and would stop receiving security fixes automatically). This is the owner's question; the choice is an assumption (A2) and a concern (C3).

### 3.3 Liberica identifier format
SDKMAN's Liberica vendor suffix is `-librca`. The exact identifier (`27.0.0-librca` or a `27.0.0.fx`-style variant) is not guessed here: the plan must read it from `sdk list java` on a current SDKMAN (`sdk selfupdate force` first, since an outdated SDKMAN hides 27 GA builds) and use a GA, non-EA entry, then verify `sdk env install` on a clean machine or container (F4).

### 3.4 Renovate: remove the LTS restriction, keep the group
Delete `allowedVersions` so any new Java major (28, 29, …) is proposed. Keep `groupName: java`, `separateMajorMinor: false`, `automerge: false` (global). Update the rule `description` and drop "LTS only". Early-access builds must not be proposed: the `java-version` datasource lists Temurin GA releases, so EA is not expected; a check that the group never proposes an EA value is part of verification (A6, C4).

### 3.6 `.sdkmanrc` is updated through the review, like the Boot overrides
Renovate cannot edit `.sdkmanrc` (SDKMAN identifiers differ per platform and old builds are withdrawn). Instead, follow the Spring Boot overrides pattern in `REVIEW.md` and `claude.yml` (which ignores bot comments unless they contain a known `@claude` request):
1. A Renovate Java major PR changes `pom.xml` `java.version` and the `setup-java` pins, and `.sdkmanrc` still names the old major.
2. The `claude-code-review.yml` review flags this as a `[major]` finding on the changed Java line and ends the comment with one exact request, e.g. `@claude update .sdkmanrc to the Liberica identifier for Java <new major> that sdk list java offers, touching no other line, commit as "Update .sdkmanrc for Java <new major>", push to this branch, and reply with the old and new identifier. Do not repeat this request in your reply.`
3. `claude.yml` accepts the bot comment because it contains that request phrase: the bot-comment gate gets a second phrase next to the Boot one (minimal change to the upstream template).
4. Claude runs `sdk list java` (installing SDKMAN on the runner if needed), picks the GA Liberica (`-librca`) identifier for the new major, edits `.sdkmanrc`, pushes, and replies. If no GA Liberica identifier exists for that major yet, it changes nothing and says so, and a human decides.
5. The push re-triggers the review, which finds the pins consistent and posts nothing for this rule.

This replaces the earlier rule that no tool may suggest an identifier: the identifier now comes from `sdk list java` on the runner, not from a guess, and a human still reviews the commit before merge. `.sdkmanrc` stays outside Renovate's managers. The request wording lives in `REVIEW.md` and is mirrored in the `claude.yml` condition; changing one means changing the other.

### 3.5 Docs: history stays, the deviation is marked resolved
Edit the 001 and 014 specs minimally: the 001 deviation sentence becomes a statement that 27 is now in use (with a pointer to this spec); the 014 spec lines that say LTS-only or Java 25 get a one-line note and the lines that describe the current state are corrected. The intent says the docs "no longer describe 25 as the target", so current-state statements change, historical rationale is kept and annotated.

## 4. Requirements

### Functional
| ID | Requirement |
|----|-------------|
| F1 | `./mvnw -B clean verify` passes on Liberica 27, locally and in the `build` workflow. |
| F2 | The app starts on 27 (`./mvnw spring-boot:run`) and `GET /actuator/health` returns `{"status":"UP"}`. |
| F3 | `pom.xml` `java.version`, `.sdkmanrc`, and `java-version` in `build.yml`, `snyk.yml` and `dependency-updates.yml` all name major 27. |
| F4 | `.sdkmanrc` names a Liberica (`-librca`) GA identifier for 27 that SDKMAN offers, and `sdk env install` installs it on a fresh machine. |
| F5 | All three workflows use `distribution: liberica`. |
| F6 | `renovate.json5` proposes every new Java major (28, 29, …) in the `java` group, without an LTS filter; `automerge` stays false; `.sdkmanrc` stays unmanaged. |
| F9 | A Renovate Java major PR gets a review comment ending with the `@claude update .sdkmanrc` request, `claude.yml` acts on it, and the pushed commit sets `.sdkmanrc` to a GA Liberica identifier for the new major (3.6). |
| F7 | `CLAUDE.md`, `README.md` and `REVIEW.md` name Java 27 / Liberica and no longer call 25 the target or describe the deviation; the 001 and 014 specs record the deviation as resolved. |
| F8 | The `snyk` workflow still completes on 27 and its SARIF artifact is produced. |

### Non-functional / constraints
- GA builds only, no early access (intent constraint).
- Stay within what Boot 4.1 supports; no Boot version change.
- Config remains YAML for Spring; `renovate.json5` stays JSON5. No `application.properties`.
- Third-party actions stay on version tags (`actions/setup-java@v6`), not SHAs.
- Workflow edits are linted with `actionlint .github/workflows/*.yml`.
- All changes go through a branch and PR; nothing is pushed to `main`.
- `claude-code-review.yml` is not changed. `claude.yml` changes only to accept the `.sdkmanrc` request phrase (3.6, A8).

## 5. Design

### 5.1 Files changed
```
pom.xml                                   # java.version 27
.sdkmanrc                                 # java=<27 Liberica identifier from sdk list java>
.github/workflows/build.yml               # distribution: liberica, java-version: 27
.github/workflows/snyk.yml                # same
.github/workflows/dependency-updates.yml  # same (fix job)
renovate.json5                            # java rule: drop allowedVersions and LTS wording
CLAUDE.md                                 # Java bullet, CI build.yml line, Renovate paragraph
README.md                                 # lines 17 and 22
REVIEW.md                                 # line 14 example; Java pins rule becomes the @claude request (3.6)
.github/workflows/claude.yml              # accept the .sdkmanrc request phrase from bot comments
intent/001-spring-petclinic/spec.md       # deviation resolved
intent/014-automated-dependency-updates/spec.md  # LTS-only and Java 25 statements
```

### 5.2 Build and CI
- `pom.xml`: `<java.version>27</java.version>`; nothing else (the Boot parent maps it to the compiler release).
- Each `setup-java` step: `distribution: liberica`, `java-version: 27`. Keep the `@v6` tag. If Maven caching is configured, leave it as is.
- `snyk.yml` comment "same JDK as the build (C5)" still holds and is kept.
- Failure on 27 in any tool is handled per 3.1.

### 5.3 `.sdkmanrc`
`java=<identifier>` where the identifier is Liberica 27 GA from `sdk list java` (3.3). The comment lines at the top of the file stay. For later majors the file is updated by the review-triggered `@claude` flow (3.6); patch drift and identifiers withdrawn after the fact are not tracked (014 C9).

### 5.4 `renovate.json5`
The `java` rule becomes:
- `description`: "Java majors in pom.xml and setup-java move together; every major, LTS or not".
- `matchDepNames: ["java", "java-jdk"]`, `groupName: "java"`, `separateMajorMinor: false` unchanged; `allowedVersions` removed.
- File header comment and the `.sdkmanrc` comment are kept, the latter extended to say the review flow updates the file (3.6).
Validate with `renovate-config-validator --strict` and a `--platform=local --dry-run=lookup` run on a copy pinned at 25, expecting one `renovate/java` branch raising `pom.xml` and the `java-jdk` pins to the newest GA major and no EA values. Note this would currently show 27 or a later major, so also run it on a 27 copy to confirm it proposes nothing, or 28 when it exists.

### 5.5 `REVIEW.md`
Line 14: replace "Java 25" with "Java 27". Rewrite the Java pins rule (line 18): the finding stays `[major]` on the changed Java line, but instead of asking a human to pick an identifier it ends with the exact `@claude update .sdkmanrc ...` request from 3.6, as the Spring Boot overrides rule does. Add the second phrase to the bot-comment condition in `claude.yml` (the two `contains` checks), leaving the rest of that file as generated.

### 5.6 Documentation
- `CLAUDE.md`: Java bullet becomes "Java 27 (Liberica), pinned in `pom.xml` and `.sdkmanrc` (`java=<identifier>`). Run `sdk env install` / `sdk env` …"; remove the "not installable via SDKMAN … deliberate deviation" sentence. Add: keep SDKMAN current (`sdk selfupdate force`) because an outdated install hides new GA builds. CI line: "on Liberica 27". Add to the Renovate paragraph that the `java` group proposes every major.
- `README.md`: "Java 27" and "JDK 27 (Liberica)".
- 001 spec: the Java line and `.sdkmanrc` line state 27 / Liberica with a note that the original deviation is resolved by intent 016; the project-generation step keeps its history but names 27 where it describes the current state.
- 014 spec: correct lines 15, 80, 116, 126, 156, 196 to say every major is proposed and the fix job uses Liberica 27; add a "superseded by 016" note to the LTS-only decisions.
- Never mention "intent 016" in code, YAML, SQL or CSS comments (`CLAUDE.md`). The docs under `intent/` and `CLAUDE.md` prose may reference it.

## 6. Acceptance and verification
| ID | Check |
|----|-------|
| A1 | `sdk env install && sdk env` on a fresh machine with a current SDKMAN installs Liberica 27 GA; `java -version` shows 27 and the Liberica vendor. |
| A2 | `./mvnw clean verify` passes locally on that JDK. |
| A3 | `./mvnw spring-boot:run` starts; `curl -i localhost:8080/actuator/health` returns 200 and `UP`. |
| A4 | The PR's `build` check is green on Liberica 27 and its log shows the Liberica 27 distribution. |
| A5 | The PR's `snyk` check completes and uploads `snyk-sarif`. |
| A6 | `grep -rn "temurin\|java-version: 25\|<java.version>25" .github pom.xml` finds nothing; `grep` of `CLAUDE.md`, `README.md`, `REVIEW.md` finds no "Java 25" as target. |
| A7 | `actionlint .github/workflows/*.yml` passes. |
| A8 | `renovate-config-validator --strict` passes, and a lookup dry run proposes the newest Java major without an LTS filter and no EA build. |
| A9 | The `dependency-updates` fix job's JDK step uses Liberica 27 (checked by reading the workflow; it only runs on a failed Renovate PR). |
| A11 | On a test PR raising `java.version` and the `setup-java` pins to a newer major with `.sdkmanrc` unchanged, the review posts the request, `claude.yml` runs on the bot comment, and a commit updates `.sdkmanrc`; a second review run posts nothing for this rule. |
| A10 | The 001 and 014 specs no longer describe 25 as the current target. |

## 7. Policy conformance
- **CLAUDE.md:** Java pinned in `pom.xml` and `.sdkmanrc` and updated together; YAML-only Spring config untouched; actions on tags; no intent mentions in code or YAML comments; changes via branch and PR; `actionlint` run.
- **Design system / UX:** no UI change; not applicable.
- **Security (Snyk, REVIEW.md):** newer JDK with current security patches; CI keeps resolving the latest Liberica 27 patch. No new dependency, secret, permission or action added.
- **Boot-managed dependencies:** none added.
- **Workflow protection:** `claude-code-review.yml` untouched; `claude.yml` gains one accepted phrase (3.6). Note `build.yml`, `snyk.yml` and `dependency-updates.yml` are not Claude workflows and run on the PR normally.

## 8. Areas of concern

- **C1 — Non-LTS commitment.** 27 stops receiving updates when 28 ships (about March 2027). Staying on the latest JDK is now a standing duty: the Renovate PR for 28 must be handled promptly (set `.sdkmanrc`, confirm Boot supports 28). If Boot has not added 28 support by then, the Renovate PR will fail the build; the project then runs an unsupported JDK 27 until Boot catches up, or stays on 27 knowingly. No automation enforces this.
- **C2 — Renovate will propose Java versions Boot does not yet support.** Removing the LTS filter also removes a natural brake: 28 or 29 can be proposed before the Boot line supports them. The build is the guard (the PR is red) and `dependency-updates.yml` will try to fix it, which could waste both attempts on an unfixable platform incompatibility. Accepted, but a human should judge such PRs. A possible mitigation (a delay with `minimumReleaseAge` for the `java` group) is not added because the intent wants every major as it ships.
- **C3 — Patch gap local vs CI.** See 3.2. Local and CI may differ by a patch (27.0.0 vs 27.0.1). Rare behavioural differences are possible but the major is identical.
- **C4 — `.sdkmanrc` stays manual and fragile.** The SDKMAN identifier can disappear again. Intent 014 documented that the current one is already withdrawn. No pin-consistency check exists (014 owner decision, 5.4), so a Java major PR can still merge with `.sdkmanrc` stale, or a withdrawn identifier can break `sdk env install` for new machines without a signal. The review-triggered `@claude` flow (3.6) closes the common case (a Java major PR with a stale `.sdkmanrc`), but not an identifier withdrawn later with no PR, and it relies on Claude being able to run `sdk list java` on the runner. Renovate keeps not managing the file.
- **C5 — Vendor change risk.** Moving from Temurin to Liberica changes the build vendor in CI. Liberica is what Boot builds on, so compatibility is expected, but cached `setup-java` Maven caches are keyed independently of vendor, so a first run may be slower.
- **C6 — Unverified here.** Availability of Liberica 27 GA through both SDKMAN and `setup-java`, and Snyk's behaviour on 27, could not be checked while writing this spec (no network certainty, unattended). The plan must confirm them first; if either is missing, the work stops and goes back to the owner rather than falling back to another vendor or EA.
- **C7 — `dependency-updates.yml` omitted from the intent's CI list.** The intent names `build.yml` and `snyk.yml`, but the fix job also pins Temurin 25, so leaving it would fix Renovate PRs on a different JDK.

## 9. Assumptions
- **A1.** `dependency-updates.yml` is changed too, even though the intent lists only `build.yml` and `snyk.yml` explicitly, because it also sets up Temurin 25 (the intent's "the `dependency-updates.yml` fix loop" does list it as affected).
- **A2.** The patch-level gap (open question 2, owner matthew-js-porter) is acceptable: CI pins the major only and the local pin is whatever Liberica 27 SDKMAN offers. This is the most reasonable choice because every other Java pin is major-only and the owner can overturn it.
- **A3.** "Matches Spring Boot" means Liberica vendor only; no need to match Boot's exact patch version.
- **A4.** Specs 003–011 and the `plan.md` files are historical and are not edited; only the 001 and 014 specs the intent names are updated. `plan.md` files (001, 002, 003, 014) are left alone.
- **A5.** The 001 and 014 specs are edited in the same PR as the code, not a separate one.
- **A6.** Early-access builds are excluded by relying on the `java-version` datasource listing GA releases, plus a verification step; no explicit EA filter is added to the Renovate rule.
- **A7.** The `.sdkmanrc` identifier is chosen at implementation time from `sdk list java` and is not fixed in this spec. Later majors are set by the `@claude` flow (3.6), which reads `sdk list java` on the runner.
- **A8.** `REVIEW.md`'s Java pins rule is rewritten to the `@claude` request pattern (3.6), and the "Java 25" example string changes. `claude.yml` gets a second accepted bot phrase, a deliberate exception to leaving the Claude workflows alone; because Claude workflows skip PRs that change them, it takes effect only after merge.
- **A9.** No toolchain, `release` flag or compiler plugin settings are added; `java.version` alone drives the compile target.
- **A10.** If Snyk or a plugin fails on 27 and no Boot-supported fix exists, work stops and the owner decides (3.1), instead of reverting to 25.

## 10. Handoff notes for engineering
- Suggested order: (1) `sdk selfupdate force`, find the Liberica 27 GA identifier, confirm `setup-java` Liberica 27 resolves; (2) `pom.xml`, `.sdkmanrc` and the three workflows, then `./mvnw clean verify` locally; (3) `renovate.json5` and its validator run; (4) the `REVIEW.md` rule and `claude.yml` phrase (3.6), then docs; (5) open one PR, with the Liberica identifier and the resolved CI patch version recorded in the description.
- Single small PR, estimated under two hours, most of it verification.
- The PR will show `build` and `snyk` running against the changed workflows; check both logs for the Liberica 27 distribution.
- After merge, the Java 28 Renovate PR (about March 2027, C1) is handled by the 3.6 flow.
- The spec needs full human review before a plan is written.
