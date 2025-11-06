FROM eclipse-temurin:17-jre-alpine
COPY build/libs/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]