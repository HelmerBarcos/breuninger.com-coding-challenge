# syntax=docker/dockerfile:1

# ---- dependency cache ------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# ---- DEV target: full JDK + maven + remote debugger (used by docker compose)
FROM deps AS dev
COPY src ./src
EXPOSE 8080 5005
# devtools restarts the context when recompiled classes land in the mounted target/
CMD ["mvn", "-o", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]

# ---- build the layered jar -------------------------------------------------
FROM deps AS build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/homefeed-*.jar extract --layers --launcher --destination extracted

# ---- PROD target: distroless (no shell, no package manager, no JDK tooling),
# non-root (uid 65532), layered COPY order so code changes re-push only KBs ---
FROM gcr.io/distroless/java21-debian12:nonroot AS prod
WORKDIR /app
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
USER nonroot
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
