FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="kildeen"
LABEL description="SPS Demo - Simple Pub/Sub demonstration service"

WORKDIR /app

# Copy Quarkus fast-jar layout
COPY target/quarkus-app/lib/ /app/lib/
COPY target/quarkus-app/*.jar /app/
COPY target/quarkus-app/app/ /app/app/
COPY target/quarkus-app/quarkus/ /app/quarkus/

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
