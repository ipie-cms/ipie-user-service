# ipie-user-service

Owns user identity, self-registration, organisations and pillar linking — the write side that publishes the domain events other services consume.

Extracted from the `ipie-platform-mca` monorepo. It builds against the platform as **published
artifacts** — there is no `project(':...')` dependency and no `includeBuild` anywhere here.

## Note on lombok.config

`lombok.config` at the repository root is **not optional**. It carries
`lombok.copyableAnnotations += …Value`, without which Lombok drops the `@Value("\${…}")`
annotations from the constructor `@RequiredArgsConstructor` generates for
`StakeholderLinkServiceImpl`, and the whole service fails to start with
`NoSuchBeanDefinitionException: No qualifying bean of type 'java.time.Duration'`.

In the monorepo this file sat at the root and every module inherited it silently. An extracted
repository inherits nothing, which is exactly how this service's extraction first failed.

## Platform dependency

One property in `gradle.properties` fixes the version:

    ipiePlatformVersion=0.1.0-SNAPSHOT

which pins `in.gov.ipie:ipie-parent` (the version BOM), `in.gov.ipie:ipie-common-libs` (shared
libraries, plus its test-fixtures variant carrying the ArchUnit rules and Testcontainers base
classes) and `in.gov.ipie:ipie-build-conventions` (the `ipie.*` convention plugins, which also
carry the Checkstyle and SpotBugs configuration).

Bumping it is a deliberate act — that is the trade the extraction buys. This service is no longer
dragged by every platform change, but it must choose when to take one. Automate the bump as a pull
request gated on this repository's own CI. **Never** a dynamic `1.+` or `-SNAPSHOT` range: builds
stop being reproducible, and one bad platform commit would reach every service unchecked.

The shared test fixtures are consumed with `testFixtures("in.gov.ipie:ipie-common-libs:…")`, not a
`:test-fixtures` classifier. A classifier fetches the jar but bypasses Gradle Module Metadata
variant selection, so Testcontainers and `spring-boot-starter-test` would not come with it and the
first integration test would fail with `NoClassDefFoundError`.

## Resolving the platform artifacts

They live in GitHub Packages under `ipie-cms/ipie-platform-mca`. That registry authenticates every
read, whether or not the repository is public, so reads need credentials:

- **Locally** — set `ipie.packages.user` and `ipie.packages.token` in `~/.gradle/gradle.properties`
  (never in this repository), or publish the platform to Maven Local, which is checked first.
- **In CI** — a workflow's default `GITHUB_TOKEN` is scoped to *its own* repository and cannot read
  a private package owned by another one. Grant that access explicitly in the package settings, or
  supply a PAT.

## Local stack

`docker-compose.yml` in `ipie-platform-mca` starts the infrastructure — Postgres, Keycloak,
RabbitMQ, Redis, Elasticsearch, MailHog and the observability stack. It no longer builds any
service. Start it there, then run this service against it.

## Standards

`MASTER_CODE_STANDARDS.md` is committed here so you can check your work without leaving the
repository. It is generated into `.docx` alongside; edit the `.md`, never the `.docx`.

## Build

    ./gradlew check      # tests, ArchUnit, Checkstyle, SpotBugs
    ./gradlew bootJar
