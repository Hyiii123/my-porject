ARG TJ_DOCKER_REGISTRY=docker.m.daocloud.io
FROM ${TJ_DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
