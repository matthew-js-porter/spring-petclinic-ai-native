# Plan: Upgrade to Java 27 (intent 016)

> **Note:** this is the plan as approved. During implementation the owner switched the vendor from Liberica to Temurin (`27.0.0.0+35-tem`, `distribution: temurin`); read "Liberica" below as Temurin. See spec §11 for this and the other deviations.

## Context
The project is on Java 25 only because SDKMAN couldn't install JDK 27 when intent 001 was specced. JDK 27 GA is now available, but Renovate's LTS-only filter means it will never be proposed. Local (Java.net) and CI (Temurin) also use different vendors, and neither is Liberica, which Spring Boot uses. Spec `intent/016-upgrade-java-27/spec.md` sets the target: Java 27 + Liberica everywhere, Renovate proposes every major, and `.sdkmanrc` gets updated on Java major PRs by a review-triggered `@claude` request (spec 3.6). Docs stop calling 25 the target.

Checked while planning: the SDKMAN API lists Liberica 27 GA as **`27.0.0.0+36-librca`** on both `darwinarm64` and `linuxx64`. The `-fx` variant is not the one we want. This settles spec 3.3; record it in the PR description.

## Branch
`feature/016-upgrade-java-27` from `main`. Save this plan as `intent/016-upgrade-java-27/plan.md`. Open one PR.

## Steps

### 1. Pre-flight
- `sdk selfupdate force`, `sdk list java | grep librca`: confirm `27.0.0.0+36-librca` is listed locally.
- Stop and go back to the owner if Liberica 27 GA is missing locally or from setup-java (spec C6/A10).

### 2. Java pins
- `pom.xml:30` → `<java.version>27</java.version>`.
- `.sdkmanrc` → `java=27.0.0.0+36-librca` (keep the two comment lines).
- `.github/workflows/build.yml`, `snyk.yml`, `dependency-updates.yml` (fix job): `distribution: liberica`, `java-version: 27`. Keep `@v6` and `cache: maven`, plus the snyk "same JDK as the build" comment.
- `sdk env install && sdk env && java -version` (should show Liberica 27), then `./mvnw clean verify`, then `./mvnw spring-boot:run` + `curl -i localhost:8080/actuator/health` → 200 UP.

### 3. Renovate (`renovate.json5`)
- `java` rule: delete `allowedVersions`. Description → "Java majors in pom.xml and setup-java move together; every major, LTS or not".
- `.sdkmanrc` comment: replace "A human sets it on Java major PRs" with a line saying the review asks `@claude` to set it on Java major PRs. No intent number in the comment.
- Validate: `npx --yes --package renovate -- renovate-config-validator --strict renovate.json5`.
- Lookup dry run (`LOG_LEVEL=debug npx renovate --platform=local --dry-run=lookup`) on a scratchpad copy pinned at 25: expect one `renovate/java` branch to the newest GA major, with no EA. Then run on the 27 copy: expect no java branch, or 28 if it has shipped.

### 4. `.sdkmanrc` review flow (spec 3.6)
- `REVIEW.md:14`: change "Java 25" to "Java 27".
- `REVIEW.md:18` rewrite. It keeps the same trigger and `[major]` finding on the changed Java line, and now covers `dependency-updates.yml` too. Drop "ask a human / do not suggest one". End with the exact request:
  `@claude update .sdkmanrc to the Liberica identifier for Java <new major> that sdk list java offers, touching no other line, commit as "Update .sdkmanrc for Java <new major>", push to this branch, and reply with the old and new identifier. Do not repeat this request in your reply.`
  Note that its wording is what lets `claude.yml` act on a bot comment, matching the Boot overrides rule. If no GA `-librca` build exists for the major, Claude changes nothing and says so.
- `.github/workflows/claude.yml` lines 16–17: add `|| contains(github.event.comment.body, '@claude update .sdkmanrc to the Liberica identifier')` inside each bot-gate parenthesis, next to the Boot phrase. Update the `allowed_bots` comment to mention both requests.
- **Gap the spec doesn't cover:** claude-code-action doesn't allow arbitrary Bash by default, so Claude on the runner can't run `sdk list java` or `curl` as configured. Minimal fix: add `--allowed-tools "Bash(curl -s https://api.sdkman.io/2/candidates/java/*)"` to the existing `claude_args` line. The request wording then implicitly relies on the SDKMAN API (`.../java/linuxx64/versions/list?installed=` and `darwinarm64`). Claude should pick an identifier listed on both, since 014 found identifiers can differ per platform. Record this as a small deviation in spec 3.6/A8. It's cheaper and more reliable than installing SDKMAN on the runner.
- `actionlint .github/workflows/*.yml`.

### 5. Docs
- `CLAUDE.md:18`: "Java 27 (Liberica), pinned in `pom.xml` and `.sdkmanrc` (`java=27.0.0.0+36-librca`)…". Drop the deviation sentence. Add "keep SDKMAN current (`sdk selfupdate force`); an outdated install hides new GA builds".
- `CLAUDE.md:41`: "on Liberica 27".
- `CLAUDE.md` Renovate paragraph: the `java` group proposes every major. Replace "set it by hand…" with the review/`@claude` flow.
- `README.md:17,22`: Java 27 / JDK 27 (Liberica).
- `intent/001-spring-petclinic/spec.md:26,27`: state 27/Liberica. Keep the original deviation as history, marked "resolved by intent 016". Line 47 stays as history.
- `intent/014-automated-dependency-updates/spec.md`: update the current-state lines (15, 80, 116, 119, 126, 156, 183, 196): LTS-only → every major, Temurin 25 → Liberica 27, human sets `.sdkmanrc` → review `@claude` flow. Add a short "superseded by 016" note to each.
- `intent/016-upgrade-java-27/spec.md`: record the resolved identifier, the CI-resolved Liberica patch, and the `--allowed-tools` deviation.
- Specs 003–011 and other `plan.md` files stay as they are (A4).

### 6. PR
Commit, push, then `gh pr create`. The description lists the identifier, the Liberica patch from the `build` log, and the verification results. The `CLAUDE.md` change triggers `evals.yml`, which is expected. The `claude.yml` change means Claude review jobs skip this PR (expected, A8).

## Verification
- A1–A3 locally (step 2).
- A4/A5: on the PR, `build` is green and `snyk` completes with `snyk-sarif` uploaded. Both setup-java logs show Liberica 27.x.
- A6: `grep -rn "temurin\|java-version: 25\|<java.version>25" .github pom.xml` is empty (except the snyk comment, if it names nothing). No "Java 25" as target in `CLAUDE.md`/`README.md`/`REVIEW.md`.
- A7 actionlint, A8 Renovate validator + dry run, A9 read `dependency-updates.yml`, A10 read the 001/014 specs.
- A11 (after merge, since `claude.yml` only takes effect from `main`): open a throwaway PR raising `java.version` and the setup-java pins to 28 (or a fake higher major), leaving `.sdkmanrc` unchanged. Expect the review to post the request, `claude.yml` to run, and either a commit to `.sdkmanrc` or a "no GA Liberica build" reply. Close the PR afterwards without merging.
