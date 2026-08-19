FROM eclipse-temurin:21-jre
# 8080 actuator/management, 5000 Camel REST API
EXPOSE 8080 5000

# Built with the repository root as the context, so the path is module-qualified:
# the jar is produced by ./gradlew :zeebe-ops:bootJar
COPY zeebe-ops/build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
