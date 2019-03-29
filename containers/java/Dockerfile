FROM fidelius/base:latest
MAINTAINER  Fidelius Contributors

ARG proxy
ENV http_proxy ${proxy}
ENV https_proxy ${proxy}

RUN yum update -y; yum clean all
RUN yum install -y java-1.8.0-openjdk

ENV http_proxy ""
ENV https_proxy ""