# Intent: Persistence foundation
Author: matthew-js-porter

## Problem
The PetClinic baseline (intent 001) is a web MVC + Actuator app with no database. Every PetClinic domain feature (owners, pets, vets, visits) needs somewhere to store data, and there is nothing to build on yet. We want a "persistence foundation" in place first, using an H2 database and Spring Boot's Data JDBC starter, so later domain intents only have to add their own tables and repositories.

## Proposed outcome
The application starts with an in-memory H2 database wired up through Spring Data JDBC, and the database's health counts toward the application's health.

Observable results:
- The project builds and all tests pass (`./mvnw clean verify`).
- The application starts without errors and connects to an in-memory H2 database; nothing is written to disk and each start begins with an empty database.
- A request to `/actuator/health` returns HTTP 200 with status `UP`. The database check feeds that overall status (if the database is unavailable, health is not `UP`), but the response shows only the status, as Spring Boot does by default.

Out of scope: any PetClinic domain tables, entities, repositories or seed data; schema migration tooling; enabling the H2 web console (the `spring-boot-h2console` module is on the classpath at reviewer request, but stays disabled; see spec 4.1); file-based or production databases (e.g. MySQL/Postgres); exposing any actuator endpoint other than `health`; showing health details or components.

## Affected users and systems
- Developers and agents working on this repo (gain a datasource to build domain features on).
- The application's build dependencies and configuration.
- The actuator health status (now also reflects the database).
- CI build (`build.yml`) and Snyk scans, which will see the new dependencies.

## Constraints
- Database: H2, in-memory.
- Persistence: Spring Boot Data JDBC starter (not JPA).
- Config stays YAML only (`application.yaml`).
- Actuator surface stays minimal: only `health` is exposed, and health details and components stay hidden (Spring Boot defaults, no `management` settings).
- Dependencies must pass the existing Snyk gate (no `high` or above findings).

## Open questions
- None at this stage.
