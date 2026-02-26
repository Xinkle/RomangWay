FROM gradle:8.14.0-jdk17 AS builder

WORKDIR /workspace
COPY . .

# FFLogs GraphQL introspection runs during Gradle build in this project.
# Pass build args (or CI secrets) so image build can generate the client code.
ARG FFLOGS_CLIENT_ID
ARG FFLOGS_CLIENT_SECRET
ENV FFLOGS_CLIENT_ID=${FFLOGS_CLIENT_ID}
ENV FFLOGS_CLIENT_SECRET=${FFLOGS_CLIENT_SECRET}

RUN test -n "$FFLOGS_CLIENT_ID" && test -n "$FFLOGS_CLIENT_SECRET"
RUN gradle --no-daemon shadowJar
RUN cp build/libs/*-all.jar /tmp/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /tmp/app.jar /app/app.jar
COPY docker/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh \
    && mkdir -p /config

# Optional mount path for secret_profile.properties (Unraid appdata recommended)
ENV SECRET_PROFILE_PATH=/config/secret_profile.properties

ENTRYPOINT ["/entrypoint.sh"]

