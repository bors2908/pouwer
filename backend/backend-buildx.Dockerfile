ARG BUILD_IMAGE=gradle:jdk25
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre
ARG KT_STRATUM_REPO=https://github.com/bors2908/KtStratum
ARG KT_STRATUM_REF=p2pool-specific

FROM ${BUILD_IMAGE} AS build
ARG KT_STRATUM_REPO
ARG KT_STRATUM_REF
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    ca-certificates \
  && rm -rf /var/lib/apt/lists/*
USER gradle
WORKDIR /workspace

RUN set -eux; \
    git clone --depth 1 --branch "${KT_STRATUM_REF}" "${KT_STRATUM_REPO}" /workspace/KtStratum; \
    cd /workspace/KtStratum; \
    gradle --no-daemon publishToMavenLocal -x test

WORKDIR /workspace/backend
COPY --chown=gradle:gradle . .

RUN gradle --no-daemon :challenge:bootJar -x test

RUN set -eux; \
    cd /workspace/backend/challenge/build/libs; \
    jarFile="$(ls -1 challenge-*.jar | grep -v -- '-plain\\.jar$' | head -n 1)"; \
    cp "${jarFile}" /workspace/app.jar

FROM ${RUNTIME_IMAGE} AS backend-image
WORKDIR /opt/app
COPY --from=build /workspace/app.jar /opt/app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]

FROM scratch AS compose
COPY docker-compose.backend.yml /docker-compose.yml
