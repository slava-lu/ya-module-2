FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/payments-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
