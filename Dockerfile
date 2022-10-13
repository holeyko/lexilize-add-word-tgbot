# syntax=docker/dockerfile:1
FROM maven
WORKDIR /app

USER root

# copy source dir
COPY src/ src/

# import maven's files
COPY pom.xml .

# copy credencials
COPY run.sh .

EXPOSE 8080

# build and run project
ENTRYPOINT chmod +x ./run.sh && ./run.sh
