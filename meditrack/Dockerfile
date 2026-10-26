# Build the Java application

FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Run the application on Tomcat 9

FROM tomcat:9.0-jdk11-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/meditrack.war /usr/local/tomcat/webapps/meditrack.war

EXPOSE 8080

CMD ["catalina.sh", "run"]

