# Production image (multi-arch base images: runs on x86 and ARM servers such as Oracle Ampere).
# Tests run in CI; the image build only packages.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/research-office-backend-*.jar app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app && mkdir -p /data/files && chown -R app /data
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app
ENV RO_FILES_DIR=/data/files \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
