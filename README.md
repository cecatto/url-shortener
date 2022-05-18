# URL Shortener

This is a Java RESTful service built with Spring Boot that provides operations to create short URLs and access them.
It uses a Postgres database for storage.

## Documentation
[OpenAPI spec (Swagger)](swagger.yml)

## Requirements
#### For development
 - JDK 11
 - Gradle
 - Docker
#### For running
 - Docker (verified with Docker Desktop v4.7.1)

## How to build and test
Run `./gradlew`. This will execute the default tasks `clean build` and also run all the tests.

## How to run
In the project directory:
 - Build the Docker image with `./gradlew jibDockerBuild`. This will create the image `url-shortener:1.0.0-SNAPSHOT` in your local storage.
 - Run `docker compose up` (or `docker-compose up` if you are using Docker Compose v1). This will start the Postgres database and the application.
