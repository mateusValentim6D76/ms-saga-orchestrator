FROM openjdk:17-alpine
COPY build/libs/product-validation-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]