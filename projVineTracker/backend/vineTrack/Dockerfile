# Use uma imagem do Maven como imagem base
FROM maven:3.8.1-openjdk-17-slim AS build

# Defina o diretório de trabalho no container
WORKDIR /vineTrack
RUN apt update && apt install -y wget

CMD mvn spring-boot:run