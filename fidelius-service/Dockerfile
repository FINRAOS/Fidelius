FROM fidelius/java:latest

ARG proxy
ENV http_proxy ${http_proxy}
ENV https_proxy ${http_proxy}

RUN echo $http_proxy
RUN echo $https_proxy

ARG jar_file
ADD src/main/resources/application.yml /usr/share/fidelius/config/application.yml
ADD target/${jar_file} /usr/share/fidelius/app.jar
RUN ls -ltr /usr/share/fidelius
ENV JAVA_OPTS=""
ENTRYPOINT ["java", "-Dspring.config.location=/usr/share/fidelius/config/application.yml",  "-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=container","-jar","/usr/share/fidelius/app.jar"]