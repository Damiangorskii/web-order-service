FROM openjdk:17-jdk

WORKDIR /app

LABEL maintainer="damian" \
      version="1.0" \
      description="Docker image for the web-order-service"

COPY target/web-order-service-0.0.1-SNAPSHOT.jar /app/web-order-service.jar

EXPOSE 8088

CMD ["java", "-jar", "/app/web-order-service.jar"]