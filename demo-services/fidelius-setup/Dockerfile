FROM fidelius/java:latest
MAINTAINER  Fidelius Contributors

RUN yum -y install python-pip
RUN pip install credstash

ARG jar_file
ADD target/${jar_file} /usr/share/fidelius/app.jar
RUN ls -ltr /usr/share/fidelius
ENV JAVA_OPTS=""
ENTRYPOINT ["java", "-jar","/usr/share/fidelius/app.jar"]