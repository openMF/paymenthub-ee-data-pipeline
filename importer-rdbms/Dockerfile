FROM eclipse-temurin:21-jre
# healthcheck endpoint served by Healthcheck.java
EXPOSE 5000

# Built with the repository root as the context, so the path is module-qualified:
# the jar is produced by ./gradlew :importer-rdbms:bootJar
COPY importer-rdbms/build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
