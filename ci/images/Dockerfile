FROM repo.cicc.com.cn/public-sec-docker-virtual/zulu-openjdk-kylin:8u382b05-v5

ENV LANG C.UTF-8
RUN echo 'Asia/Shanghai' >/etc/timezone

ARG SERVICE_PACKAGE_NAME
COPY ${SERVICE_PACKAGE_NAME} /app/service.jar
WORKDIR /app
EXPOSE 8080 8090
ENTRYPOINT ["java", "-jar", "./service.jar"]
