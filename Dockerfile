# syntax=docker/dockerfile:1
#
# Approved Dockerfile shape for every iPIE microservice (master standards doc, section 13).
# This service is its own repository as of 2026-08-09, so the build context IS this directory:
#
#   docker build -t ipie-user-service .
#
# It was a Gradle module until then, which is why this file used to require the monorepo root as
# context and a module-qualified :ipie-user-service:bootJar task.

# ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Whole repository - the Gradle wrapper, settings.gradle, build.gradle, gradle.properties and
# lombok.config are all required, and .dockerignore already excludes build/.gradle/.git, so this
# stays a lean context. lombok.config in particular is easy to miss: without it Lombok drops
# @Value from generated constructor parameters and the service fails to start at runtime, not
# at build time.
COPY . .

# Tests and static analysis already ran in CI before this image is built (master standards doc,
# section 13: compile -> test -> style/analysis -> ... -> build image) - skip them here so the
# image build stays fast and repeatable.
#
# --mount=type=cache,target=/root/.gradle - a BuildKit-managed cache, not a bind mount to a
# specific host path (portable across machines/CI, not just this one) - so the Gradle wrapper
# distribution and dependency jars survive across builds instead of being re-downloaded from
# scratch every time. Found the hard way: a cold download of the ~150MB Gradle distribution over
# this build sandbox's network was flaky enough (connect timeouts, mid-download read timeouts) to
# fail more often than it succeeded before this was added.
#
# sharing=locked - every sibling service's Dockerfile mounts this same cache target, and
# `docker compose up --build` builds them in parallel by default. Without locked sharing, two
# Gradle daemons writing to the shared /root/.gradle/caches/journal-1 at once hit "Timeout waiting
# to lock journal cache" and the build fails outright; locked sharing just queues them instead.
# The platform (ipie-parent, ipie-common-libs, ipie-build-conventions) resolves as published
# artifacts, and this build runs in an isolated container that can reach neither the host's Maven
# Local nor a private GitHub Packages repository without credentials. Two supported paths:
#
#   local  - docker build --build-context m2=$HOME/.m2/repository -t <name> .
#            The bind mount below makes the host's Maven Local visible, so a platform build that
#            has not been published yet still works.
#   CI     - publish the platform first, then supply credentials:
#            docker build --secret id=gh_token --build-context m2=/dev/null ...
#            with ipie.packages.user/token reaching Gradle via the environment.
#
# The mount is optional at runtime: BuildKit only binds it when the m2 context is supplied.
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    --mount=type=bind,from=m2,target=/root/.m2/repository \
    chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test

# ---- Run stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

RUN addgroup --system ipie && adduser --system --ingroup ipie ipie
USER ipie

COPY --from=build /workspace/build/libs/ipie-user-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
