FROM eclipse-temurin:21-jre
WORKDIR /app

# Built with the repository root as the context, so the path is module-qualified:
# the jar is produced by ./gradlew :importer-es:bootJar
COPY importer-es/build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
