FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /usr/src/app

COPY pom.xml /usr/src/app
RUN mvn -B -Ddocker.build.skip=true package test

COPY src /usr/src/app/src

RUN --mount=type=cache,target=/root/.m2 mvn clean compile package

FROM tomcat:10.0-jdk17-openjdk

WORKDIR /usr/local/tomcat

COPY --from=builder /usr/src/app/target/tukano-1.war webapps/webapp.war

EXPOSE 8080
