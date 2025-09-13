FROM maven:3-eclipse-temurin-22-alpine
WORKDIR /tiles
COPY pom.xml pom.xml
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -o
ENTRYPOINT ["java", "-jar", "/tiles/target/sourdough-builder-HEAD-with-deps.jar"]
