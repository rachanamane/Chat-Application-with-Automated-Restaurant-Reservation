FROM alpine:latest
COPY ./build/libs/client.jar /usr/app/
WORKDIR /usr/app
RUN java -Djava.awt.headless=true -jar client.jar
