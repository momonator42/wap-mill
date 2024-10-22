FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/scala-3.3.1/wap-mill.jar /app/wap-mill.jar
CMD ["java", "-jar", "/app/wap-mill.jar"]