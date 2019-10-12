FROM openjdk:8-jdk-slim

WORKDIR /usr/src/app

EXPOSE 8080

COPY ./target/priceous.jar /usr/src/app/
COPY config.edn /usr/src/app/

CMD ["java", "-Xms512m", "-Xmx1024m", "-jar", "priceous.jar"]
